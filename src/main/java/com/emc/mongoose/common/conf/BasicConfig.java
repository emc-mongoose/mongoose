package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
//
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.tree.DefaultExpressionEngine;
//
import org.apache.commons.lang.text.StrBuilder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
/**
 Created by kurila on 20.01.16.
 */
public class BasicConfig
extends HierarchicalConfiguration
implements AppConfig {
	//
	static {
		setDefaultExpressionEngine(new DefaultExpressionEngine());
	}
	//
	@Override
	public String getAuthId() {
		return getString(CONFIG_ROOT + KEY_AUTH_ID);
	}
	//
	@Override
	public String getAuthSecret() {
		return getString(CONFIG_ROOT + KEY_AUTH_SECRET);
	}
	//
	@Override
	public String getAuthToken() {
		return getString(CONFIG_ROOT + KEY_AUTH_TOKEN);
	}
	//
	@Override
	public int getIoBufferSizeMin() {
		return (int) SizeUtil.toSize(getString(CONFIG_ROOT + KEY_IO_BUFFER_SIZE_MIN));
	}
	//
	@Override
	public int getIoBufferSizeMax() {
		return (int) SizeUtil.toSize(getString(CONFIG_ROOT + KEY_IO_BUFFER_SIZE_MAX));
	}
	//
	@Override
	public ItemImpl getItemClass() {
		return ItemImpl.valueOf(getString(CONFIG_ROOT + KEY_ITEM_CLASS).toUpperCase());
	}
	//
	@Override
	public String getItemContainerName() {
		return getString(CONFIG_ROOT + KEY_ITEM_CONTAINER_NAME);
	}
	//
	@Override
	public ContentSourceImpl getItemDataContentClass() {
		return ContentSourceImpl
			.valueOf(getString(CONFIG_ROOT + KEY_ITEM_DATA_CONTENT_CLASS).toUpperCase());
	}
	//
	@Override
	public String getItemDataContentFile() {
		return getString(CONFIG_ROOT + KEY_ITEM_DATA_CONTENT_FILE);
	}
	//
	@Override
	public String getItemDataContentSeed() {
		return getString(CONFIG_ROOT + KEY_ITEM_DATA_CONTENT_SEED);
	}
	//
	@Override
	public int getItemDataContentSize() {
		return getInt(CONFIG_ROOT + KEY_ITEM_DATA_CONTENT_SIZE);
	}
	//
	@Override
	public DataRangesScheme getItemDataRangesClass() {
		return DataRangesScheme
			.valueOf(getString(CONFIG_ROOT + KEY_ITEM_DATA_RANGES_CLASS).toUpperCase());
	}
	//
	@Override
	public String getItemDataRangesFixedBytes() {
		return getString(CONFIG_ROOT + KEY_ITEM_DATA_RANGES_FIXED_BYTES);
	}
	//
	@Override
	public int getItemDataContentRangesRandomCount() {
		return getInt(CONFIG_ROOT + KEY_ITEM_DATA_RANGES_RANDOM_COUNT);
	}
	//
	@Override
	public DataSizeScheme getItemDataSizeClass() {
		return DataSizeScheme
			.valueOf(getString(CONFIG_ROOT + KEY_ITEM_DATA_SIZE_CLASS).toUpperCase());
	}
	//
	@Override
	public long getItemDataSizeFixed() {
		return SizeUtil.toSize(getString(CONFIG_ROOT + KEY_ITEM_DATA_SIZE_FIXED));
	}
	//
	@Override
	public long getItemDataSizeRandomMin() {
		return SizeUtil.toSize(getString(CONFIG_ROOT + KEY_ITEM_DATA_SIZE_RANDOM_MIN));
	}
	//
	@Override
	public long getItemDataSizeRandomMax() {
		return SizeUtil.toSize(getString(CONFIG_ROOT + KEY_ITEM_DATA_SIZE_RANDOM_MAX));
	}
	//
	@Override
	public double getItemDataSizeRandomBias() {
		return getDouble(CONFIG_ROOT + KEY_ITEM_DATA_SIZE_RANDOM_BIAS);
	}
	//
	@Override
	public boolean getItemDataVerify() {
		return getBoolean(CONFIG_ROOT + KEY_ITEM_DATA_VERIFY);
	}
	//
	@Override
	public String getItemInputFile() {
		return getString(CONFIG_ROOT + KEY_ITEM_INPUT_FILE);
	}
	//
	@Override
	public int getItemInputBatchSize() {
		return getInt(CONFIG_ROOT + KEY_ITEM_INPUT_BATCH_SIZE);
	}
	//
	@Override
	public ItemNamingScheme getItemNaming() {
		return new BasicItemNamingScheme(
			ItemNamingScheme.Type.valueOf(
				getString(CONFIG_ROOT + KEY_ITEM_NAMING).toUpperCase()
			)
		);
	}
	//
	@Override
	public int getItemQueueSizeLimit() {
		return getInt(CONFIG_ROOT + KEY_ITEM_QUEUE_SIZE_LIMIT);
	}
	//
	@Override
	public boolean getLoadCircular() {
		return getBoolean(CONFIG_ROOT + KEY_LOAD_CIRCULAR);
	}
	//
	@Override
	public LoadType getLoadClass() {
		return LoadType.valueOf(getString(CONFIG_ROOT + KEY_LOAD_CLASS).toUpperCase());
	}
	//
	@Override
	public int getLoadThreads() {
		return getInt(CONFIG_ROOT + KEY_LOAD_THREADS);
	}
	//
	@Override
	public long getLoadLimitCount() {
		return getLong(CONFIG_ROOT + KEY_LOAD_LIMIT_COUNT);
	}
	//
	@Override
	public double getLoadLimitRate() {
		return getDouble(CONFIG_ROOT + KEY_LOAD_LIMIT_RATE);
	}
	//
	@Override
	public long getLoadLimitTime() {
		final String rawValue = getString(CONFIG_ROOT + KEY_LOAD_LIMIT_TIME);
		return TimeUtil.getTimeUnit(rawValue).toSeconds(TimeUtil.getTimeValue(rawValue));
	}
	//
	@Override
	public int getLoadMetricsPeriod() {
		final String rawValue = getString(CONFIG_ROOT + KEY_LOAD_METRICS_PERIOD);
		return (int) TimeUtil.getTimeUnit(rawValue).toSeconds(TimeUtil.getTimeValue(rawValue));
	}
	//
	@Override
	public String[] getLoadServerAddrs() {
		return getStringArray(CONFIG_ROOT + KEY_LOAD_SERVER_ADDRS);
	}
	//
	@Override
	public boolean getLoadServerAssignToNode() {
		return getBoolean(CONFIG_ROOT + KEY_LOAD_SERVER_ASSIGN_TO_NODE);
	}
	//
	@Override
	public boolean getNetworkServeJmx() {
		return getBoolean(CONFIG_ROOT + KEY_NETWORK_SERVE_JMX);
	}
	//
	@Override
	public int getNetworkSocketTimeoutMilliSec() {
		return getInt(CONFIG_ROOT + KEY_NETWORK_SOCKET_TIMEOUT_MILLISEC);
	}
	//
	@Override
	public boolean getNetworkSocketReuseAddr() {
		return getBoolean(CONFIG_ROOT + KEY_NETWORK_SOCKET_REUSE_ADDR);
	}
	//
	@Override
	public boolean getNetworkSocketKeepAlive() {
		return getBoolean(CONFIG_ROOT + KEY_NETWORK_SOCKET_KEEP_ALIVE);
	}
	//
	@Override
	public boolean getNetworkSocketTcpNoDelay() {
		return getBoolean(CONFIG_ROOT + KEY_NETWORK_SOCKET_TCP_NO_DELAY);
	}
	//
	@Override
	public int getNetworkSocketLinger() {
		return getInt(CONFIG_ROOT + KEY_NETWORK_SOCKET_LINGER);
	}
	//
	@Override
	public int getNetworkSocketBindBacklogSize() {
		return getInt(CONFIG_ROOT + KEY_NETWORK_SOCKET_BIND_BACKLOG_SIZe);
	}
	//
	@Override
	public boolean getNetworkSocketInterestOpQueued() {
		return getBoolean(CONFIG_ROOT + KEY_NETWORK_SOCKET_INTEREST_OP_QUEUED);
	}
	//
	@Override
	public int getNetworkSocketSelectInterval() {
		return getInt(CONFIG_ROOT + KEY_NETWORK_SOCKET_SELECT_INTERVAL);
	}
	//
	@Override
	public String getRunId() {
		return getString(CONFIG_ROOT + KEY_RUN_ID);
	}
	//
	@Override
	public String getRunMode() {
		return getString(CONFIG_ROOT + KEY_RUN_MODE);
	}
	//
	@Override
	public String getRunName() {
		return getString(CONFIG_ROOT + KEY_RUN_NAME);
	}
	//
	@Override
	public boolean getRunResumeEnabled() {
		return getBoolean(CONFIG_ROOT + KEY_RUN_RESUME_ENABLED);
	}
	//
	@Override
	public String getRunVersion() {
		return getString(CONFIG_ROOT + KEY_RUN_VERSION);
	}
	//
	@Override
	public StorageType getStorageClass() {
		return StorageType.valueOf(getString(CONFIG_ROOT + KEY_STORAGE_CLASS));
	}
	//
	@Override
	public String[] getStorageHttpAddrs() {
		return getStringArray(CONFIG_ROOT + KEY_STORAGE_HTTP_ADDRS);
	}
	//
	@Override
	public String getStorageHttpApiClass() {
		return getString(CONFIG_ROOT + KEY_STORAGE_HTTP_API_CLASS);
	}
	//
	@Override
	public int getStorageHttpApiS3Port() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_API_S3_PORT);
	}
	//
	@Override
	public int getStorageHttpApiAtmosPort() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_API_ATMOS_PORT);
	}
	//
	@Override
	public int getStorageHttpApiSwiftPort() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_API_SWIFT_PORT);
	}
	//
	@Override
	public boolean getStroageHttpFsAccess() {
		return getBoolean(CONFIG_ROOT + KEY_STORAGE_HTTP_FS_ACCESS);
	}
	//
	@Override
	public Configuration getStorageHttpHeaders() {
		return subset(CONFIG_ROOT + KEY_STORAGE_HTTP_HEADERS);
	}
	//
	@Override
	public String getStorageHttpNamespace() {
		return getString(CONFIG_ROOT + KEY_STORAGE_HTTP_NAMESPACE);
	}
	//
	@Override
	public boolean getStorageHttpVersioning() {
		return getBoolean(CONFIG_ROOT + KEY_STORAGE_HTTP_VERSIONING);
	}
	//
	@Override
	public int getStorageHttpMockHeadCount() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_MOCK_HEAD_COUNT);
	}
	//
	@Override
	public int getStorageHttpMockWorkersPerSocket() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_MOCK_WORKERS_PER_SOCKET);
	}
	//
	@Override
	public int getStorageHttpMockCapacity() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_MOCK_CAPACITY);
	}
	//
	@Override
	public int getStorageHttpMockContainerCapacity() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_MOCK_CONTAINER_CAPACITY);
	}
	//
	@Override
	public int getStorageHttpMockContainerCountLimit() {
		return getInt(CONFIG_ROOT + KEY_STORAGE_HTTP_MOCK_CONTAINER_COUNT_LIMIT);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Serialization and formatting section
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static String
		TABLE_BORDER = "\n+--------------------------------+----------------------------------------------------------------+",
		TABLE_HEADER = "Configuration parameters:";
	@Override
	public String toString() {
		String nextKey;
		Object nextVal;
		final StrBuilder strBuilder = new StrBuilder()
			.append(TABLE_HEADER).append(TABLE_BORDER)
			.appendNewLine().append("| ").appendFixedWidthPadRight("Key", 31, ' ')
			.append("| ").appendFixedWidthPadRight("Value", 63, ' ').append('|')
			.append(TABLE_BORDER);
		for(
			final Iterator<String> keyIterator = getKeys();
			keyIterator.hasNext();
		) {
			nextKey = keyIterator.next();
			nextVal = getProperty(nextKey);
			switch(nextKey) {
				case KEY_ITEM_CLASS:
				case KEY_ITEM_CONTAINER_NAME:
				case KEY_LOAD_CLASS:
				case KEY_LOAD_THREADS:
				case KEY_LOAD_LIMIT_COUNT:
				case KEY_LOAD_LIMIT_TIME:
				case KEY_RUN_ID:
				case KEY_RUN_MODE:
				case KEY_RUN_VERSION:
				case KEY_STORAGE_CLASS:
					strBuilder
						.appendNewLine().append("| ")
						.appendFixedWidthPadRight(nextKey, 31, ' ')
						.append("| ")
						.appendFixedWidthPadRight(nextVal, 63, ' ')
						.append('|');
					break;
			}
		}
		return strBuilder.append(TABLE_BORDER).toString();
	}
	//
	@Override
	public String toFormattedString() {
		final Logger log = LogManager.getLogger();
		//
		final ObjectMapper
			mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
		//
		final ObjectNode
			rootNode = mapper.createObjectNode(),
			configNode = mapper.createObjectNode();
		rootNode.set(CONFIG_ROOT, configNode);
		for(super.getKeys())
		try {
			return mapper.writeValueAsString(rootNode);
		} catch(final JsonProcessingException e) {
			LogUtil.exception(log, Level.WARN, e, "Failed to convert the configuration to JSON");
		}
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		final byte jsonData[] = toFormattedString().getBytes();
		out.writeInt(jsonData.length);
		out.write(jsonData);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final int l = in.readInt();
		final byte jsonData[] = new byte[l];
		in.readFully(jsonData);
		new JsonConfigLoader(this).loadPropsFromJsonByteArray(jsonData);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Load from the external sources
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void load() {
		String dirRoot = System.getProperty("user.dir");
		try {
			dirRoot = new File(
				Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()
			).getParent();
		} catch(final URISyntaxException e) {
			synchronized(System.err) {
				System.err.println("Failed to determine the executable path:");
				e.printStackTrace(System.err);
			}
		}
		loadFromJson(
			Paths.get(dirRoot, Constants.DIR_CONF).resolve(FNAME_CONF)
		);
		loadFromEnv();
	}
	//
	public void loadFromJson(final Path filePath) {
		final Logger log = LogManager.getLogger();
		final String prefixKeyAliasingWithDot = PREFIX_KEY_ALIASING + ".";
		new JsonConfigLoader(this).loadPropsFromJsonCfgFile(filePath);
		log.debug(Markers.MSG, "Going to override the aliasing section");
		String key, correctKey;
		for(final Iterator<String> keyIter = getKeys(PREFIX_KEY_ALIASING); keyIter.hasNext();) {
			key = keyIter.next();
			correctKey = key.replaceAll(prefixKeyAliasingWithDot, "");
			log.trace(
				Markers.MSG, "Alias: \"{}\" -> \"{}\"", correctKey, getStringArray(key)
			);
		}
	}
	//
	public void loadFromEnv() {
		final Logger log = LogManager.getLogger();
		final SystemConfiguration sysProps = new SystemConfiguration();
		String key;
		Object sharedValue;
		for(final Iterator<String> keyIter = sysProps.getKeys(); keyIter.hasNext();) {
			key = keyIter.next();
			log.trace(
				Markers.MSG, "System property: \"{}\": \"{}\" -> \"{}\"",
				key, getProperty(key), sysProps.getProperty(key)
			);
			sharedValue = sysProps.getProperty(key);
			setProperty(key, sharedValue);
		}
	}
	//
}
