package com.emc.mongoose.core.impl.v1.item.data;
//
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.v1.item.container.Container;
import com.emc.mongoose.core.api.v1.item.data.HttpDataItem;
import com.emc.mongoose.core.api.v1.item.data.ContainerHelper;
import com.emc.mongoose.core.api.v1.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.v1.item.container.BasicContainer;
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
public abstract class HttpContainerHelperBase<T extends HttpDataItem, C extends Container<T>>
implements ContainerHelper<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String CONTAINER_PREFIX = BasicConfig.THREAD_CONTEXT.get().getRunName();
	//
	protected final HttpRequestConfig<T, C> reqConf;
	protected final String containerName;
	protected final String path;
	protected final String idPrefix;
	protected final int pathLen;
	protected final int idPrefixLen;
	protected final int idRadix;
	protected final boolean verifyContent;
	//
	@SuppressWarnings("unchecked")
	protected HttpContainerHelperBase(final HttpRequestConfig<T, C> reqConf, final C container) {
		this.reqConf = reqConf;
		String tmpName = container == null ? null : container.getName();
		if(tmpName == null || tmpName.length() == 0) {
			final Date dt = Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime();
			tmpName = CONTAINER_PREFIX + "-" + LogUtil.FMT_DT.format(dt);
			if(container == null) {
				reqConf.setContainer((C) new BasicContainer<T>(tmpName));
			} else {
				container.setName(tmpName);
			}
		}
		//
		final int firstSepPos = tmpName.indexOf('/');
		if(firstSepPos < 0) {
			containerName = tmpName;
			path = null;
			pathLen = 0;
		} else {
			containerName = tmpName.substring(0, firstSepPos);
			path = tmpName.substring(firstSepPos + 1);
			pathLen = path.length();
		}
		this.idPrefix = reqConf.getNamePrefix();
		this.idRadix = reqConf.getNameRadix();
		idPrefixLen = idPrefix == null ? 0 : idPrefix.length();
		this.verifyContent = reqConf.getVerifyContentFlag();
	}
	//
	public final String toString() {
		return containerName;
	}
	//
	@Override
	public final T buildItem(
		final Constructor<T> itemConstructor, final String rawId, final long size
	) throws IllegalStateException {
		//
		T item = null;
		//
		String id = null;
		if(rawId != null && !rawId.isEmpty()) {
			if(path != null) { // include the items which have the path matching to configured one
				if(rawId.startsWith(path) && rawId.length() > pathLen) {
					id = rawId.substring(pathLen + 1);
					if(id.contains("/")) { // doesn't include the items from the subdirectories
						id = null;
					}
				} else {
					if(!rawId.contains("/")) { // doesn't include the items from another directories
						id = rawId;
					}
				}
			} else {
				id = rawId;
			}
			// exclude the prefix from the id
			if(idPrefix != null && rawId.startsWith(idPrefix) && rawId.length() > idPrefixLen) {
				id = rawId.substring(idPrefixLen);
			}
		}
		//
		if(id != null) {
			long offset = 0;
			try {
				offset = Long.parseLong(id, idRadix);
			} catch(NumberFormatException e) {
				LogUtil.exception(LOG, Level.WARN, e, "Failed to parse the item id \"{}\"", rawId);
			}
			try {
				item = itemConstructor.newInstance(
					id, offset, size, 0, reqConf.getContentSource()
				);
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IllegalStateException(e);
			}
		} else {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Item with name \"{}\" was excluded", rawId);
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
