package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.io.value.RangePatternDefinedInput;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
//
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.BasicFile;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.core.impl.item.data.DirectoryItemInput;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIoConfig<F extends FileItem, D extends Directory<F>>
extends IoConfigBase<F, D>
implements FileIoConfig<F, D> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private Input<String> pathInput = null;
	private int batchSize = BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize();
	//
	public BasicFileIoConfig() {
		super();
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathInput = new RangePatternDefinedInput(containerName);
			}
		}
	}
	//
	public BasicFileIoConfig(final BasicFileIoConfig<F, D> another) {
		super(another);
		pathInput = another.pathInput;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicFileIoConfig<F, D> setAppConfig(final AppConfig appConfig) {
		// note that it's incorrect to invoke super here
		this.appConfig = appConfig;
		setLoadType(appConfig.getLoadType());
		setNameSpace(appConfig.getStorageHttpNamespace());
		setNamePrefix(appConfig.getItemNamingPrefix());
		try {
			setContentSource(ContentSourceBase.getInstance(appConfig));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to apply the content source");
		}
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
	public final BasicFileIoConfig<F, D> setContainer(final D container) {
		super.setContainer(container);
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathInput = new RangePatternDefinedInput(containerName);
			}
		}
		return this;
	}
	//
	@Override
	public Input<F> getContainerListInput(final long maxCount, final String addr) {
		return new DirectoryItemInput<>(
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
		if(pathInput == null) {
			if(container == null) {
				return null;
			} else {
				return container.getName();
			}
		} else {
			try {
				return pathInput.get();
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to get the target item path");
				return null;
			}
		}
	}
	//
	@Override
	public String toString() {
	return "FS-" + StringUtils.capitalize(loadType.name().toLowerCase());
	}
}
