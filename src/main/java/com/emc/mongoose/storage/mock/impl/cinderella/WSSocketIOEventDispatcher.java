package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.storage.mock.api.stats.IOStats;
import com.emc.mongoose.storage.mock.impl.io.BasicAdaptiveListeningIOReactor;
//
import org.apache.http.impl.nio.DefaultHttpServerIODispatch;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.NHttpConnectionFactory;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
/**
 Created by kurila on 13.05.15.
 */
public final class WSSocketIOEventDispatcher
extends DefaultHttpServerIODispatch
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final ListeningIOReactor ioReactor;
	private final InetSocketAddress socketAddress;
	//
	public WSSocketIOEventDispatcher(
		final RunTimeConfig runTimeConfig,
		final HttpAsyncService protocolHandler, final int port,
		final NHttpConnectionFactory<DefaultNHttpServerConnection> connFactory,
		final IOStats ioStats
	) throws IOReactorException {
		super(protocolHandler, connFactory);
		socketAddress = new InetSocketAddress(port);
		// set I/O reactor configuration
		final IOReactorConfig ioReactorConf = IOReactorConfig.custom()
			.setIoThreadCount(runTimeConfig.getStorageMockIoThreadsPerSocket())
			.setBacklogSize((int) runTimeConfig.getSocketBindBackLogSize())
			.setInterestOpQueued(runTimeConfig.getSocketInterestOpQueued())
			.setSelectInterval(runTimeConfig.getSocketSelectInterval())
			.setShutdownGracePeriod(runTimeConfig.getSocketTimeOut())
			.setSoKeepAlive(runTimeConfig.getSocketKeepAliveFlag())
			.setSoLinger(runTimeConfig.getSocketLinger())
			.setSoReuseAddress(runTimeConfig.getSocketReuseAddrFlag())
			.setSoTimeout(runTimeConfig.getSocketTimeOut())
			.setTcpNoDelay(runTimeConfig.getSocketTCPNoDelayFlag())
			.setRcvBufSize(LoadExecutor.BUFF_SIZE_LO)
			.setSndBufSize(LoadExecutor.BUFF_SIZE_LO)
			.setConnectTimeout(runTimeConfig.getConnTimeOut())
			.build();
		// create the server-side I/O reactor
		ioReactor = new BasicAdaptiveListeningIOReactor(ioReactorConf, ioStats);
	}
	//
	@Override
	public final String toString() {
		return WSSocketIOEventDispatcher.class.getSimpleName() + ":" + socketAddress.getPort();
	}
	//
	@Override
	public final void run() {
		try {
			// Listen of the given port
			ioReactor.listen(socketAddress);
			// Ready to go!
			ioReactor.execute(this);
		} catch (final InterruptedIOException e) {
			LOG.debug(LogUtil.MSG, "{}: interrupted", this);
		} catch (final IOReactorException e) {
			LogUtil.exception(LOG, Level.WARN, e, "{}: I/O reactor failure", this);
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "{}: I/O failure", this);
		}
	}
}
