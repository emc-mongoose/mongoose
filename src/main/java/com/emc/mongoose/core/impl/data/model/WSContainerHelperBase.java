package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ContainerHelper;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
//
import com.emc.mongoose.core.impl.container.BasicContainer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
/**
 Created by kurila on 10.07.15.
 */
public abstract class WSContainerHelperBase<T extends WSObject, C extends Container<T>>
extends BasicContainer<T>
implements ContainerHelper<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final WSRequestConfig<T, C> reqConf;
	protected final String name, idPrefix;
	protected final int idPrefixLen;
	protected final boolean fsAccess, verifyContent;
	//
	protected WSContainerHelperBase(
		final WSRequestConfig<T, C> reqConf, final String name,
		final boolean versioningEnabled
	) {
		this.reqConf = reqConf;
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			this.name = Constants.MONGOOSE_PREFIX + LogUtil.FMT_DT.format(dt);
		} else {
			this.name = name;
		}
		this.fsAccess = reqConf.getFileAccessEnabled();
		this.idPrefix = reqConf.getNamePrefix();
		idPrefixLen = idPrefix == null ? 0 : idPrefix.length();
		this.verifyContent = reqConf.getVerifyContentFlag();
	}
	//
	@Override
	public final T buildItem(
		final Constructor<T> itemConstructor, final String rawId, final long size
	) throws IllegalStateException {
		//
		T item = null;
		//
		String name = null;
		if(rawId != null && !rawId.isEmpty()) {
			if(fsAccess) { // include the items which have the path matching to configured one
				if(rawId.startsWith(idPrefix) && rawId.length() > idPrefixLen) {
					name = rawId.substring(idPrefixLen + 1);
					if(name.contains("/")) { // doesn't include the items from the subdirectories
						name = null;
					}
				}
			} else {
				if(!rawId.contains("/")) { // doesn't include the items from the directories
					name = rawId;
				}
			}
		}
		//
		if(name != null) {
			final long offset;
			if(verifyContent) { // should parse the id into the ring buffer offset
				try {
					offset = Long.parseLong(name, T.ID_RADIX);
				} catch(NumberFormatException e) {
					throw new IllegalStateException(e);
				}
			} else { // ring buffer offset doesn't matter
				offset = 0;
			}
			try {
				item = itemConstructor.newInstance(name, offset, size, 0, reqConf.getContentSource());
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IllegalStateException(e);
			}
		}
		//
		return item;
	}
}
