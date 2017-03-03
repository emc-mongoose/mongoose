package com.emc.mongoose.storage.mock.impl.http;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.storage.mock.api.DataItemMock;
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
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig;

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
	private final StepConfig stepConfig;
	private final NamingConfig namingConfig;

	public StorageMockFactory(
		final StorageConfig storageConfig, final LoadConfig loadConfig, final ItemConfig itemConfig,
		final StepConfig stepConfig
	) {
		this.storageConfig = storageConfig;
		this.loadConfig = loadConfig;
		this.itemConfig = itemConfig;
		this.stepConfig = stepConfig;
		this.namingConfig = itemConfig.getNamingConfig();
	}

	public StorageMockNode newStorageNodeMock()
	throws IOException {
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSrc = ContentSourceUtil.getInstance(
			contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
		);
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<DataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, stepConfig, contentSrc, handlers
		);
		final StorageMockNode<DataItemMock> storageMockNode = new NagainaNode(
			storage, contentSrc
		);
		final StorageMockClient<DataItemMock> client = storageMockNode.client();
		final LimitConfig limitConfig = stepConfig.getLimitConfig();
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

	public StorageMock newStorageMock()
	throws IOException {
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
		final String contentSourcePath = contentConfig.getFile();
		final ContentSource contentSrc = ContentSourceUtil.getInstance(
			contentSourcePath, contentConfig.getSeed(), contentConfig.getRingSize()
		);
		final List<ChannelInboundHandler> handlers = new ArrayList<>();
		final StorageMock<DataItemMock> storage = new Nagaina(
			storageConfig, loadConfig, itemConfig, stepConfig, contentSrc, handlers
		);
		final LimitConfig limitConfig = stepConfig.getLimitConfig();
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
