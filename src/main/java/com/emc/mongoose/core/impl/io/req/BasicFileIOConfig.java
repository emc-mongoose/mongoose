package com.emc.mongoose.core.impl.io.req;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.impl.container.BasicDirectory;
import com.emc.mongoose.core.impl.data.BasicFileItem;
import com.emc.mongoose.core.impl.data.model.DirectoryItemSrc;

import java.io.IOException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOConfig<T extends FileItem, C extends Directory<T>>
extends IOConfigBase<T, C> {
	//
	private int batchSize = RunTimeConfig.getContext().getBatchSize();
	//
	public BasicFileIOConfig() {
		super();
	}
	//
	public BasicFileIOConfig(final BasicFileIOConfig<T, C> another) {
		super(another);
	}
	//
	@Override
	public BasicFileIOConfig<T, C> setProperties(final RunTimeConfig rtConfig) {
		super.setProperties(rtConfig);
		batchSize = rtConfig.getBatchSize();
		return this;
	}
	//
	@Override
	public ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return new DirectoryItemSrc<>(
			container, getItemClass(), maxCount, batchSize, contentSrc
		);
	}
	//
	@Override
	public Class<C> getContainerClass() {
		return (Class<C>) BasicDirectory.class;
	}
	//
	@Override
	public Class<T> getItemClass() {
		return (Class<T>) BasicFileItem.class;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
}
