package com.emc.mongoose.storage.driver.builder;

import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.model.data.DataInput;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.storage.StorageDriver;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;
import static com.emc.mongoose.api.common.env.PathUtil.getBaseDir;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.output.metrics.average.AverageConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.Level;

import static org.apache.logging.log4j.CloseableThreadContext.Instance;

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
	
	private String stepName;
	private DataInput contentSrc;
	private ItemConfig itemConfig;
	private LoadConfig loadConfig;
	private AverageConfig avgMetricsConfig;
	private StorageConfig storageConfig;

	protected final String getStepId() {
		return stepName;
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
	public AverageConfig getAverageConfig() {
		return avgMetricsConfig;
	}

	@Override
	public StorageConfig getStorageConfig() {
		return storageConfig;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setTestStepName(final String jobName) {
		this.stepName = jobName;
		return this;
	}
	
	@Override
	public BasicStorageDriverBuilder<I, O, T> setContentSource(final DataInput contentSrc) {
		this.contentSrc = contentSrc;
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
	public BasicStorageDriverBuilder<I, O, T> setAverageConfig(
		final AverageConfig avgMetricsConfig
	) {
		this.avgMetricsConfig = avgMetricsConfig;
		return this;
	}

	@Override
	public BasicStorageDriverBuilder<I, O, T> setStorageConfig(final StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public T build()
	throws UserShootHisFootException, InterruptedException {

		try(
			final Instance ctx = CloseableThreadContext
				.put(KEY_TEST_STEP_ID, stepName)
				.put(KEY_CLASS_NAME, BasicStorageDriverBuilder.class.getSimpleName())
		) {

			final DriverConfig driverConfig = storageConfig.getDriverConfig();
			final List<Map<String, Object>> implConfig = driverConfig.getImpl();
			final boolean verifyFlag = itemConfig.getDataConfig().getVerify();
			final Map<String, URL> implClsPathUrls = new HashMap<>();
			final Map<String, String> implFqcnsByType = new HashMap<>();

			for(final Map<String, Object> nextImplInfo : implConfig) {

				final String implFile = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_FILE);
				try {
					implClsPathUrls.put(
						implFile, new File(getBaseDir() + File.separator + implFile).toURI().toURL()
					);
				} catch(final MalformedURLException e) {
					Loggers.ERR.warn("Invalid storage driver implementation file: {}", implFile);
				}

				implFqcnsByType.put(
					(String) nextImplInfo.get(DriverConfig.KEY_IMPL_TYPE),
					(String) nextImplInfo.get(DriverConfig.KEY_IMPL_FQCN)
				);
			}

			final String driverType = driverConfig.getType();
			T driver = null;
			final URL[] clsPathUrls = new URL[implClsPathUrls.size()];
			implClsPathUrls.values().toArray(clsPathUrls);

			try {
				final URLClassLoader clsLoader = new URLClassLoader(clsPathUrls);
				final Class<T> matchingImplCls = (Class<T>) Class.forName(
					implFqcnsByType.get(driverType), true, clsLoader
				);
				if(matchingImplCls == null) {
					throw new UserShootHisFootException(
						"No matching implementation class for the storage driver type \"" +
							driverType + "\""
					);
				}
				final Constructor<T> constructor = matchingImplCls.<T>getConstructor(
					String.class, DataInput.class, LoadConfig.class, StorageConfig.class,
					Boolean.TYPE
				);
				Loggers.MSG.info("New storage driver for type \"{}\"", driverType);
				driver = constructor.newInstance(
					stepName, contentSrc, loadConfig, storageConfig, verifyFlag
				);
			} catch(final ClassNotFoundException | NoClassDefFoundError e) {
				throw new UserShootHisFootException(
					"Failed to load storage driver implementation for type: " + driverType
				);
			} catch(final NoSuchMethodException e) {
				throw new UserShootHisFootException(
					"No valid constructor to make the \"" + driverType +
						"\" storage driver instance"
				);
			} catch(final InvocationTargetException e) {
				final Throwable cause = e.getCause();
				if(cause instanceof InterruptedException) {
					throw (InterruptedException) cause;
				} else {
					throw new UserShootHisFootException(e);
				}
			} catch(final InstantiationException | IllegalAccessException e) {
				throw new UserShootHisFootException(e);
			}

			return driver;
		}
	}
}
