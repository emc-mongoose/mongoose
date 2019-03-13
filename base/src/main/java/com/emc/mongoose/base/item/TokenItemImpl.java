package com.emc.mongoose.base.item;

/** Created by andrey on 28.01.17. */
public final class TokenItemImpl extends ItemImpl implements TokenItem {

	public TokenItemImpl() {}

	public TokenItemImpl(final String value) {
		super(value);
	}

	@Override
	public final boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof TokenItemImpl)) {
			return false;
		}
		final TokenItemImpl other = (TokenItemImpl) o;
		if (name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}
}
