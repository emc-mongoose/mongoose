package com.emc.mongoose.storage.mock.impl.http.net;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.ConnSupport;
import org.apache.http.impl.entity.DisallowIdentityContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.reactor.IOSession;
//
import org.apache.http.nio.util.DirectByteBufferAllocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Created by olga on 04.02.15.
 */
public final class BasicHttpStorageMockConnFactory
extends DefaultNHttpServerConnectionFactory  {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ConnectionConfig connConfig;
	//
	private final NHttpMessageParserFactory<HttpRequest> requestParserFactory = null;
	private final NHttpMessageWriterFactory<HttpResponse> responseWriterFactory = null;
	//
	public BasicHttpStorageMockConnFactory(final ConnectionConfig connConfig) {
		super(connConfig);
		this.connConfig = connConfig;
	}
	//
	@Override
	public final DefaultNHttpServerConnection createConnection(final IOSession session) {
		final DefaultNHttpServerConnection conn = new BasicHttpStorageMockConnection(
			session,
			connConfig.getBufferSize(),
			connConfig.getFragmentSizeHint(),
			DirectByteBufferAllocator.INSTANCE,
			ConnSupport.createDecoder(connConfig),
			ConnSupport.createEncoder(connConfig),
			connConfig.getMessageConstraints(),
			DisallowIdentityContentLengthStrategy.INSTANCE,
			StrictContentLengthStrategy.INSTANCE,
			requestParserFactory,
			responseWriterFactory
		);
		return conn;
	}
}
