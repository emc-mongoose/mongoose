package com.emc.mongoose.base.item;

/** Created by andrey on 28.01.17. */
public final class PathItemImpl extends ItemImpl implements PathItem {

	public PathItemImpl() {}

	public PathItemImpl(final String value) {
		super(value);
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PathItemImpl)) {
			return false;
		}
		final PathItemImpl other = (PathItemImpl) o;
		if (name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}
}
