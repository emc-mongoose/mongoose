package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.impl.data.ContentSourceUtil;
import com.emc.mongoose.storage.mock.api.MutableDataItemMock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageMockClient;
import com.emc.mongoose.storage.mock.api.StorageMockNode;
import com.emc.mongoose.storage.mock.impl.http.request.AtmosRequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.S3RequestHandler;
import com.emc.mongoose.storage.mock.impl.http.request.SwiftRequestHandler;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import io.netty.channel.ChannelInboundHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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
	private ContentSource contentSrc;

	public StorageMockFactory(
		final Config.StorageConfig storageConfig, final Config.LoadConfig loadConfig,
		final Config.ItemConfig itemConfig
	) {
		this.storageConfig = storageConfig;
		this.loadConfig = loadConfig;
		this.itemConfig = itemConfig;
		this.limitConfig = loadConfig.getLimitConfig();
		this.namingConfig = itemConfig.getNamingConfig();
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig =
			itemConfig
				.getDataConfig()
				.getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		try {
			this.contentSrc = ContentSourceUtil.getInstance(
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
		final StorageMock<MutableDataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, contentSrc, handlers
		);
		final StorageMockNode<MutableDataItemMock> storageMockNode = new NagainaNode(
			storage, contentSrc
		);
		final StorageMockClient<MutableDataItemMock> client = storageMockNode.client();
		handlers.add(
			new SwiftRequestHandler<>(limitConfig, namingConfig, storage, client, contentSrc)
		);
		handlers.add(
			new AtmosRequestHandler<>(limitConfig, namingConfig, storage, client, contentSrc)
		);
		handlers.add(
			new S3RequestHandler<>(limitConfig, namingConfig, storage, client, contentSrc)
		);
		return storageMockNode;
	}

	public StorageMock newNagaina() {
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<MutableDataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, contentSrc, handlers
		);
		try {
			handlers.add(
				new SwiftRequestHandler<>(limitConfig, namingConfig, storage, null, contentSrc)
			);
			handlers.add(
				new AtmosRequestHandler<>(limitConfig, namingConfig, storage, null, contentSrc)
			);
			handlers.add(
				new S3RequestHandler<>(limitConfig, namingConfig, storage, null, contentSrc)
			);
		} catch(final RemoteException ignore) {
		}
		return storage;
	}
}
