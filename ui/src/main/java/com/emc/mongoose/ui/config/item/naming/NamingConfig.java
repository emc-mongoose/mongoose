package com.emc.mongoose.ui.config.item.naming;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class NamingConfig
implements Serializable {

	public static final String KEY_TYPE = "type";
	public static final String KEY_PREFIX = "prefix";
	public static final String KEY_RADIX = "radix";
	public static final String KEY_OFFSET = "offset";
	public static final String KEY_LENGTH = "length";

	public final void setType(final String type) {
		this.type = type;
	}

	public final void setPrefix(final String prefix) {
		this.prefix = prefix;
	}

	public final void setRadix(final int radix) {
		this.radix = radix;
	}

	public final void setOffset(final long offset) {
		this.offset = offset;
	}

	public final void setLength(final int length) {
		this.length = length;
	}

	@JsonProperty(KEY_TYPE) private String type;
	@JsonProperty(KEY_PREFIX) private String prefix;
	@JsonProperty(KEY_RADIX) private int radix;
	@JsonProperty(KEY_OFFSET) private long offset;
	@JsonProperty(KEY_LENGTH) private int length;

	public NamingConfig() {
	}

	public NamingConfig(final NamingConfig other) {
		this.type = other.getType();
		this.prefix = other.getPrefix();
		this.radix = other.getRadix();
		this.offset = other.getOffset();
		this.length = other.getLength();
	}

	public final String getType() {
		return type;
	}

	public final String getPrefix() {
		return prefix;
	}

	public final int getRadix() {
		return radix;
	}

	public final long getOffset() {
		return offset;
	}

	public final int getLength() {
		return length;
	}
}