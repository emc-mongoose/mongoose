package com.emc.mongoose.storage.adapter.s3;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.data.model.GenericContainer;
/**
 Created by kurila on 02.10.14.
 */
public interface Bucket<T extends WSObject>
extends GenericContainer<T> {
	String
		VERSIONING_ENTITY_CONTENT =
		"<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
			"<Status>Enabled</Status></VersioningConfiguration>",
		VERSIONING_URL_PART = "/?versioning",
		URL_ARG_MAX_KEYS = "max-keys",
		URL_ARG_MARKER = "marker";
}
