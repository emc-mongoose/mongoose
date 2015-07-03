package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.data.model.GenericContainer;
/**
 Created by kurila on 02.10.14.
 */
public interface Bucket<T extends WSObject>
extends GenericContainer<T> {}
