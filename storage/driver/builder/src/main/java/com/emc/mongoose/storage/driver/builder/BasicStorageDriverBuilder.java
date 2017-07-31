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
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.File;
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
			final Map<String, Class<T>> availableImpls = new HashMap<>();

			for(final Map<String, Object> nextImplInfo : implConfig) {

				final String implType = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_TYPE);
				final String implFile = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_FILE);
				final String implFqcn = (String) nextImplInfo.get(DriverConfig.KEY_IMPL_FQCN);

				try {
					final URL implUrl = new File(getBaseDir() + File.separator + implFile)
						.toURI().toURL();
					final URLClassLoader clsLoader = new URLClassLoader(new URL[] { implUrl });
					final Class<T> implCls = (Class<T>) Class.forName(implFqcn, true, clsLoader);
					Loggers.MSG.debug(
						"Loaded storage driver implementation \"{}\" from the class \"{}\"",
						implType, implFqcn
					);
					availableImpls.put(implType, implCls);
				} catch(final MalformedURLException e) {
					Loggers.ERR.warn("Invalid storage driver implementation file: {}", implFile);
				} catch(final ClassNotFoundException | NoClassDefFoundError e) {
					Loggers.ERR.warn(
						"Invalid FQCN \"{}\" for the implementation from file: {}", implFqcn,
						implFile
					);
				}
			}

			final String driverType = driverConfig.getType();
			final Class<T> matchingImplCls = availableImpls.get(driverType);

			if(matchingImplCls == null) {
				throw new UserShootHisFootException(
					"No matching implementation class for the storage driver type \"" +
						driverType + "\""
				);
			}

			try {
				final Constructor<T> constructor = matchingImplCls.<T>getConstructor(
					String.class, DataInput.class, LoadConfig.class, StorageConfig.class,
					Boolean.TYPE
				);
				Loggers.MSG.info("New storage driver for type \"{}\"", driverType);
				return constructor.newInstance(
					stepName, contentSrc, loadConfig, storageConfig, verifyFlag
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
		}
	}
}
