package com.emc.mongoose.api.model.item;

/**
 Created by kurila on 28.03.16.
 */
public enum ItemType {
	
	DATA,
	PATH,
	TOKEN;
	
	public static ItemFactory<? extends Item> getItemFactory(final ItemType itemType) {
		if(ItemType.DATA.equals(itemType)) {
			return new BasicDataItemFactory<BasicDataItem>();
		} else if(ItemType.PATH.equals(itemType)) {
			return new BasicPathItemFactory<BasicPathItem>();
		} else if(ItemType.TOKEN.equals(itemType)) {
			return new BasicTokenItemFactory<BasicTokenItem>();
		} else {
			throw new AssertionError("Item type \"" + itemType + "\" is not supported");
		}
	}
}
