package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.io.conf.IOConfig;
//
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
//
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 23.11.15.
 */
public abstract class IOConfigBase<T extends DataItem, C extends Container<T>>
implements IOConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AtomicBoolean closeFlag = new AtomicBoolean(false);
	//
	protected AppConfig.LoadType loadType;
	protected C container;
	protected ContentSource contentSrc;
	protected volatile boolean verifyContentFlag;
	protected volatile AppConfig appConfig;
	protected volatile String nameSpace;
	protected volatile String namePrefix;
	protected int buffSize;
	@Deprecated protected int reqSleepMilliSec = 0;
	protected int nameRadix = Character.MAX_RADIX;
	//
	protected IOConfigBase() {
		appConfig = BasicConfig.THREAD_CONTEXT.get();
		loadType = AppConfig.LoadType.WRITE;
		container = null;
		contentSrc = ContentSourceBase.getDefaultInstance();
		verifyContentFlag = appConfig.getItemDataVerify();
		nameSpace = appConfig.getStorageHttpNamespace();
		namePrefix = appConfig.getItemNamingPrefix();
		nameRadix = appConfig.getItemNamingRadix();
		buffSize = appConfig.getIoBufferSizeMin();
	}
	//
	protected IOConfigBase(final IOConfigBase<T, C> ioConf2Clone) {
		this();
		if(ioConf2Clone != null) {
			setLoadType(ioConf2Clone.getLoadType());
			setContentSource(ioConf2Clone.getContentSource());
			setVerifyContentFlag(ioConf2Clone.getVerifyContentFlag());
			setLoadType(ioConf2Clone.getLoadType());
			setContainer(ioConf2Clone.getContainer());
			setNameSpace(ioConf2Clone.getNameSpace());
			setNamePrefix(ioConf2Clone.namePrefix);
			setNameRadix(ioConf2Clone.getNameRadix());
			setBuffSize(ioConf2Clone.getBuffSize());
			this.reqSleepMilliSec = ioConf2Clone.reqSleepMilliSec;
		}
	}
	//
	@Override
	@SuppressWarnings("unchecked")
	public IOConfigBase<T, C> clone()
		throws CloneNotSupportedException {
		final IOConfigBase<T, C> ioConf = (IOConfigBase<T, C>) super.clone();
		ioConf
			.setLoadType(loadType)
			.setContentSource(contentSrc)
			.setVerifyContentFlag(verifyContentFlag)
			.setLoadType(loadType)
			.setContainer(container)
			.setNameSpace(nameSpace)
			.setNamePrefix(namePrefix)
			.setNameRadix(nameRadix)
			.setBuffSize(buffSize)
			.reqSleepMilliSec = reqSleepMilliSec;
		return ioConf;
	}
	//
	@Override
	public void close()
	throws IOException {
		if(closeFlag.compareAndSet(false, true)) {
			LOG.debug(Markers.MSG, "Request config instance #{} marked as closed", hashCode());
		}
	}
	//
	@Override
	public final boolean isClosed() {
		return closeFlag.get();
	}
	//
	@Override
	protected void finalize()
	throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}
	//
	@Override
	public final AppConfig.LoadType getLoadType() {
		return loadType;
	}
	//
	@Override
	public IOConfigBase<T, C> setLoadType(final AppConfig.LoadType loadType) {
		LOG.trace(Markers.MSG, "Setting load type {}", loadType);
		this.loadType = loadType;
		return this;
	}
	//
	@Override
	public final String getNameSpace() {
		return nameSpace;
	}
	//
	@Override
	public IOConfigBase<T, C> setNameSpace(final String nameSpace) {
		this.nameSpace = nameSpace;
		return this;
	}
	//
	@Override
	public String getNamePrefix() {
		return namePrefix;
	}
	//
	@Override
	public IOConfigBase<T, C> setNamePrefix(final String namePrefix) {
		this.namePrefix = namePrefix;
		return this;
	}
	//
	@Override
	public int getNameRadix() {
		return nameRadix;
	}
	//
	@Override
	public IOConfigBase<T, C> setNameRadix(final int nameRadix) {
		this.nameRadix = nameRadix;
		return this;
	}
	//
	@Override
	public final ContentSource getContentSource() {
		return contentSrc;
	}
	//
	@Override
	public IOConfigBase<T, C> setContentSource(final ContentSource dataSrc) {
		this.contentSrc = dataSrc;
		return this;
	}
	//
	@Override
	public final C getContainer() {
		return container;
	}
	//
	@Override
	public IOConfigBase<T, C> setContainer(final C container) {
		this.container = container;
		return this;
	}
	//
	@Override
	public final boolean getVerifyContentFlag() {
		return verifyContentFlag;
	}
	//
	@Override
	public final IOConfigBase<T, C> setVerifyContentFlag(final boolean verifyContentFlag) {
		this.verifyContentFlag = verifyContentFlag;
		return this;
	}
	//
	@Override
	public final int getBuffSize() {
		return buffSize;
	}
	//
	@Override
	public final IOConfigBase<T, C> setBuffSize(final int buffSize) {
		this.buffSize = buffSize;
		return this;
	}
	//
	public IOConfigBase<T, C> setAppConfig(final AppConfig appConfig) {
		this.appConfig = appConfig;
		setLoadType(appConfig.getLoadType());
		final String newContainerName = appConfig.getItemContainerName();
		if(newContainerName != null && !newContainerName.isEmpty()) {
			setContainer((C) new BasicContainer<T>(newContainerName));
		} else {
			setContainer(null);
		}
		setNameSpace(appConfig.getStorageHttpNamespace());
		setNamePrefix(appConfig.getItemNamingPrefix());
		setVerifyContentFlag(appConfig.getItemDataVerify());
		setBuffSize(appConfig.getIoBufferSizeMin());
		return this;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(getLoadType());
		LOG.trace(Markers.MSG, "Written load type \"" + loadType + "\"");
		out.writeObject(getContainer());
		LOG.trace(Markers.MSG, "Written container \"" + container + "\"");
		out.writeObject(getNameSpace());
		LOG.trace(Markers.MSG, "Written namespace \"" + nameSpace + "\"");
		out.writeObject(getNamePrefix());
		LOG.trace(Markers.MSG, "Written name prefix \"" + namePrefix + "\"");
		out.writeObject(getContentSource());
		LOG.trace(Markers.MSG, "Written content src \"" + contentSrc + "\"");
		out.writeBoolean(getVerifyContentFlag());
		LOG.trace(Markers.MSG, "Written flag");
		out.writeInt(getBuffSize());
		LOG.trace(Markers.MSG, "Written buffer size \"" + buffSize + "\"");
		out.writeInt(reqSleepMilliSec);
		LOG.trace(Markers.MSG, "Written req sleep time \"" + reqSleepMilliSec + "\"");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		setLoadType(AppConfig.LoadType.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got load type {}", loadType);
		setContainer((C) in.readObject());
		LOG.trace(Markers.MSG, "Got container {}", container);
		setNameSpace((String) in.readObject());
		LOG.trace(Markers.MSG, "Got namespace {}", nameSpace);
		setNamePrefix((String) in.readObject());
		LOG.trace(Markers.MSG, "Got name prefix {}", namePrefix);
		setContentSource((ContentSource) in.readObject());
		LOG.trace(Markers.MSG, "Got data source {}", contentSrc);
		setVerifyContentFlag(in.readBoolean());
		LOG.trace(Markers.MSG, "Got verify content flag {}", verifyContentFlag);
		setBuffSize(in.readInt());
		LOG.trace(Markers.MSG, "Got buff size {}", buffSize);
		reqSleepMilliSec = in.readInt();
		LOG.trace(Markers.MSG, "Got request interval {}", reqSleepMilliSec);
	}
	//
	@Override
	public String toString() {
		return StringUtils.capitalize(loadType.name().toLowerCase());
	}
}
