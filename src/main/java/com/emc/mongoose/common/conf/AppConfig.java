package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemNamingScheme;
import com.emc.mongoose.core.api.item.data.ContentSource;
//
import org.apache.commons.configuration.Configuration;
//
import java.io.Externalizable;
import java.util.Map;
/**
 Created by kurila on 20.01.16.
 */
public interface AppConfig
extends Configuration, Externalizable {

	String KEY_CONFIG_NAME = "config.name";
	String KEY_CONFIG_VERSION = "config.version";
	String KEY_CONFIG_MODE = "config.mode";
	String KEY_CONFIG_AUTH_ID = "config.auth.id";
	String KEY_CONFIG_AUTH_SECRET = "config.auth.secret";
	String KEY_CONFIG_AUTH_TOKEN = "config.auth.token";
	String KEY_CONFIG_IO_BUFFER_SIZE_MIN = "config.io.buffer.size.min";
	String KEY_CONFIG_IO_BUFFER_SIZE_MAX = "config.io.buffer.size.max";
	String KEY_CONFIG_ITEM_CLASS = "config.item.class";
	String KEY_CONFIG_ITEM_CONTAINER_NAME = "config.item.container.name";
	String KEY_CONFIG_ITEM_DATA_CONTENT_CLASS = "config.item.data.content.class";
	String KEY_CONFIG_ITEM_DATA_CONTENT_FILE = "config.item.data.content.file";
	String KEY_CONFIG_ITEM_DATA_CONTENT_RING_SEED = "config.item.data.content.ring.seed";
	String KEY_CONFIG_ITEM_DATA_CONTENT_RING_SIZE = "config.item.data.content.ring.size";

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getName();

	String getVersion();

	String getMode();

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getAuthId();

	String getAuthSecret();

	String getAuthToken();

	////////////////////////////////////////////////////////////////////////////////////////////////

	int getIOBufferSizeMin();

	int getIoBufferSizeMax();

	////////////////////////////////////////////////////////////////////////////////////////////////

	Class<? extends Item> getItemClass();

	String getItemContainerName();

	Class<? extends ContentSource> getItemDataContentClass();

	String getItemDataContentFile();

	long getItemDataContentRingSeed();

	int getItemDataContentRingSize();

	enum DataRangesScheme { FIXED, RANDOM }
	DataRangesScheme getItemDataRangesClass();

	String getItemDataRangesFixedBytes();

	int getItemDataContentRangesRandomCount();

	enum DataSizeScheme { FIXED, RANDOM }
	DataSizeScheme getItemDataSizeClass();

	long getItemDataSizeFixed();

	long getItemDataSizeRandomMin();

	long getItemDataSizeRandomMax();

	double getItemDataSizeRandomBias();

	boolean getItemDataVerify();

	String getItemInputFile();

	int getItemInputBatchSize();

	ItemNamingScheme getItemNamingClass();

	int getItemQueueSizeLimit();

	////////////////////////////////////////////////////////////////////////////////////////////////

	boolean getLoadCircular();

	enum LoadType { WRITE, READ, DELETE }
	LoadType getLoadClass();

	int getLoadThreads();

	long getLoadLimitCount();

	double getLoadLimitRate();

	/** Return the time limit converted into the seconds */
	long getLoadLimitTime();

	/** Return the period in seconds */
	int getLoadMetricsPeriod();

	String[] getLoadServerAddrs();

	boolean getLoadServerAssignToNode();

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getStorageClass();

	String[] getStorageHttpAddrs();

	String getStorageHttpApiClass();

	int getStorageHttpApiS3Port();

	int getStorageHttpApiAtmosPort();

	int getStorageHttpApiSwiftPort();

	boolean getStroageHttpFsAccess();

	Map<String, String> getStorageHttpHeaders();

	String getStorageHttpNamespace();

	boolean getStorageHttpVersioning();

	int getStorageHttpMockHeadCount();

	int getStorageHttpMockWorkersPerSocket();

	int getStorageHttpMockCapacity();

	int getStorageHttpMockContainerCapacity();

	int getStorageHttpMockContainerCountLimit();

	////////////////////////////////////////////////////////////////////////////////////////////////

	boolean getNetworkServeJmx();

	int getNetworkSocketTimeoutMilliSec();

	boolean getNetworkSocketReuseAddr();

	boolean getNetworkSocketKeepAlive();

	boolean getNetworkSocketTcpNoDelay();

	int getNetworkSocketLinger();

	int getNetworkSocketBindBacklogSize();

	boolean getNetworkSocketInterestOpQueued();

	int getNetworkSocketSelectInterval();
}
