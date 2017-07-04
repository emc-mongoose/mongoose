package com.emc.mongoose.ui.config.item.data.content.ring;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class RingConfig
implements Serializable {

	public static final String KEY_CACHE = "cache";
	public static final String KEY_SIZE = "size";

	@JsonProperty(KEY_CACHE) private int cache;

	@JsonProperty(KEY_SIZE)
	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	private SizeInBytes size;

	public final void setCache(final int cache) {
		this.cache = cache;
	}

	public final void setSize(final SizeInBytes size) {
		this.size = size;
	}

	public RingConfig() {
	}

	public RingConfig(final RingConfig other) {
		this.cache = other.getCache();
		this.size = other.getSize();
	}

	public final int getCache() {
		return cache;
	}

	public final SizeInBytes getSize() {
		return size;
	}
}