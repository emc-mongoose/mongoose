package com.emc.mongoose.storage.driver.net.http.swift;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.net.http.base.HttpStorageDriverBase;
import com.emc.mongoose.ui.config.Config;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.net.URISyntaxException;
/**
 Created by andrey on 07.10.16.
 */
public class SwiftStorageDriver<I extends Item, O extends IoTask<I>>
extends HttpStorageDriverBase<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	// TODO implement

	public SwiftStorageDriver(
		final String runId, final Config.LoadConfig loadConfig,
		final Config.StorageConfig storageConfig, final String srcContainer,
		final boolean verifyFlag, final Config.SocketConfig socketConfig
	) throws IllegalStateException {
		super(runId, loadConfig, storageConfig, srcContainer, verifyFlag, socketConfig);
	}

	@Override
	public final void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException {
	}
}
