package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemNamingScheme;
import com.emc.mongoose.core.api.item.data.ContentSource;
import org.apache.commons.configuration.BaseConfiguration;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
/**
 Created by kurila on 20.01.16.
 */
public class BasicConfig
extends BaseConfiguration
implements AppConfig {
	//
	@Override
	public String getName() {
		return getString("config.name");
	}
	//
	@Override
	public String getVersion() {
		return getString("config.version");
	}
	//
	@Override
	public String getMode() {
		return null;
	}
	//
	@Override
	public String getAuthId() {
		return null;
	}
	//
	@Override
	public String getAuthSecret() {
		return null;
	}
	//
	@Override
	public String getAuthToken() {
		return null;
	}
	//
	@Override
	public int getIOBufferSizeMin() {
		return 0;
	}
	//
	@Override
	public int getIoBufferSizeMax() {
		return 0;
	}
	//
	@Override
	public Class<? extends Item> getItemClass() {
		return null;
	}
	//
	@Override
	public String getItemContainerName() {
		return null;
	}
	//
	@Override
	public Class<? extends ContentSource> getItemDataContentClass() {
		return null;
	}
	//
	@Override
	public String getItemDataContentFile() {
		return null;
	}
	//
	@Override
	public long getItemDataContentRingSeed() {
		return 0;
	}
	//
	@Override
	public int getItemDataContentRingSize() {
		return 0;
	}
	//
	@Override
	public DataRangesScheme getItemDataRangesClass() {
		return null;
	}
	//
	@Override
	public String getItemDataRangesFixedBytes() {
		return null;
	}
	//
	@Override
	public int getItemDataContentRangesRandomCount() {
		return 0;
	}
	//
	@Override
	public DataSizeScheme getItemDataSizeClass() {
		return null;
	}
	//
	@Override
	public long getItemDataSizeFixed() {
		return 0;
	}
	//
	@Override
	public long getItemDataSizeRandomMin() {
		return 0;
	}
	//
	@Override
	public long getItemDataSizeRandomMax() {
		return 0;
	}
	//
	@Override
	public double getItemDataSizeRandomBias() {
		return 0;
	}
	//
	@Override
	public boolean getItemDataVerify() {
		return false;
	}
	//
	@Override
	public String getItemInputFile() {
		return null;
	}
	//
	@Override
	public int getItemInputBatchSize() {
		return 0;
	}
	//
	@Override
	public ItemNamingScheme getItemNamingClass() {
		return null;
	}
	//
	@Override
	public int getItemQueueSizeLimit() {
		return 0;
	}
	//
	@Override
	public boolean getLoadCircular() {
		return false;
	}
	//
	@Override
	public IOTask.Type getLoadClass() {
		return null;
	}
	//
	@Override
	public int getLoadThreads() {
		return 0;
	}
	//
	@Override
	public long getLoadLimitCount() {
		return 0;
	}
	//
	@Override
	public double getLoadLimitRate() {
		return 0;
	}
	//
	@Override
	public long getLoadLimitTime() {
		return 0;
	}
	//
	@Override
	public int getLoadMetricsPeriod() {
		return 0;
	}
	//
	@Override
	public String[] getLoadServerAddrs() {
		return new String[0];
	}
	//
	@Override
	public boolean getLoadServerAssignToNode() {
		return false;
	}
	//
	@Override
	public String getStorageClass() {
		return null;
	}
	//
	@Override
	public String[] getStorageHttpAddrs() {
		return new String[0];
	}
	//
	@Override
	public String getStorageHttpApiClass() {
		return null;
	}
	//
	@Override
	public int getStorageHttpApiS3Port() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpApiAtmosPort() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpApiSwiftPort() {
		return 0;
	}
	//
	@Override
	public boolean getStroageHttpFsAccess() {
		return false;
	}
	//
	@Override
	public Map<String, String> getStorageHttpHeaders() {
		return null;
	}
	//
	@Override
	public String getStorageHttpNamespace() {
		return null;
	}
	//
	@Override
	public boolean getStorageHttpVersioning() {
		return false;
	}
	//
	@Override
	public int getStorageHttpMockHeadCount() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpMockWorkersPerSocket() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpMockCapacity() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpMockContainerCapacity() {
		return 0;
	}
	//
	@Override
	public int getStorageHttpMockContainerCountLimit() {
		return 0;
	}
	//
	@Override
	public boolean getNetworkServeJmx() {
		return false;
	}
	//
	@Override
	public int getNetworkSocketTimeoutMilliSec() {
		return 0;
	}
	//
	@Override
	public boolean getNetworkSocketReuseAddr() {
		return false;
	}
	//
	@Override
	public boolean getNetworkSocketKeepAlive() {
		return false;
	}
	//
	@Override
	public boolean getNetworkSocketTcpNoDelay() {
		return false;
	}
	//
	@Override
	public int getNetworkSocketLinger() {
		return 0;
	}
	//
	@Override
	public int getNetworkSocketBindBacklogSize() {
		return 0;
	}
	//
	@Override
	public boolean getNetworkSocketInterestOpQueued() {
		return false;
	}
	//
	@Override
	public int getNetworkSocketSelectInterval() {
		return 0;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
	}
	//
	@Override
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
	}
}
