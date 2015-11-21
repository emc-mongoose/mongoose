package com.emc.mongoose.core.impl.container;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
/**
 Created by andrey on 22.11.15.
 */
public class BasicDirectory<T extends FileItem>
extends BasicContainer<T>
implements Directory<T> {
}
