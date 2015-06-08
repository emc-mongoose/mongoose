package com.emc.mongoose.storage.mock.impl.net;
//
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.logging.LogUtil;
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
public class BasicWSMockConnection
extends DefaultNHttpServerConnection {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSMockConnection(
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
	}
	//
	public BasicWSMockConnection(
		final IOSession session, final int buffersize, final CharsetDecoder chardecoder,
		final CharsetEncoder charencoder, final MessageConstraints constraints
	) {
		super(session, buffersize, chardecoder, charencoder, constraints);
	}
	//
	public BasicWSMockConnection(final IOSession session, final int buffersize) {
		super(session, buffersize);
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
					Math.min(Constants.BUFF_SIZE_HI, Long.parseLong(contentLenHeader.getValue()))
				);
			} else {
				newBuffSize = Constants.BUFF_SIZE_LO;
			}
			//
			if(lastBuffSize != newBuffSize) {
				LOG.info(
					LogUtil.MSG, "{}: IN buffer size {} to {}",
					socket, SizeUtil.formatSize(lastBuffSize), SizeUtil.formatSize(newBuffSize)
				);
				socket.setReceiveBufferSize(newBuffSize);
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
					Math.min(Constants.BUFF_SIZE_HI, Long.parseLong(contentLenHeader.getValue()))
				);
			} else {
				newBuffSize = Constants.BUFF_SIZE_LO;
			}
			//
			if(lastBuffSize != newBuffSize) {
				LOG.info(
					LogUtil.MSG, "{}: OUT buffer size {} to {}",
					socket, SizeUtil.formatSize(lastBuffSize), SizeUtil.formatSize(newBuffSize)
				);
				socket.setSendBufferSize(newBuffSize);
			}
		} catch(final SocketException | NumberFormatException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Socket buffer size adjustment failure");
		}
	}
}
