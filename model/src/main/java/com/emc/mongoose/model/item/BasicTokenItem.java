package com.emc.mongoose.model.item;

/**
 Created by andrey on 28.01.17.
 */
public final class BasicTokenItem
extends BasicItem
implements TokenItem {

	public BasicTokenItem() {
	}

	public BasicTokenItem(final String value) {
		super(value);
	}

	@Override
	public final boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicTokenItem)) {
			return false;
		}
		final BasicTokenItem other = (BasicTokenItem) o;
		if(name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}
}
