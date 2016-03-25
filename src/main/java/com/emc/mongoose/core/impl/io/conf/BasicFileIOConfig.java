package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.generator.FormatRangeGenerator;
import com.emc.mongoose.common.generator.ValueGenerator;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOConfig<F extends FileItem, D extends Directory<F>>
extends IOConfigBase<F, D>
implements FileIOConfig<F, D> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private ValueGenerator<String> pathGenerator = null;
	private int batchSize = BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize();
	//
	public BasicFileIOConfig() {
		super();
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathGenerator = new FormatRangeGenerator(containerName);
			}
		}
	}
	//
	public BasicFileIOConfig(final BasicFileIOConfig<F, D> another) {
		super(another);
		pathGenerator = another.pathGenerator;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicFileIOConfig<F, D> setAppConfig(final AppConfig appConfig) {
		this.appConfig = appConfig;
		setLoadType(appConfig.getLoadType());
		setNameSpace(appConfig.getStorageHttpNamespace());
		setNamePrefix(appConfig.getItemNamingPrefix());
		setVerifyContentFlag(appConfig.getItemDataVerify());
		setBuffSize(appConfig.getIoBufferSizeMin());
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
	public final BasicFileIOConfig<F, D> setContainer(final D container) {
		super.setContainer(container);
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathGenerator = new FormatRangeGenerator(containerName);
			}
		}
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
	public final String getTargetItemPath() {
		return pathGenerator == null ?
			container == null ? null :
				container.getName() :
			pathGenerator.get();
	}
	//
	@Override
	public String toString() {
	return "FS-" + StringUtils.capitalize(loadType.name().toLowerCase());
	}
}
