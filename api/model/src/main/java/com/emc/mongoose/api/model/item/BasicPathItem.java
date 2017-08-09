package com.emc.mongoose.api.model.item;

/**
 Created by andrey on 28.01.17.
 */
public final class BasicPathItem
extends BasicItem
implements PathItem {

	public BasicPathItem() {
	}

	public BasicPathItem(final String value) {
		super(value);
	}

	@Override
	public final boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicPathItem)) {
			return false;
		}
		final BasicPathItem other = (BasicPathItem) o;
		if(name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}
}
