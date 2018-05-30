package com.emc.mongoose.item;

/**
 Created by kurila on 28.03.16.
 */
public enum ItemType {
	
	DATA,
	PATH,
	TOKEN;
	
	@SuppressWarnings("unchecked")
	public static <I extends Item, F extends ItemFactory<I>> F getItemFactory(
		final ItemType itemType
	) {
		if(ItemType.DATA.equals(itemType)) {
			return (F) new BasicDataItemFactory<BasicDataItem>();
		} else if(ItemType.PATH.equals(itemType)) {
			return (F) new BasicPathItemFactory<BasicPathItem>();
		} else if(ItemType.TOKEN.equals(itemType)) {
			return (F) new BasicTokenItemFactory<BasicTokenItem>();
		} else {
			throw new AssertionError("Item type \"" + itemType + "\" is not supported");
		}
	}
}
