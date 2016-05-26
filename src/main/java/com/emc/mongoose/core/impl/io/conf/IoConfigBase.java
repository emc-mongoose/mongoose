package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.RangePatternDefinedInput;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.io.conf.IoConfig;
//
import com.emc.mongoose.core.impl.item.base.BasicItemNameInput;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.container.NewContainerInput;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
//
import com.emc.mongoose.core.impl.item.data.NewDataItemInput;
import org.apache.commons.lang.StringUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;

/**
 Created by kurila on 23.11.15.
 */
public abstract class IoConfigBase<T extends DataItem, C extends Container<T>>
implements IoConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AtomicBoolean closeFlag = new AtomicBoolean(false);
	//
	protected LoadType loadType;
	protected C dstContainer;
	protected C srcContainer;
	protected Input<String> pathInput = null;
	protected ContentSource contentSrc;
	protected volatile boolean verifyContentFlag;
	protected volatile AppConfig appConfig;
	protected volatile String nameSpace;
	protected volatile String namingPrefix;
	protected int namingLength = 13;
	protected int namingRadix = Character.MAX_RADIX;
	protected long namingOffset = 0;
	protected int buffSize;
	@Deprecated protected int reqSleepMilliSec = 0;
	//
	protected IoConfigBase() {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	protected IoConfigBase(final AppConfig appConfig) {
		setAppConfig(appConfig);
	}
	//
	protected IoConfigBase(final IoConfigBase<T, C> ioConf2Clone) {
		this();
		if(ioConf2Clone != null) {
			setLoadType(ioConf2Clone.getLoadType());
			setContentSource(ioConf2Clone.getContentSource());
			setVerifyContentFlag(ioConf2Clone.getVerifyContentFlag());
			setDstContainer(ioConf2Clone.getDstContainer());
			setSrcContainer(ioConf2Clone.getSrcContainer());
			setNameSpace(ioConf2Clone.getNameSpace());
			setItemNamingPrefix(ioConf2Clone.getItemNamingPrefix());
			setItemNamingLength(ioConf2Clone.getItemNamingLength());
			setItemNamingRadix(ioConf2Clone.getItemNamingRadix());
			setItemNamingOffset(ioConf2Clone.getItemNamingOffset());
			setBuffSize(ioConf2Clone.getBuffSize());
			this.pathInput = ioConf2Clone.pathInput;
			this.reqSleepMilliSec = ioConf2Clone.reqSleepMilliSec;
		}
	}
	//
	@Override
	@SuppressWarnings("unchecked")
	public IoConfigBase<T, C> clone()
		throws CloneNotSupportedException {
		final IoConfigBase<T, C> ioConf = (IoConfigBase<T, C>) super.clone();
		ioConf
			.setLoadType(loadType)
			.setContentSource(contentSrc)
			.setVerifyContentFlag(verifyContentFlag)
			.setDstContainer(dstContainer)
			.setNameSpace(nameSpace)
			.setItemNamingPrefix(namingPrefix)
			.setItemNamingLength(namingLength)
			.setItemNamingRadix(namingRadix)
			.setItemNamingOffset(namingOffset)
			.setBuffSize(buffSize)
			.pathInput = this.pathInput;
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
	public final LoadType getLoadType() {
		return loadType;
	}
	//
	@Override
	public IoConfigBase<T, C> setLoadType(final LoadType loadType) {
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
	public IoConfigBase<T, C> setNameSpace(final String nameSpace) {
		this.nameSpace = nameSpace;
		return this;
	}
	//
	@Override
	public String getItemNamingPrefix() {
		return namingPrefix;
	}
	//
	@Override
	public IoConfigBase<T, C> setItemNamingPrefix(final String namingPrefix) {
		this.namingPrefix = namingPrefix;
		return this;
	}
	//
	@Override
	public int getItemNamingLength() {
		return namingLength;
	}
	//
	@Override
	public IoConfigBase<T, C> setItemNamingLength(final int namingLength) {
		this.namingLength = namingLength;
		return this;
	}
	//
	@Override
	public int getItemNamingRadix() {
		return namingRadix;
	}
	//
	@Override
	public IoConfigBase<T, C> setItemNamingRadix(final int namingRadix) {
		this.namingRadix = namingRadix;
		return this;
	}
	//
	@Override
	public long getItemNamingOffset() {
		return namingOffset;
	}
	//
	@Override
	public IoConfigBase<T, C> setItemNamingOffset(final long namingOffset) {
		this.namingOffset = namingOffset;
		return this;
	}
	//
	@Override
	public final ContentSource getContentSource() {
		return contentSrc;
	}
	//
	@Override
	public IoConfigBase<T, C> setContentSource(final ContentSource dataSrc) {
		this.contentSrc = dataSrc;
		return this;
	}
	//
	@Override
	public final C getDstContainer() {
		return dstContainer;
	}
	//
	@Override
	public IoConfigBase<T, C> setDstContainer(final C container) {
		this.dstContainer = container;
		return this;
	}
	//
	@Override
	public final C getSrcContainer() {
		return srcContainer;
	}
	//
	@Override
	public IoConfigBase<T, C> setSrcContainer(final C container) {
		this.srcContainer = container;
		return this;
	}
	//
	@Override
	public final boolean getVerifyContentFlag() {
		return verifyContentFlag;
	}
	//
	@Override
	public final IoConfigBase<T, C> setVerifyContentFlag(final boolean verifyContentFlag) {
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
	public final IoConfigBase<T, C> setBuffSize(final int buffSize) {
		this.buffSize = buffSize;
		return this;
	}
	//
	//
	@SuppressWarnings("unchecked")
	public IoConfigBase<T, C> setAppConfig(final AppConfig appConfig) {
		this.appConfig = appConfig;
		setLoadType(appConfig.getLoadType());
		final String dstContainerValue = appConfig.getItemDstContainer();
		if(dstContainerValue != null && !dstContainerValue.isEmpty()) {
			final int firstSlashPos = dstContainerValue.indexOf(Item.SLASH);
			if(firstSlashPos < 0) {
				setDstContainer((C) new BasicContainer<T>(dstContainerValue));
				pathInput = new RangePatternDefinedInput(Item.SLASH);
			} else {
				setDstContainer(
					(C) new BasicContainer<T>(dstContainerValue.substring(0, firstSlashPos))
				);
				pathInput = new RangePatternDefinedInput(
					dstContainerValue.substring(firstSlashPos)
				);
			}
		} else {
			setDstContainer(null);
			pathInput = new RangePatternDefinedInput(Item.SLASH);
		}
		final String srcContainerValue = appConfig.getItemSrcContainer();
		if(srcContainerValue != null && !srcContainerValue.isEmpty()) {
			final int firstSlashPos = dstContainerValue.indexOf(Item.SLASH);
			if(firstSlashPos < 0) {
				setSrcContainer((C) new BasicContainer<T>(srcContainerValue));
				pathInput = new RangePatternDefinedInput(Item.SLASH);
			} else {
				setSrcContainer(
					(C) new BasicContainer<T>(dstContainerValue.substring(0, firstSlashPos))
				);
				pathInput = new RangePatternDefinedInput(
					srcContainerValue.substring(firstSlashPos)
				);
			}
		} else {
			setSrcContainer(null);
		}
		setNameSpace(appConfig.getStorageHttpNamespace());
		setItemNamingPrefix(appConfig.getItemNamingPrefix());
		setItemNamingLength(appConfig.getItemNamingLength());
		setItemNamingRadix(appConfig.getItemNamingRadix());
		setItemNamingOffset(appConfig.getItemNamingOffset());
		try {
			setContentSource(ContentSourceBase.getInstance(appConfig));
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to apply the content source");
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
		return this;
	}
	//
	@Override
	public Input<T> getNewDataItemsInput(
		final ItemNamingType namingType, final Class<T> itemCls, final SizeInBytes sizeInfo
	) throws NoSuchMethodException {
		final BasicItemNameInput itemNameInput = new BasicItemNameInput(
			namingType, getItemNamingPrefix(), getItemNamingLength(), getItemNamingRadix(),
			getItemNamingOffset()
		);
		return new NewDataItemInput<>(
			itemCls, pathInput, itemNameInput, getContentSource(), sizeInfo
		);
	}
	//
	@Override
	public Input<C> getNewContainersInput(final ItemNamingType namingType, final Class<C> itemCls)
	throws NoSuchMethodException {
		final BasicItemNameInput itemNameInput = new BasicItemNameInput(
			namingType, getItemNamingPrefix(), getItemNamingLength(), getItemNamingRadix(),
			getItemNamingOffset()
		);
		return new NewContainerInput<>(itemCls, itemNameInput);
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeObject(getLoadType());
		LOG.trace(Markers.MSG, "Written load type \"" + loadType + "\"");
		out.writeObject(getDstContainer());
		LOG.trace(Markers.MSG, "Written destination container \"" + dstContainer + "\"");
		out.writeObject(getSrcContainer());
		LOG.trace(Markers.MSG, "Written source container \"" + srcContainer + "\"");
		out.writeObject(pathInput);
		LOG.trace(Markers.MSG, "Written path input \"" + pathInput + "\"");
		out.writeObject(getNameSpace());
		LOG.trace(Markers.MSG, "Written namespace \"" + nameSpace + "\"");
		out.writeObject(getItemNamingPrefix());
		LOG.trace(Markers.MSG, "Written name prefix \"" + namingPrefix + "\"");
		out.writeInt(getItemNamingLength());
		LOG.trace(Markers.MSG, "Written name length \"" + namingLength + "\"");
		out.writeInt(getItemNamingRadix());
		LOG.trace(Markers.MSG, "Written name radix \"" + namingRadix + "\"");
		out.writeLong(getItemNamingOffset());
		LOG.trace(Markers.MSG, "Written name offset \"" + namingOffset + "\"");
		out.writeObject(getContentSource());
		LOG.trace(Markers.MSG, "Written content src \"" + contentSrc + "\"");
		out.writeBoolean(getVerifyContentFlag());
		LOG.trace(Markers.MSG, "Written verify content flag \"" + verifyContentFlag + "\"");
		out.writeInt(getBuffSize());
		LOG.trace(Markers.MSG, "Written buffer size \"" + buffSize + "\"");
		out.writeInt(reqSleepMilliSec);
		LOG.trace(Markers.MSG, "Written req sleep time \"" + reqSleepMilliSec + "\"");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		setLoadType(LoadType.class.cast(in.readObject()));
		LOG.trace(Markers.MSG, "Got load type {}", loadType);
		setDstContainer((C) in.readObject());
		LOG.trace(Markers.MSG, "Got destination container {}", dstContainer);
		setSrcContainer((C) in.readObject());
		LOG.trace(Markers.MSG, "Got source container {}", srcContainer);
		pathInput = (Input<String>) in.readObject();
		LOG.trace(Markers.MSG, "Got path input {}", pathInput);
		setNameSpace((String) in.readObject());
		LOG.trace(Markers.MSG, "Got namespace {}", nameSpace);
		setItemNamingPrefix((String) in.readObject());
		LOG.trace(Markers.MSG, "Got name prefix {}", namingPrefix);
		setItemNamingLength(in.readInt());
		LOG.trace(Markers.MSG, "Got name length {}", namingLength);
		setItemNamingRadix(in.readInt());
		LOG.trace(Markers.MSG, "Got name radix {}", namingRadix);
		setItemNamingOffset(in.readLong());
		LOG.trace(Markers.MSG, "Got name offset {}", namingOffset);
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
