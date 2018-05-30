package com.emc.mongoose.storage.driver.coop.net.http;

import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.Item;

import com.emc.mongoose.storage.driver.coop.net.NetStorageDriver;

/**
 Created by kurila on 30.08.16.
 */
public interface HttpStorageDriver<I extends Item, O extends IoTask<I>>
extends NetStorageDriver<I, O> {
	
	int REQ_LINE_LEN = 1024;
	int HEADERS_LEN = 2048;
	int CHUNK_SIZE = 8192;

	String KEY_CONTENT = "content";
}
