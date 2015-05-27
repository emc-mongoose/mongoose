package com.emc.mongoose.storage.mock.impl.io;
//
import com.emc.mongoose.common.concurrent.NamingWorkerFactory;
//import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.storage.mock.api.stats.IOStats;
//
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListeningIOReactor;
//
//import java.net.Socket;
//import java.net.SocketException;
/**
 Created by kurila on 18.05.15.
 */
public final class BasicAdaptiveListeningIOReactor
extends DefaultListeningIOReactor
implements ListeningIOReactor {
	//
	private final IOStats ioStats;
	//
	public BasicAdaptiveListeningIOReactor(final IOReactorConfig config, final IOStats ioStats)
	throws IOReactorException {
		super(config, new NamingWorkerFactory("ioDispatch"));
		this.ioStats = ioStats;
	}
	/*
	@Override
	protected final void prepareSocket(final Socket socket)
	throws SocketException {
		socket.setTcpNoDelay(config.isTcpNoDelay());
		socket.setKeepAlive(config.isSoKeepalive());
		if(config.getSoTimeout() > 0) {
			socket.setSoTimeout(config.getSoTimeout());
		}
		final double
			inRate = ioStats.getWriteRate(),
			outRate = ioStats.getReadRate(),
			buffSize;
		if(inRate > outRate) { // care about ingest rate
			buffSize = ioStats.getWriteRateBytes() / inRate; // size estimation
			if(buffSize < LoadExecutor.BUFF_SIZE_LO) {
				socket.setReceiveBufferSize(LoadExecutor.BUFF_SIZE_LO);
			} else if(buffSize > LoadExecutor.BUFF_SIZE_HI) {
				socket.setReceiveBufferSize(LoadExecutor.BUFF_SIZE_HI);
			} else {
				socket.setReceiveBufferSize((int) buffSize); // type cast should be safe here
			}
		} else if(outRate > inRate) { // care about read back rate
			buffSize = ioStats.getReadRateBytes() / outRate; // size estimation
			if(buffSize < LoadExecutor.BUFF_SIZE_LO) {
				socket.setSendBufferSize(LoadExecutor.BUFF_SIZE_LO);
			} else if(buffSize > LoadExecutor.BUFF_SIZE_HI) {
				socket.setSendBufferSize(LoadExecutor.BUFF_SIZE_HI);
			} else {
				socket.setSendBufferSize((int) buffSize); // type cast should be safe here
			}
		} else { // have no idea, don't change the buffers sizes
			if(config.getSndBufSize() > 0) {
				socket.setSendBufferSize(config.getSndBufSize());
			}
			if(config.getRcvBufSize() > 0) {
				socket.setReceiveBufferSize(config.getRcvBufSize());
			}
		}
		final int linger = config.getSoLinger();
		if(linger >= 0) {
			socket.setSoLinger(true, linger);
		}
	}*/
}
