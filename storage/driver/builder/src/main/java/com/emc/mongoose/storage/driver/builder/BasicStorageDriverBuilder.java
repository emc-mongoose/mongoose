package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.ContentSourceUtil;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.MetricsConfig;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private MetricsConfig metricsConfig;
	private StorageConfig storageConfig;
	
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
	public MetricsConfig getMetricsConfig() {
		return metricsConfig;
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
	public BasicStorageDriverBuilder<I, O, T> setMetricsConfig(final MetricsConfig metricsConfig) {
		this.metricsConfig = metricsConfig;
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

		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		final List<Map<String, Object>> implConfig = driverConfig.getImplConfig();
		final boolean verifyFlag = itemConfig.getDataConfig().getVerify();
		final Map<String, Class<T>> availableImpls = new HashMap<>();

		for(final Map<String, Object> nextImplInfo : implConfig) {

			final String implType = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_TYPE);
			final String implFile = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_FILE);
			final String implFqcn = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_FQCN);

			try {
				final URL implUrl = new File(PathUtil.getBaseDir() + File.separatorChar + implFile)
					.toURI().toURL();
				final URLClassLoader clsLoader = new URLClassLoader(new URL[] { implUrl });
				final Class<T> implCls = (Class<T>) Class.forName(implFqcn, true, clsLoader);
				LOG.info(
					Markers.MSG,
					"Loaded storage driver implementation \"{}\" from the class \"{}\"",
					implType, implFqcn
				);
				availableImpls.put(implType, implCls);
			} catch(final MalformedURLException e) {
				LOG.warn(Markers.ERR, "Invalid storage driver implementation file: {}", implFile);
			} catch(final ClassNotFoundException e) {
				LOG.warn(
					Markers.ERR, "Invalid FQCN \"{}\" for the implementation from file: {}",
					implFqcn, implFile
				);
			}
		}

		final String driverType = driverConfig.getType();
		final Class<T> matchingImplCls = availableImpls.get(driverType);

		try {
			final Constructor<T> constructor = matchingImplCls.<T>getConstructor(
				String.class, LoadConfig.class, StorageConfig.class, Boolean.TYPE
			);
			return constructor.newInstance(jobName, loadConfig, storageConfig, verifyFlag);
		} catch(final NoSuchMethodException e) {
			throw new UserShootHisFootException(
				"No valid constructor to make the \"" + driverType +
					"\" storage driver instance"
			);
		} catch(
			final InstantiationException | IllegalAccessException | InvocationTargetException e
		) {
			throw new UserShootHisFootException(e.getCause());
		}
	}
}
