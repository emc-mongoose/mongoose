package com.emc.mongoose.core.impl.item.container;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
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
	public BasicDirectory(final String path, final String name) {
		super(path, name);
	}
	//
	public BasicDirectory(final String path, final String name, final ContentSource contentSrc) {
		super(path, name, contentSrc);
	}
}
