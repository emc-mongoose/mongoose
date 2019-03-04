package com.emc.mongoose.base.svc;

import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import org.apache.logging.log4j.Level;

public abstract class ServiceBase extends AsyncRunnableBase implements Service {

	protected final int port;

	protected ServiceBase(final int port) {
		this.port = port;
	}

	@Override
	public final int registryPort() {
		return port;
	}

	@Override
	protected void doStart() {
		try {
			try {
				ServiceUtil.create(this, port);
			} catch (final RemoteException
							| URISyntaxException
							| MalformedURLException
							| SocketException e) {
				LogUtil.exception(
								Level.ERROR, e, "Failed to start the service \"{}\" @ port #{}", name(), port);
			}
			Loggers.MSG.info("Service \"{}\" started @ port #{}", name(), port);
		} catch (final RemoteException ignored) {}
	}

	@Override
	protected void doShutdown() {}

	@Override
	protected void doStop() {
		try {
			ServiceUtil.close(this);
			Loggers.MSG.info("Service \"{}\" stopped listening the port #{}", name(), port);
		} catch (final RemoteException | MalformedURLException e) {
			try {
				throw new RemoteException("Failed to stop the service " + name(), e);
			} catch (final RemoteException ignored) {}
		}
	}
}
