package com.emc.mongoose.storage.driver.coop.netty.http;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.Item;

import com.emc.mongoose.storage.driver.coop.netty.NettyStorageDriver;

/**
 Created by kurila on 30.08.16.
 */
public interface HttpStorageDriver<I extends Item, O extends Operation<I>>
extends NettyStorageDriver<I, O> {
	
	int REQ_LINE_LEN = 1024;
	int HEADERS_LEN = 2048;
	int CHUNK_SIZE = 8192;

	String KEY_CONTENT = "content";
}
