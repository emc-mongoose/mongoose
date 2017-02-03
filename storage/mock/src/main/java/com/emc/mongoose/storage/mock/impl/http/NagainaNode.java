package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.model.DaemonBase;
import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockNode;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockClient;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockServer;
import com.emc.mongoose.storage.mock.impl.base.ProtoStorageMockClient;
import com.emc.mongoose.storage.mock.impl.base.ProtoStorageMockServer;
import com.emc.mongoose.storage.mock.impl.proto.StorageMockProto;
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
public class NagainaNode
extends DaemonBase
implements StorageMockNode<MutableDataItemMock> {

	private static final Logger LOG = LogManager.getLogger();
	private JmDNS jmDns;
	private StorageMockClient<MutableDataItemMock> client;
	private StorageMockServer<MutableDataItemMock> server;

	public NagainaNode(
		final StorageMock<MutableDataItemMock> storage, final ContentSource contentSrc
	) {
		// System.setProperty("java.rmi.server.hostname", NetUtil.getHostAddrString()); workaround
		try {
			jmDns = JmDNS.create(NetUtil.getHostAddr());
			LOG.info("mDNS address: " + jmDns.getInetAddress());
			server = new ProtoStorageMockServer<>(storage, jmDns);
			client = new ProtoStorageMockClient<>(contentSrc, jmDns);
		} catch(final IOException | OmgDoesNotPerformException | OmgLookAtMyConsoleException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to create storage mock node");
		}
	}

	@Override
	public StorageMockClient<MutableDataItemMock> client() {
		return client;
	}

	@Override
	public StorageMockServer<MutableDataItemMock> server() {
		return server;
	}

	@Override
	protected void doStart()
	throws IllegalStateException {
		try {
			server.start();
			client.start();
		} catch(final RemoteException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	protected void doShutdown()
	throws IllegalStateException {
		try {
			server.shutdown();
			client.shutdown();
		} catch(final RemoteException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	protected void doInterrupt()
	throws IllegalStateException {
		try {
			server.interrupt();
			client.interrupt();
		} catch(final RemoteException e) {
			throw new IllegalStateException(e);
		}
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
	protected void doClose()
	throws IOException {
		server.close();
		client.close();
		jmDns.close();
	}
}
