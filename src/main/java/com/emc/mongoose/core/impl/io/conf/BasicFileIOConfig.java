package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
//
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.BasicFile;
import com.emc.mongoose.core.impl.item.data.DirectoryItemSrc;
//
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
	private int batchSize = BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize();
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
	public BasicFileIOConfig<F, D> setAppConfig(final AppConfig appConfig) {
		super.setAppConfig(appConfig);
		final String dirName = appConfig.getItemContainerName();
		if(dirName != null && !dirName.isEmpty()) {
			setContainer((D) new BasicDirectory<F>(dirName));
		} else {
			setContainer(null);
		}
		batchSize = appConfig.getItemSrcBatchSize();
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
		return (Class<F>) BasicFile.class;
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
