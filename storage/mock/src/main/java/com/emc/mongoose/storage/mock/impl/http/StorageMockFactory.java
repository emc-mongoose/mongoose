package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.common.exception.OmgDoesNotPerformException;
import com.emc.mongoose.common.exception.OmgLookAtMyConsoleException;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockNode;
import com.emc.mongoose.storage.mock.api.StorageMockServer;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockClient;
import com.emc.mongoose.storage.mock.impl.base.BasicStorageMockServer;
import com.emc.mongoose.storage.mock.impl.http.request.AtmosRequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.S3RequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.SwiftRequestHandler;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import io.netty.channel.ChannelInboundHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jmdns.JmDNS;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created on 07.09.16.
 */
public class StorageMockFactory {

	private static final Logger LOG = LogManager.getLogger();

	private final Config.StorageConfig storageConfig;
	private final Config.LoadConfig loadConfig;
	private final Config.ItemConfig itemConfig;
	private final Config.LoadConfig.LimitConfig limitConfig;
	private final Config.ItemConfig.NamingConfig namingConfig;
	private ContentSource contentSource;

	public StorageMockFactory(final Config.StorageConfig storageConfig, final Config.LoadConfig loadConfig,
		final Config.ItemConfig itemConfig) {
		this.storageConfig = storageConfig;
		this.loadConfig = loadConfig;
		this.itemConfig = itemConfig;
		this.limitConfig = loadConfig.getLimitConfig();
		this.namingConfig = itemConfig.getNamingConfig();
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig =
			itemConfig.getDataConfig().getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		try {
			this.contentSource = ContentSourceUtil.getInstance(
				contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
			);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to get content source on path {}", contentSourcePath
			);
			throw new IllegalStateException();
		}
	}

	public StorageMockNode newNagainaNode()
	throws RemoteException {
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<MutableDataItemMock> storage =
			new Nagaina(storageConfig, loadConfig, itemConfig, contentSource, handlers);
		final StorageMockNode<MutableDataItemMock, StorageMockServer<MutableDataItemMock>>
			storageMockNode = new BasicStorageMockNode(storage);
		final StorageMockClient<MutableDataItemMock, StorageMockServer<MutableDataItemMock>>
			client = storageMockNode.client();
		handlers.add(
			new SwiftRequestHandler<>(limitConfig, namingConfig, storage, client, contentSource)
		);
		handlers.add(
			new AtmosRequestHandler<>(limitConfig, namingConfig, storage, client, contentSource)
		);
		handlers.add(
			new S3RequestHandler<>(limitConfig, namingConfig, storage, client, contentSource)
		);
		return storageMockNode;
	}

	public StorageMock newNagaina() {
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<MutableDataItemMock> storage =
			new Nagaina(storageConfig, loadConfig, itemConfig, contentSource, handlers);
		try {
			handlers.add(
				new SwiftRequestHandler<>(limitConfig, namingConfig, storage, null, contentSource)
			);
			handlers.add(
				new AtmosRequestHandler<>(limitConfig, namingConfig, storage, null, contentSource)
			);
			handlers.add(
				new S3RequestHandler<>(limitConfig, namingConfig, storage, null, contentSource)
			);
		} catch(final RemoteException ignore) {
		}
		return storage;
	}

	private static class BasicStorageMockNode
	implements StorageMockNode<MutableDataItemMock, StorageMockServer<MutableDataItemMock>> {

		private static final Logger LOG = LogManager.getLogger();

		private JmDNS jmDns;
		private StorageMockClient<MutableDataItemMock, StorageMockServer<MutableDataItemMock>> client;
		private StorageMockServer<MutableDataItemMock> server;

		public BasicStorageMockNode(final StorageMock<MutableDataItemMock> storage) {
//			System.setProperty("java.rmi.server.hostname", NetUtil.getHostAddrString()); workaround
			try {
				jmDns = JmDNS.create(NetUtil.getHostAddr());
				LOG.info("mDNS address: " + jmDns.getInetAddress());
				server = new BasicStorageMockServer<>(storage, jmDns);
				client = new BasicStorageMockClient<>(jmDns);
			} catch(final IOException | OmgDoesNotPerformException | OmgLookAtMyConsoleException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to create storage mock node"
				);
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
		public void start() throws UserShootHisFootException {
			try {
				server.start();
			} catch(final RemoteException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to start storage mock server"
				);
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

}
