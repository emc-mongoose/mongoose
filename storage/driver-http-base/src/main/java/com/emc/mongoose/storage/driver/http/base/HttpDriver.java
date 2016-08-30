package com.emc.mongoose.storage.driver.http.base;

import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.model.api.load.Driver;

/**
 Created by kurila on 30.08.16.
 */
public interface HttpDriver<I extends Item, O extends IoTask<I>>
extends Driver<I, O> {
	
	String SIGN_METHOD = "HmacSHA1";
	
}
