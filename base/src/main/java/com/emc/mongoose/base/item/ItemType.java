package com.emc.mongoose.base.item;

/** Created by kurila on 28.03.16. */
public enum ItemType {
	DATA, PATH, TOKEN;

	@SuppressWarnings("unchecked")
	public static <I extends Item, F extends ItemFactory<I>> F getItemFactory(
					final ItemType itemType) {
		if (ItemType.DATA.equals(itemType)) {
			return (F) new DataItemFactoryImpl<DataItemImpl>();
		} else if (ItemType.PATH.equals(itemType)) {
			return (F) new PathItemFactoryImpl<PathItemImpl>();
		} else if (ItemType.TOKEN.equals(itemType)) {
			return (F) new TokenItemFactoryImpl<TokenItemImpl>();
		} else {
			throw new AssertionError("Item type \"" + itemType + "\" is not supported");
		}
	}
}
