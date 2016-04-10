package com.emc.mongoose.core.impl.item.container;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.item.data.FileItem;
/**
 Created by andrey on 22.11.15.
 */
public class BasicDirectory<T extends FileItem>
extends BasicContainer<T>
implements Directory<T> {
	//
	public BasicDirectory() {
		super();
	}
	//
	public BasicDirectory(final String name) {
		super(name);
	}
	//
	public BasicDirectory(final String name, final ContentSource contentSrc) {
		super(name, contentSrc);
	}
}
