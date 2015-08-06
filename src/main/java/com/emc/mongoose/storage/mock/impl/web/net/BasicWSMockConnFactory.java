package com.emc.mongoose.storage.mock.impl.web.net;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.ConnSupport;
import org.apache.http.impl.entity.DisallowIdentityContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
//
import org.apache.http.impl.nio.DefaultNHttpServerConnectionFactory;
import org.apache.http.nio.NHttpMessageParserFactory;
import org.apache.http.nio.NHttpMessageWriterFactory;
import org.apache.http.nio.reactor.IOSession;
//
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
/**
 * Created by olga on 04.02.15.
 */
public final class BasicWSMockConnFactory
extends DefaultNHttpServerConnectionFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ConnectionConfig connConfig;
	//
	private final int minConnLifeMilliSec, maxConnLifeMilliSec;
	private final ScheduledExecutorService connDropExecutor;
	// TODO: for future extension
	private final NHttpMessageParserFactory<HttpRequest> requestParserFactory = null;
	private final NHttpMessageWriterFactory<HttpResponse> responseWriterFactory = null;
	//
	public BasicWSMockConnFactory(
		final ConnectionConfig connConfig,
		final int minConnLifeMilliSec, final int maxConnLifeMilliSec
	) {
		super(connConfig);
		this.connConfig = connConfig;
		//
		this.minConnLifeMilliSec = minConnLifeMilliSec;
		this.maxConnLifeMilliSec = maxConnLifeMilliSec;
		if(minConnLifeMilliSec < 0 || maxConnLifeMilliSec < minConnLifeMilliSec) {
			throw new IllegalArgumentException(
				"Illegal min/max connection live times: " + minConnLifeMilliSec + ", " +
				maxConnLifeMilliSec
			);
		}
		//
		if(maxConnLifeMilliSec == 0) {
			connDropExecutor = null;
		} else {
			connDropExecutor = Executors.newScheduledThreadPool(
				2, new GroupThreadFactory("failConn", true)
			);
		}
	}
	//
	@Override
	public final BasicWSMockConnection createConnection(final IOSession session) {
		final BasicWSMockConnection conn = new BasicWSMockConnection(
			session,
			connConfig.getBufferSize(),
			connConfig.getFragmentSizeHint(),
			HeapByteBufferAllocator.INSTANCE,
			ConnSupport.createDecoder(connConfig),
			ConnSupport.createEncoder(connConfig),
			connConfig.getMessageConstraints(),
			DisallowIdentityContentLengthStrategy.INSTANCE,
			StrictContentLengthStrategy.INSTANCE,
			requestParserFactory,
			responseWriterFactory
		);
		if(connDropExecutor != null) {
			connDropExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							Thread.sleep(
								ThreadLocalRandom.current().nextInt(minConnLifeMilliSec, maxConnLifeMilliSec)
							);
							conn.close();
						} catch(final IOException | InterruptedException ignored) {
						}
					}
				}
			);
		}
		return conn;
	}
}
