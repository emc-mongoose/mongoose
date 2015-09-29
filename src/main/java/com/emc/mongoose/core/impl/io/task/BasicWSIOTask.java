package com.emc.mongoose.core.impl.io.task;
// mongoose-common
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.http.ContentUtil;
import com.emc.mongoose.common.net.http.content.InputChannel;
import com.emc.mongoose.common.net.http.content.OutputChannel;
import com.emc.mongoose.common.log.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.data.DataCorruptionException;
import com.emc.mongoose.core.api.data.DataSizeException;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.WSIOTask;
// mongoose-core-impl
import com.emc.mongoose.core.impl.data.RangeLayerData;
import com.emc.mongoose.core.impl.data.UniformData;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
//
import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
/**
 Created by kurila on 06.06.14.
 */
public class BasicWSIOTask<T extends WSObject>
extends BasicObjectIOTask<T>
implements WSIOTask<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile OutputChannel chanOut = null;
	private volatile InputChannel chanIn = null;
	//
	public BasicWSIOTask(
		final T dataObject, final String nodeAddr, final WSRequestConfig<T> reqConf
	) {
		super(dataObject, nodeAddr, reqConf);
	}
	/**
	 * Warning: invoked implicitly and untimely in the depths of HttpCore lib.
	 * So does nothing
	 */
	@Override
	public final void close() {}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private volatile Exception exception = null;
	@SuppressWarnings("FieldCanBeLocal")
	private volatile int respStatusCode = -1;
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncRequestProducer implementation /////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final HttpHost getTarget() {
		return ((WSRequestConfig<T>) reqConf).getNodeHost(nodeAddr);
	}
	//
	@Override
	public final HttpRequest generateRequest()
	throws IOException, HttpException {
		final HttpEntityEnclosingRequest httpRequest;
		try {
			 httpRequest = ((WSRequestConfig<T>) reqConf).createDataRequest(dataItem, nodeAddr);
		} catch(final URISyntaxException e) {
			throw new HttpException("Failed to generate the request", e);
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(
				Markers.MSG, "I/O task #{}: generated the request: {}",
				hashCode(), httpRequest
			);
		}
		reqTimeStart = System.nanoTime() / 1000;
		return httpRequest;
	}
	//
	@Override
	public final void produceContent(final ContentEncoder encoder, final IOControl ioCtl)
	throws IOException {
		//
		if(chanOut == null) { // 1st time invocation
			if(dataItem.getSize() == 0 && dataItem.getAppendSize() == 0) { // nothing to do
				encoder.complete();
				return;
			} else { // wrap the encoder w/ output channel
				chanOut = new OutputChannel(encoder);
			}
		}
		//
		try {
			switch(ioType) {
				case CREATE:
					produceObjectContent(ioCtl);
					break;
				case READ:
					// TODO here is an ability to specify some content here
					break;
				case DELETE:
					// TODO here is an ability to specify some content here
					break;
				case UPDATE:
					produceUpdatedRangesContent(ioCtl);
					break;
				case APPEND:
					produceAugmentContent(ioCtl);
					break;
			}
		} catch(final ClosedChannelException e) { // probably a manual interruption
			status = Status.CANCELLED;
			LogUtil.exception(
				LOG, Level.TRACE, e, "#{}: output channel closed during the operation", hashCode()
			);
		} catch(final IOException e) {
			status = Status.FAIL_IO;
			LogUtil.exception(
				LOG, Level.DEBUG, e, "#{}: I/O failure during the data output", hashCode()
			);
		} catch(final Exception e) {
			status = Status.FAIL_UNKNOWN;
			LogUtil.exception(LOG, Level.ERROR, e, "#{}: producing content failure", hashCode());
		}
	}
	//
	private void produceObjectContent(final IOControl ioCtl)
	throws IOException {
		countBytesDone += dataItem.write(chanOut, contentSize - countBytesDone);
		if(countBytesDone == contentSize) {
			dataItem.resetUpdates();
			chanOut.close();
		}
	}
	//
	private void produceUpdatedRangesContent(final IOControl ioCtl)
	throws IOException {
		//
		if(countBytesDone + countBytesSkipped == nextRangeOffset) {
			// find next updating range
			do {
				currRangeSize = dataItem.getRangeSize(currRangeIdx);
				// select the current range if it's updating
				if(dataItem.isCurrLayerRangeUpdating(currRangeIdx)) {
					currRange = new UniformData(
						dataItem.getOffset() + nextRangeOffset, currRangeSize,
						currDataLayerIdx, UniformDataSource.DEFAULT
					);
				} else if(dataItem.isNextLayerRangeUpdating(currRangeIdx)) {
					currRange = new UniformData(
						dataItem.getOffset() + nextRangeOffset, currRangeSize,
						currDataLayerIdx + 1, UniformDataSource.DEFAULT
					);
				} else {
					countBytesSkipped += currRangeSize;
					currRange = null;
				}
				currRangeIdx ++;
				nextRangeOffset = RangeLayerData.getRangeOffset(currRangeIdx);
			} while(currRange == null && countBytesDone < contentSize);
		}
		// write the current updating range's content
		if(currRangeSize > 0 && currRange != null) {
			countBytesDone += currRange.write(
				chanOut, nextRangeOffset - countBytesDone - countBytesSkipped
			);
		}
		// all scheduled updates are written out, commit the update transaction and finish
		if(countBytesDone == contentSize) {
			dataItem.commitUpdatedRanges();
			chanOut.close();
		}
	}
	//
	private void produceAugmentContent(final IOControl ioCtl)
	throws IOException {
		if(currRange == null) {
			final long prevSize = dataItem.getSize();
			currRangeIdx = prevSize > 0 ? RangeLayerData.getRangeCount(prevSize) - 1 : 0;
			if(dataItem.isCurrLayerRangeUpdated(currRangeIdx)) {
				currRange = new UniformData(
					dataItem.getOffset() + prevSize, contentSize, currDataLayerIdx + 1,
					UniformDataSource.DEFAULT
				);
			} else {
				currRange = new UniformData(
					dataItem.getOffset() + prevSize, contentSize, currDataLayerIdx,
					UniformDataSource.DEFAULT
				);
			}
		}
		//
		countBytesDone += dataItem.write(chanOut, contentSize - countBytesDone);
		if(countBytesDone == contentSize) {
			dataItem.commitAppend();
			chanOut.close();
		}
	}
	//
	@Override
	public final void requestCompleted(final HttpContext context) {
		reqTimeDone = System.nanoTime() / 1000;
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "I/O task #{}: request sent completely", hashCode());
		}
	}
	//
	@Override
	public final void failed(final Exception e) {
		if(e instanceof ConnectionClosedException) {
			LogUtil.exception(LOG, Level.TRACE, e, "I/O task dropped while executing");
			status = Status.CANCELLED;
			exception = e;
			respTimeDone = System.nanoTime() / 1000;
		} else {
			LogUtil.exception(LOG, Level.WARN, e, "I/O task failure");
			status = Status.FAIL_UNKNOWN;
			exception = e;
			respTimeDone = System.nanoTime() / 1000;
		}
	}
	//
	@Override
	public final boolean isRepeatable() {
		return WSObject.IS_CONTENT_REPEATABLE;
	}
	//
	@Override
	public final void resetRequest() {
		respStatusCode = -1;
		countBytesDone = 0;
		status = Status.FAIL_UNKNOWN;
		exception = null;
	}
	/*
	@Override @SuppressWarnings("unchecked")
	public final BasicWSIOTask<T> reuse(final Object... args) {
		return (BasicWSIOTask<T>) super.reuse(args);
	}*/
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpAsyncResponseConsumer implementation ////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void responseReceived(final HttpResponse response)
	throws IOException, HttpException {
		//
		respTimeStart = System.nanoTime() / 1000;
		final StatusLine status = response.getStatusLine();
		respStatusCode = status.getStatusCode();
		//
		if(respStatusCode < 200 || respStatusCode >= 300) {
			LOG.debug(Markers.ERR, "I/O task #{}: got response \"{}\"", hashCode(), status);
			//
			switch(respStatusCode) {
				case HttpStatus.SC_CONTINUE:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_BAD_REQUEST:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNAUTHORIZED:
				case HttpStatus.SC_FORBIDDEN:
					this.status = Status.RESP_FAIL_AUTH;
					break;
				case HttpStatus.SC_NOT_FOUND:
					this.status = Status.RESP_FAIL_NOT_FOUND;
					break;
				case HttpStatus.SC_METHOD_NOT_ALLOWED:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_CONFLICT:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_LENGTH_REQUIRED:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_REQUEST_TOO_LONG:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUEST_URI_TOO_LONG:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
					this.status = Status.RESP_FAIL_CLIENT;
					break;
				case 429:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INTERNAL_SERVER_ERROR:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_NOT_IMPLEMENTED:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_BAD_GATEWAY:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_SERVICE_UNAVAILABLE:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_GATEWAY_TIMEOUT:
					this.status = Status.FAIL_TIMEOUT;
					break;
				case HttpStatus.SC_HTTP_VERSION_NOT_SUPPORTED:
					this.status = Status.RESP_FAIL_SVC;
					break;
				case HttpStatus.SC_INSUFFICIENT_STORAGE:
					this.status = Status.RESP_FAIL_SPACE;
					break;
				default:
					this.status = Status.FAIL_UNKNOWN;
					break;
			}
		} else {
			this.status = Status.SUCC;
			((WSRequestConfig<T>) reqConf).applySuccResponseToObject(response, dataItem);
		}
	}
	//
	@Override
	public final void consumeContent(final ContentDecoder decoder, final IOControl ioCtl) {
		try {
			if(respStatusCode < 200 || respStatusCode >= 300) { // failure, no user data is expected
				consumeFailedResponseContent(decoder, ioCtl);
			} else {
				// check for the content corruption
				if(Type.READ.equals(ioType)) {
					// just consume quietly if marked as corrupted once
					if(!Status.RESP_FAIL_CORRUPT.equals(status) && reqConf.getVerifyContentFlag()) {
						// should verify the content
						consumeAndVerifyContent(decoder, ioCtl);
					} else { // consume quietly
						countBytesDone += ContentUtil.consumeQuietly(
							decoder, contentSize - countBytesDone
						);
					}
				} else {
					ContentUtil.consumeQuietly(decoder, Constants.BUFF_SIZE_LO);
				}
			}
		} catch(final ClosedChannelException e) {
			status = Status.CANCELLED;
			LogUtil.exception(LOG, Level.TRACE, e, "Output channel closed during the operation");
		} catch(final IOException e) {
			if(!reqConf.isClosed()) {
				LogUtil.exception(LOG, Level.DEBUG, e, "I/O failure during content consuming");
			}
		}
	}
	//
	private void consumeFailedResponseContent(final ContentDecoder in, final IOControl ioCtl)
	throws IOException {
		final ByteBuffer bbuff = ByteBuffer.allocate(Constants.BUFF_SIZE_LO);
		while(in.read(bbuff) >= 0 && bbuff.remaining() > 0);
		LOG.debug(
			Markers.ERR, "#{}: {} - {}", hashCode(), status.description,
			new String(bbuff.array(), 0, bbuff.position(), StandardCharsets.UTF_8)
		);
	}
	//
	private void consumeAndVerifyContent(final ContentDecoder decoder, final IOControl ioCtl)
	throws IOException {
		//
		if(chanIn == null) {
			chanIn = new InputChannel(decoder);
		}
		//
		final ByteBuffer buffIn;
		try {
			if(dataItem.hasBeenUpdated()) {
				// switch the range if current is done or not set yet
				if(countBytesDone == nextRangeOffset) {
					currRangeSize = dataItem.getRangeSize(currRangeIdx);
					if(dataItem.isCurrLayerRangeUpdated(currRangeIdx)) {
						currRange = new UniformData(
							dataItem.getOffset() + nextRangeOffset, currRangeSize,
							currDataLayerIdx + 1, UniformDataSource.DEFAULT
						);
					} else {
						currRange = new UniformData(
							dataItem.getOffset() + nextRangeOffset, currRangeSize,
							currDataLayerIdx, UniformDataSource.DEFAULT
						);
					}
					currRangeIdx ++;
					nextRangeOffset = RangeLayerData.getRangeOffset(currRangeIdx);
				}
				//
				if(currRangeSize > 0) {
					buffIn = ((IOWorker) Thread.currentThread())
						.getThreadLocalBuff(nextRangeOffset - countBytesDone);
					final int n = currRange.readAndVerify(chanIn, buffIn);
					if(n > 0) {
						countBytesDone += n;
					}
					if(countBytesDone == contentSize) {
						chanIn.close();
					}
				} else {
					chanIn.close();
				}
			} else {
				buffIn = ((IOWorker) Thread.currentThread())
					.getThreadLocalBuff(contentSize - countBytesDone);
				final int n = dataItem.readAndVerify(chanIn, buffIn);
				if(n > 0) {
					countBytesDone += n;
				}
			}
		} catch(final DataSizeException e) {
			countBytesDone += e.offset;
			LOG.warn(
				Markers.MSG,
				"{}: content size mismatch, expected: {}, actual: {}",
				dataItem.getId(), dataItem.getSize(), e.offset
			);
			status = Status.RESP_FAIL_CORRUPT;
		} catch(final DataCorruptionException e) {
			countBytesDone += e.offset;
			LOG.warn(
				Markers.MSG,
				"{}: content mismatch @ offset {}, expected: {}, actual: {}",
				dataItem.getId(), e.offset,
				String.format(
					"\"0x%X\"", e.expected), String.format("\"0x%X\"", e.actual
				)
			);
			status = Status.RESP_FAIL_CORRUPT;
		}
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
		respTimeDone = System.nanoTime() / 1000;
	}
	//
	@Override
	public final WSIOTask<T> getResult() {
		return this;
	}
	//
	@Override
	public final Exception getException() {
		return exception;
	}
	//
	@Override
	public final boolean isDone() {
		return respTimeDone != 0;
	}
	//
	@Override
	public final boolean cancel() {
		LOG.debug(Markers.MSG, "{}: I/O task cancel", hashCode());
		return false;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// HttpContext implementation //////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final HttpContext wrappedHttpCtx = new BasicHttpContext();
	//
	@Override
	public final Object getAttribute(final String s) {
		return wrappedHttpCtx.getAttribute(s);
	}
	@Override
	public final void setAttribute(final String s, final Object o) {
		wrappedHttpCtx.setAttribute(s, o);
	}
	@Override
	public final Object removeAttribute(final String s) {
		return wrappedHttpCtx.removeAttribute(s);
	}
}
