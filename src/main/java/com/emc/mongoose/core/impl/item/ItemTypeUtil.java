package com.emc.mongoose.core.impl.item;
//
//
import com.emc.mongoose.common.conf.enums.ItemType;
import com.emc.mongoose.common.conf.enums.StorageType;
import com.emc.mongoose.core.api.item.base.Item;
//
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.BasicFile;
import com.emc.mongoose.core.impl.item.data.BasicHttpData;
/**
 Created by kurila on 17.03.16.
 */
public abstract class ItemTypeUtil {
	//
	@SuppressWarnings("unchecked")
	public static <T extends Item> Class<T> getItemClass(
		final ItemType itemType, final StorageType storageType
	) {
		if(ItemType.CONTAINER.equals(itemType)) {
			if(StorageType.FS.equals(storageType)) {
				return (Class<T>) BasicDirectory.class;
			} else { // http
				return (Class<T>) BasicContainer.class;
			}
		} else { // data
			if(StorageType.FS.equals(storageType)) {
				return (Class<T>) BasicFile.class;
			} else { // http
				return (Class<T>) BasicHttpData.class;
			}
		}
	}
	//
	private ItemTypeUtil() {}
}
