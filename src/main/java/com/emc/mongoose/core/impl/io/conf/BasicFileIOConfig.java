package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
//
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.impl.container.BasicDirectory;
import com.emc.mongoose.core.impl.data.BasicFileItem;
import com.emc.mongoose.core.impl.data.model.DirectoryItemSrc;
import org.apache.commons.lang.StringUtils;
//
import java.io.IOException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOConfig<F extends FileItem, D extends Directory<F>>
extends IOConfigBase<F, D>
implements FileIOConfig<F, D> {
	//
	private int batchSize = RunTimeConfig.getContext().getBatchSize();
	//
	public BasicFileIOConfig() {
		super();
	}
	//
	public BasicFileIOConfig(final BasicFileIOConfig<F, D> another) {
		super(another);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicFileIOConfig<F, D> setProperties(final RunTimeConfig rtConfig) {
		super.setProperties(rtConfig);
		setContainer((D) new BasicDirectory<F>(getNamePrefix()));
		batchSize = rtConfig.getBatchSize();
		return this;
	}
	//
	@Override
	public ItemSrc<F> getContainerListInput(final long maxCount, final String addr) {
		return new DirectoryItemSrc<>(
			container, getItemClass(), maxCount, batchSize, contentSrc
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<D> getContainerClass() {
		return (Class) BasicDirectory.class;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<F> getItemClass() {
		return (Class<F>) BasicFileItem.class;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
	//
	@Override
	public String toString() {
	return "FS-" + StringUtils.capitalize(loadType.name().toLowerCase());
	}
}
