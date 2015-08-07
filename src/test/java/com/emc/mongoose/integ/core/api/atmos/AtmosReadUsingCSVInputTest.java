package com.emc.mongoose.integ.core.api.atmos;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.model.DataItemOutput;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.model.CSVFileItemOutput;
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosReadUsingCSVInputTest {
	//
	private final static int COUNT_TO_WRITE = 1000;
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static <T extends BasicWSObject> void setUpClass()
		throws Exception {
		//
		try(
			final StorageClient<T> client = new BasicWSClientBuilder<T, StorageClient<T>>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.build()
		) {
			final DataItemOutput<T> writeOutput = new CSVFileItemOutput<>(
				(Class<T>) BasicWSObject.class
			);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("10MB")
			);
			COUNT_READ = client.read(writeOutput.getInput());
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final SubTenant st = new WSSubTenantImpl(
			(WSRequestConfigImpl) WSLoadBuilderFactory.getInstance(rtConfig).getRequestConfig(),
			rtConfig.getString(RunTimeConfig.KEY_API_ATMOS_SUBTENANT)
		);
		st.delete(rtConfig.getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkWrittenItemsCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
