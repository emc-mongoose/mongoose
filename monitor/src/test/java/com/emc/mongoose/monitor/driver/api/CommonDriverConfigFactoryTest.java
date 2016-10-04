package com.emc.mongoose.monitor.driver.api;

import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.impl.io.task.BasicDataIoTask;
import com.emc.mongoose.model.impl.item.BasicMutableDataItem;
import com.emc.mongoose.monitor.driver.impl.HttpDriverFactory;
import com.emc.mongoose.monitor.driver.impl.TempDriverConfigFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 Created on 25.09.16.
 */
public class CommonDriverConfigFactoryTest {

	private CommonDriverConfigFactory driverConfigFactory;

	@Before
	public void setUp() {
		driverConfigFactory = new TempDriverConfigFactory();
	}

	@Test
	public void shouldCreateDriver() throws Exception {
		final HttpDriverFactory<BasicMutableDataItem, BasicDataIoTask<BasicMutableDataItem>>
			httpDriverFactory = new HttpDriverFactory<>(driverConfigFactory);
		final Driver<BasicMutableDataItem, BasicDataIoTask<BasicMutableDataItem>>
			driver = httpDriverFactory.create(HttpDriverFactory.Api.S3);
	}
}