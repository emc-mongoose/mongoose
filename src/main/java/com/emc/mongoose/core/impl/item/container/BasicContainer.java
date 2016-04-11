package com.emc.mongoose.core.impl.item.container;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import com.emc.mongoose.core.impl.item.base.BasicItem;
/**
 Created by kurila on 20.10.15.
 */
public class BasicContainer<T extends DataItem>
extends BasicItem
implements Container<T> {
	//
	protected volatile ContentSource contentSrc;
	//
	public BasicContainer() {
		super();
	}
	//
	public BasicContainer(final String name) {
		this.name = name;
	}
	//
	public BasicContainer(final String name, final ContentSource contentSrc) {
		this.name = name;
		this.contentSrc = contentSrc;
	}
}
