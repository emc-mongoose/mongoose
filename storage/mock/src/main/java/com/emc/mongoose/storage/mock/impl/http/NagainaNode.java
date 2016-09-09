package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockNode;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockClient;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockServer;
import com.emc.mongoose.ui.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 Created on 09.09.16.
 */
class NagainaNode
	implements StorageMockNode<MutableDataItemMock, StorageMockServer<MutableDataItemMock>> {

	private static final Logger LOG = LogManager.getLogger();
	private JmDNS jmDns;
	private StorageMockClient<MutableDataItemMock, StorageMockServer<MutableDataItemMock>> client;
	private StorageMockServer<MutableDataItemMock> server;

	public NagainaNode(final StorageMock<MutableDataItemMock> storage) {
		//			System.setProperty("java.rmi.server.hostname", NetUtil.getHostAddrString()); workaround
		try {
			jmDns = JmDNS.create(NetUtil.getHostAddr());
			LOG.info("mDNS address: " + jmDns.getInetAddress());
			server = new BasicStorageMockServer<>(storage, jmDns);
			client = new BasicStorageMockClient<>(jmDns);
		} catch(final IOException | OmgDoesNotPerformException | OmgLookAtMyConsoleException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to create storage mock node");
		}
	}

	@Override
	public StorageMockClient<MutableDataItemMock, StorageMockServer<MutableDataItemMock>> client() {
		return client;
	}

	@Override
	public StorageMockServer<MutableDataItemMock> server() {
		return server;
	}

	@Override
	public void start()
	throws UserShootHisFootException {
		try {
			server.start();
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to start storage mock server");
		}
		client.start();
	}

	@Override
	public boolean isStarted() {
		try {
			return server.isStarted();
		} catch(final RemoteException ignore) {
		}
		return false;
	}

	@Override
	public boolean await()
	throws InterruptedException {
		try {
			return server.await();
		} catch(final RemoteException ignore) {
		}
		return false;
	}

	@Override
	public boolean await(final long timeout, final TimeUnit timeUnit)
	throws InterruptedException {
		try {
			return server.await(timeout, timeUnit);
		} catch(final RemoteException ignore) {
		}
		return false;
	}

	@Override
	public void close()
	throws IOException {
		client.close();
		jmDns.unregisterAllServices();
		jmDns.close();
		server.close();
	}
}
