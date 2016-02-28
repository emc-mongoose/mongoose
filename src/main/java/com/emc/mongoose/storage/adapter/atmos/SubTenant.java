package com.emc.mongoose.storage.adapter.atmos;
//
import com.emc.mongoose.core.api.item.data.MutableDataItem;
import com.emc.mongoose.storage.adapter.swift.AuthToken;
/**
 Created by kurila on 02.10.14.
 */
public interface SubTenant<T extends MutableDataItem>
extends AuthToken<T> {
	//
	String KEY_SUBTENANT_ID = "subtenantID";
	//
	String getValue();
	//
	boolean exists(final String addr)
	throws IllegalStateException;
	//
	void create(final String addr)
	throws IllegalStateException;
	//
	void delete(final String addr)
	throws IllegalStateException;
}
