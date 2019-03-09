package com.emc.mongoose.storage.driver.coop.netty.http;

import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.path.PathOperation;
import com.emc.mongoose.base.item.op.token.TokenOperation;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.OpType;
import static com.emc.mongoose.base.item.op.Operation.Status.FAIL_TIMEOUT;
import static com.emc.mongoose.base.item.op.Operation.Status.FAIL_UNKNOWN;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_AUTH;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_CLIENT;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_CORRUPT;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_NOT_FOUND;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_SPACE;
import static com.emc.mongoose.base.item.op.Operation.Status.RESP_FAIL_SVC;
import static com.emc.mongoose.base.item.op.Operation.Status.SUCC;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;

import com.emc.mongoose.storage.driver.coop.netty.ResponseHandlerBase;
import static com.emc.mongoose.storage.driver.coop.netty.data.ResponseContentUtil.verifyChunk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http.LastHttpContent;

import org.apache.logging.log4j.Level;

import java.io.IOException;

/**
Created by kurila on 05.09.16.
*/
public abstract class HttpResponseHandlerBase<I extends Item, O extends Operation<I>>
				extends ResponseHandlerBase<HttpObject, I, O> {

	protected HttpResponseHandlerBase(final HttpStorageDriverBase<I, O> driver, final boolean verifyFlag) {
		super(driver, verifyFlag);
	}

	protected boolean handleResponseStatus(
					final O op, final HttpStatusClass statusClass, final HttpResponseStatus responseStatus) {
		switch (statusClass) {
		case INFORMATIONAL:
			Loggers.ERR.warn("{}: {}", op.toString(), responseStatus.toString());
			op.status(RESP_FAIL_CLIENT);
			break;
		case SUCCESS:
			op.status(SUCC);
			return true;
		case REDIRECTION:
			Loggers.ERR.warn("{}: {}", op.toString(), responseStatus.toString());
			op.status(RESP_FAIL_CLIENT);
			break;
		case CLIENT_ERROR:
			Loggers.ERR.warn("{}: {}", op.toString(), responseStatus.toString());
			if (HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE.equals(responseStatus)) {
				op.status(RESP_FAIL_SVC);
			} else if (HttpResponseStatus.REQUEST_URI_TOO_LONG.equals(responseStatus)) {
				op.status(RESP_FAIL_SVC);
			} else if (HttpResponseStatus.UNAUTHORIZED.equals(responseStatus)) {
				op.status(RESP_FAIL_AUTH);
			} else if (HttpResponseStatus.FORBIDDEN.equals(responseStatus)) {
				op.status(RESP_FAIL_AUTH);
			} else if (HttpResponseStatus.NOT_FOUND.equals(responseStatus)) {
				op.status(RESP_FAIL_NOT_FOUND);
			} else {
				op.status(RESP_FAIL_CLIENT);
			}
			break;
		case SERVER_ERROR:
			Loggers.ERR.warn("{}: {}", op.toString(), responseStatus.toString());
			if (HttpResponseStatus.GATEWAY_TIMEOUT.equals(responseStatus)) {
				op.status(FAIL_TIMEOUT);
			} else if (HttpResponseStatus.INSUFFICIENT_STORAGE.equals(responseStatus)) {
				op.status(RESP_FAIL_SPACE);
			} else {
				op.status(RESP_FAIL_SVC);
			}
			break;
		case UNKNOWN:
			Loggers.ERR.warn("{}: {}", op.toString(), responseStatus.toString());
			op.status(FAIL_UNKNOWN);
			break;
		}

		return false;
	}

	protected abstract void handleResponseHeaders(final Channel channel, final O op, final HttpHeaders respHeaders);

	protected void handleResponseContentChunk(final Channel channel, final O op, final ByteBuf contentChunk)
					throws IOException {
		if (OpType.READ.equals(op.type())) {
			if (op instanceof DataOperation) {
				final DataOperation dataOp = (DataOperation) op;
				final long countBytesDone = dataOp.countBytesDone();
				if (dataOp.respDataTimeStart() == 0) { // if not set yet - 1st time
					try {
						dataOp.startDataResponse();
					} catch (final IllegalStateException e) {
						LogUtil.exception(Level.DEBUG, e, "{}", dataOp.toString());
					}
				}
				final int chunkSize = contentChunk.readableBytes();
				if (chunkSize > 0) {
					if (verifyFlag) {
						if (!RESP_FAIL_CORRUPT.equals(op.status())) {
							verifyChunk(dataOp, countBytesDone, contentChunk, chunkSize);
						}
					} else {
						dataOp.countBytesDone(countBytesDone + chunkSize);
					}
				}
			} else if (op instanceof PathOperation) {
				final PathOperation pathOp = (PathOperation) op;
				final long countBytesDone = pathOp.countBytesDone();
				if (pathOp.respDataTimeStart() == 0) { // if not set yet - 1st time
					pathOp.startDataResponse();
				}
				final int chunkSize = contentChunk.readableBytes();
				if (chunkSize > 0) {
					pathOp.countBytesDone(countBytesDone + chunkSize);
				}
			} else if (op instanceof TokenOperation) {
				final TokenOperation tokenOp = (TokenOperation) op;
				final long countBytesDone = tokenOp.countBytesDone();
				if (tokenOp.respDataTimeStart() == 0) { // if not set yet - 1st time
					tokenOp.startDataResponse();
				}
				final int chunkSize = contentChunk.readableBytes();
				if (chunkSize > 0) {
					tokenOp.countBytesDone(countBytesDone + chunkSize);
				}
			} else {
				throw new AssertionError("Not implemented yet");
			}
		}
	}

	protected void handleResponseContentFinish(final Channel channel, final O op) {
		driver.complete(channel, op);
	}

	@Override
	protected final void handle(final Channel channel, final O op, final HttpObject msg)
					throws IOException {

		if (msg instanceof HttpResponse) {
			try {
				op.startResponse();
			} catch (final IllegalStateException e) {
				LogUtil.exception(Level.DEBUG, e, "{}", op.toString());
			}
			final HttpResponse httpResponse = (HttpResponse) msg;
			if (Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace("{} <<<< {}", op.hashCode(), httpResponse.status());
			}
			final HttpResponseStatus httpResponseStatus = httpResponse.status();
			handleResponseStatus(op, httpResponseStatus.codeClass(), httpResponseStatus);
			handleResponseHeaders(channel, op, httpResponse.headers());
			if (msg instanceof FullHttpResponse) {
				final ByteBuf fullRespContent = ((FullHttpResponse) msg).content();
				handleResponseContentChunk(channel, op, fullRespContent);
			}
		}

		if (msg instanceof HttpContent) {
			handleResponseContentChunk(channel, op, ((HttpContent) msg).content());
			if (msg instanceof LastHttpContent) {
				handleResponseContentFinish(channel, op);
			}
		}
	}
}
