package com.emc.mongoose.storage.mock.impl.http.net;
//
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
//
import org.apache.http.nio.util.DirectByteBufferAllocator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
/**
 Created by kurila on 08.06.15.
 */
public class BasicHttpStorageMockConnection
extends DefaultNHttpServerConnection {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final int maxInBuffSize, maxOutBuffSize;
	//
	public BasicHttpStorageMockConnection(
		final IOSession session, final int buffersize, final int fragmentSizeHint,
		final ByteBufferAllocator allocator, final CharsetDecoder chardecoder,
		final CharsetEncoder charencoder, final MessageConstraints constraints,
		final ContentLengthStrategy incomingContentStrategy,
		final ContentLengthStrategy outgoingContentStrategy,
		final NHttpMessageParserFactory<HttpRequest> requestParserFactory,
		final NHttpMessageWriterFactory<HttpResponse> responseWriterFactory
	) {
		super(
			session, buffersize, fragmentSizeHint, allocator, chardecoder, charencoder, constraints,
			incomingContentStrategy, outgoingContentStrategy, requestParserFactory,
			responseWriterFactory
		);
		// get the socket
		final Socket socket = getSocket();
		if(socket == null) {
			LOG.warn(Markers.ERR, "No socket available to probe the I/O buffers for size limits");
			maxInBuffSize = Constants.BUFF_SIZE_HI;
			maxOutBuffSize = Constants.BUFF_SIZE_HI;
		} else {
			try {
				// probe the input buffer for size limit
				socket.setReceiveBufferSize(Constants.BUFF_SIZE_HI);
				maxInBuffSize = socket.getReceiveBufferSize();
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "{}: max IN buffer size is {}", this,
						SizeInBytes.formatFixedSize(maxInBuffSize)
					);
				}
				socket.setReceiveBufferSize(Constants.BUFF_SIZE_LO); // reset back to the default
				// probe the output buffer for size limit
				socket.setSendBufferSize(Constants.BUFF_SIZE_HI);
				maxOutBuffSize = socket.getSendBufferSize();
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "{}: max OUT buffer size is {}", this,
						SizeInBytes.formatFixedSize(maxOutBuffSize)
					);
				}
				socket.setSendBufferSize(Constants.BUFF_SIZE_LO); // reset back to the default
				//
				socket.setPerformancePreferences(0, 1, 2);
			} catch(final SocketException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	//
	public BasicHttpStorageMockConnection(
		final IOSession session, final int buffersize, final CharsetDecoder chardecoder,
		final CharsetEncoder charencoder, final MessageConstraints constraints
	) {
		this(
			session, buffersize, 0, DirectByteBufferAllocator.INSTANCE, chardecoder, charencoder,
			constraints, null, null, null, null
		);
	}
	//
	public BasicHttpStorageMockConnection(final IOSession session, final int buffersize) {
		this(
			session, buffersize, 0, DirectByteBufferAllocator.INSTANCE, null, null, null, null, null,
			null, null
		);
	}
	//
	@Override
	protected final void onRequestReceived(final HttpRequest httpRequest) {
		//
		super.onRequestReceived(httpRequest);
		//
		final Header contentLenHeader = httpRequest.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
		final Socket socket = getSocket();
		try {
			final int
				newBuffSize,
				lastBuffSize = socket.getReceiveBufferSize();
			if(contentLenHeader != null) {
				newBuffSize = (int) Math.max(
					Constants.BUFF_SIZE_LO,
					Math.min(maxInBuffSize, Long.parseLong(contentLenHeader.getValue()))
				);
			} else {
				newBuffSize = Constants.BUFF_SIZE_LO;
			}
			//
			if(lastBuffSize != newBuffSize) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "{}: IN buffer size {} to {}", socket,
						SizeInBytes.formatFixedSize(lastBuffSize),
						SizeInBytes.formatFixedSize(newBuffSize)
					);
				}
				socket.setSendBufferSize(newBuffSize);
			}
		} catch(final SocketException | NumberFormatException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Socket buffer size adjustment failure");
		}
	}
	//
	@Override
	protected final void onResponseSubmitted(final HttpResponse httpResponse) {
		//
		super.onResponseSubmitted(httpResponse);
		//
		final Header contentLenHeader = httpResponse.getFirstHeader(HttpHeaders.CONTENT_LENGTH);
		final Socket socket = getSocket();
		try {
			final int
				newBuffSize,
				lastBuffSize = socket.getSendBufferSize();
			if(contentLenHeader != null) {
				newBuffSize = (int) Math.max(
					Constants.BUFF_SIZE_LO,
					Math.min(maxOutBuffSize, Long.parseLong(contentLenHeader.getValue()))
				);
			} else {
				newBuffSize = Constants.BUFF_SIZE_LO;
			}
			//
			if(lastBuffSize != newBuffSize) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(
						Markers.MSG, "{}: OUT buffer size {} to {}", socket,
						SizeInBytes.formatFixedSize(lastBuffSize),
						SizeInBytes.formatFixedSize(newBuffSize)
					);
				}
				socket.setSendBufferSize(newBuffSize);
			}
		} catch(final SocketException | NumberFormatException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Socket buffer size adjustment failure");
		}
	}
}
