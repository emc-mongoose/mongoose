package com.emc.mongoose.core.impl.item.data;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.api.item.data.ContainerHelper;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
/**
 Created by kurila on 10.07.15.
 */
public abstract class WSContainerHelperBase<T extends WSObject, C extends Container<T>>
implements ContainerHelper<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final WSRequestConfig<T, C> reqConf;
	protected final C container;
	protected final String idPrefix;
	protected final int idPrefixLen, idRadix;
	protected final boolean fsAccess, verifyContent;
	//
	protected WSContainerHelperBase(final WSRequestConfig<T, C> reqConf, final C container) {
		this.reqConf = reqConf;
		this.container = container;
		final String name = container.getName();
		if(name == null || name.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			container.setName(Constants.MONGOOSE_PREFIX + LogUtil.FMT_DT.format(dt));
		}
		this.fsAccess = reqConf.getFileAccessEnabled();
		this.idPrefix = reqConf.getNamePrefix();
		this.idRadix = reqConf.getNameRadix();
		idPrefixLen = idPrefix == null ? 0 : idPrefix.length();
		this.verifyContent = reqConf.getVerifyContentFlag();
	}
	//
	public final String toString() {
		return container.getName();
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
				if(idPrefix != null && rawId.startsWith(idPrefix) && rawId.length() > idPrefixLen) {
					name = rawId.substring(idPrefixLen + 1);
					if(name.contains("/")) { // doesn't include the items from the subdirectories
						name = null;
					}
				} else {
					if(!rawId.contains("/")) { // doesn't include the items from the directories
						name = rawId;
					}
				}
			} else {
				if(idPrefix != null && rawId.startsWith(idPrefix) && rawId.length() > idPrefixLen) {
					name = rawId.substring(idPrefixLen);
				} else {
					name = rawId;
				}
			}
		}
		//
		if(name != null) {
			long offset = 0;
			try {
				offset = Long.parseLong(name, idRadix);
			} catch(NumberFormatException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to parse the item id \"{}\"", rawId);
			}
			try {
				item = itemConstructor.newInstance(
					name, offset, size, 0, reqConf.getContentSource()
				);
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IllegalStateException(e);
			}
		}
		//
		return item;
	}
	//
	@Override
	public final void close()
	throws IOException {
		reqConf.close();
	}
}
