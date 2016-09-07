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
public class StorageMockNodeFactory {

	private static final Logger LOG = LogManager.getLogger();

	public static StorageMockNode newNagainaNode(
		final Config.StorageConfig storageConfig, final Config.LoadConfig loadConfig,
		final Config.ItemConfig itemConfig
	) throws RemoteException {
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final Config.LoadConfig.LimitConfig limitConfig = loadConfig.getLimitConfig();
		final Config.ItemConfig.NamingConfig namingConfig = itemConfig.getNamingConfig();
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig = itemConfig
			.getDataConfig()
			.getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSource;
		try {
			contentSource = ContentSourceUtil.getInstance(
				contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
			);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Failed to get content source on path {}", contentSourcePath
			);
			throw new IllegalStateException();
		}
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
		public void start() throws UserShootHisFootException, RemoteException {
			server.start();
			client.start();
		}

		@Override
		public boolean isStarted() throws RemoteException {
			return server.isStarted();
		}

		@Override
		public boolean await() throws InterruptedException, RemoteException {
			return server.await();
		}

		@Override
		public boolean await(final long timeout, final TimeUnit timeUnit)
		throws InterruptedException, RemoteException {
			return server.await(timeout, timeUnit);
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
