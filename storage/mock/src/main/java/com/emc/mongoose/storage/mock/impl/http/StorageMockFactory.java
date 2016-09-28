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
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import io.netty.channel.ChannelInboundHandler;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 Created on 07.09.16.
 */
public class StorageMockFactory {

	private final StorageConfig storageConfig;
	private final LoadConfig loadConfig;
	private final ItemConfig itemConfig;
	private final LoadConfig.LimitConfig limitConfig;
	private final ItemConfig.NamingConfig namingConfig;

	public StorageMockFactory(
		final StorageConfig storageConfig, final LoadConfig loadConfig, final ItemConfig itemConfig
	) {
		this.storageConfig = storageConfig;
		this.loadConfig = loadConfig;
		this.itemConfig = itemConfig;
		this.limitConfig = loadConfig.getLimitConfig();
		this.namingConfig = itemConfig.getNamingConfig();
	}

	public StorageMockNode newNagainaNode()
	throws IOException {
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSrc = ContentSourceUtil.getInstance(
			contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
		);
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<MutableDataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, contentSrc, handlers
		);
		final StorageMockNode<MutableDataItemMock> storageMockNode = new NagainaNode(
			storage, contentSrc
		);
		final StorageMockClient<MutableDataItemMock> client = storageMockNode.client();
		handlers.add(
			new SwiftRequestHandler<>(limitConfig, namingConfig, storage, client)
		);
		handlers.add(
			new AtmosRequestHandler<>(limitConfig, namingConfig, storage, client)
		);
		handlers.add(
			new S3RequestHandler<>(limitConfig, namingConfig, storage, client)
		);
		return storageMockNode;
	}

	public StorageMock newNagaina()
	throws IOException {
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSrc = ContentSourceUtil.getInstance(
			contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
		);
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<MutableDataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, contentSrc, handlers
		);
		try {
			handlers.add(
				new SwiftRequestHandler<>(limitConfig, namingConfig, storage, null)
			);
			handlers.add(
				new AtmosRequestHandler<>(limitConfig, namingConfig, storage, null)
			);
			handlers.add(
				new S3RequestHandler<>(limitConfig, namingConfig, storage, null)
			);
		} catch(final RemoteException ignore) {
		}
		return storage;
	}
}
