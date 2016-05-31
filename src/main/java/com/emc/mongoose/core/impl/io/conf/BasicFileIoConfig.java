package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
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

import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;

/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIoConfig<F extends FileItem, D extends Directory<F>>
extends IoConfigBase<F, D>
implements FileIoConfig<F, D> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private int batchSize = BasicConfig.THREAD_CONTEXT.get().getItemSrcBatchSize();
	//
	public BasicFileIoConfig() {
		this(BasicConfig.THREAD_CONTEXT.get());
		if(dstContainer != null) {
			final String containerName = dstContainer.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathInput = new RangePatternDefinedInput(containerName);
			}
		}
	}
	//
	public BasicFileIoConfig(final AppConfig appConfig) {
		super(appConfig);
	}
	//
	public BasicFileIoConfig(final BasicFileIoConfig<F, D> another) {
		super(another);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicFileIoConfig<F, D> setAppConfig(final AppConfig appConfig) {
		// note that it's incorrect to invoke super here
		this.appConfig = appConfig;
		setLoadType(appConfig.getLoadType());
		setNameSpace(appConfig.getStorageHttpNamespace());
		setItemNamingPrefix(appConfig.getItemNamingPrefix());
		setItemNamingLength(appConfig.getItemNamingLength());
		setItemNamingRadix(appConfig.getItemNamingRadix());
		setItemNamingOffset(appConfig.getItemNamingOffset());
		try {
			setContentSource(ContentSourceBase.getInstance(appConfig));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to apply the content source");
		} catch(final OutOfMemoryError e) {
			e.printStackTrace(System.out);
		}
		setVerifyContentFlag(appConfig.getItemDataVerify());
		final SizeInBytes sizeInfo = appConfig.getItemDataSize();
		final long avgDataSize = sizeInfo.getAvgDataSize();
		setBuffSize(
			avgDataSize < BUFF_SIZE_LO ?
				BUFF_SIZE_LO :
				avgDataSize > BUFF_SIZE_HI ?
					BUFF_SIZE_HI :
					(int) avgDataSize
		);
		final String dstDir = appConfig.getItemDstContainer();
		if(dstDir != null && !dstDir.isEmpty()) {
			setDstContainer((D) new BasicDirectory<F>(dstDir));
		} else {
			setDstContainer(null);
		}
		final String srcDir = appConfig.getItemSrcContainer();
		if(srcDir != null && !srcDir.isEmpty()) {
			setSrcContainer((D) new BasicDirectory<F>(srcDir));
		} else {
			setSrcContainer(null);
		}
		batchSize = appConfig.getItemSrcBatchSize();
		return this;
	}
	//
	@Override
	public final BasicFileIoConfig<F, D> setDstContainer(final D container) {
		super.setDstContainer(container);
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathInput = new RangePatternDefinedInput(containerName);
			}
		}
		return this;
	}
	/*
	@Override
	public final BasicFileIoConfig<F, D> setSrcContainer(final D container) {
		super.setSrcContainer(container);
		if(container != null) {
			final String containerName = container.getName();
			if(containerName != null && !containerName.isEmpty()) {
				pathInput = new RangePatternDefinedInput(containerName);
			}
		}
		return this;
	}*/
	//
	@Override
	public Input<F> getContainerListInput(final long maxCount, final String addr) {
		return srcContainer == null ? null : new DirectoryItemInput<>(
			srcContainer, getItemClass(), maxCount, batchSize, contentSrc
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
	public String toString() {
	return "FS-" + StringUtils.capitalize(loadType.name().toLowerCase());
	}
}
