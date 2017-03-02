package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;

/**
 Created by andrey on 05.10.16.
 */
public class BasicStorageDriverBuilder<
	I extends Item, O extends IoTask<I>, T extends StorageDriver<I, O>
> implements StorageDriverBuilder<I, O, T> {

	private static final Logger LOG = LogManager.getLogger();

	private String jobName;
	private ItemConfig itemConfig;
	private LoadConfig loadConfig;
	private StorageConfig storageConfig;
	
	private final List<Class<T>> storageDriverImpls = new ArrayList<>();
	
	public BasicStorageDriverBuilder() {
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			final Enumeration<URL> resUrls = cl
				.getResources(RES_PREFIX + StorageDriver.class.getCanonicalName());
			String nextLine;
			Class<T> nextImplCls;
			while(resUrls.hasMoreElements()) {
				try(
					final BufferedReader br = new BufferedReader(
						new InputStreamReader(resUrls.nextElement().openStream())
					)
				) {
					while(null != (nextLine = br.readLine())) {
						try {
							nextImplCls = (Class) Class.forName(nextLine, true, cl);
							LOG.info(
								Markers.MSG, "Loaded storage driver implementation: {}",
								nextImplCls.getCanonicalName()
							);
							storageDriverImpls.add(nextImplCls);
						} catch(final ClassNotFoundException e) {
							LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
						}
					}
				} catch(final IOException e) {
					LogUtil.exception(LOG, Level.WARN, e, "Unexpected failure");
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
		}

		if(storageDriverImpls.size() == 0) {
			LOG.warn(Markers.ERR, "No storage driver implementations loaded");
		}
	}

	protected final ContentSource getContentSource()
	throws IOException {
		final ContentConfig contentConfig = itemConfig.getDataConfig().getContentConfig();
		return ContentSourceUtil.getInstance(
			contentConfig.getFile(), contentConfig.getSeed(), contentConfig.getRingSize()
		);
	}

	@Override
	public ItemConfig getItemConfig() {
		return itemConfig;
	}
	
	@Override
	public LoadConfig getLoadConfig() {
		return loadConfig;
	}

	@Override
	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setJobName(final String jobName) {
		this.jobName = jobName;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public T build()
	throws UserShootHisFootException {
		
		final String storageDriverType = storageConfig.getDriverConfig().getType();
		final boolean verifyFlag = itemConfig.getDataConfig().getVerify();

		Matcher m;
		String implFqcn, sdt;
		Class<T> matchingImplCls = null;
		for(final Class<T> implCls : storageDriverImpls) {
			implFqcn = implCls.getCanonicalName();
			m = PATTERN_IMPL_FQCN.matcher(implFqcn);
			if(m.matches()) {
				sdt = m.group(STORAGE_DRIVER_TYPE);
				if(sdt != null) {
					if(sdt.startsWith(storageDriverType)) {
						matchingImplCls = implCls;
						break;
					}
				} else {
					throw new AssertionError(); // shouldn't pass "matches" call
				}
			}
		}

		if(matchingImplCls == null) {
			throw new UserShootHisFootException(
				"Didn't found the implementation class for \"" + storageDriverType +
					"\" storage driver type"
			);
		}

		try {
			final Constructor<T> constructor = matchingImplCls.<T>getConstructor(
				String.class, LoadConfig.class, StorageConfig.class, Boolean.TYPE
			);
			return constructor.newInstance(jobName, loadConfig, storageConfig, verifyFlag);
		} catch(final NoSuchMethodException e) {
			throw new UserShootHisFootException(
				"No valid constructor to make the \"" + storageDriverType +
					"\" storage driver instance"
			);
		} catch(
			final InstantiationException | IllegalAccessException | InvocationTargetException e
		) {
			throw new UserShootHisFootException(e.getCause());
		}
	}
}
