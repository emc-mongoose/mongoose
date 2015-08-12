package com.emc.mongoose.integ.base;
import com.emc.mongoose.core.api.load.builder.WSLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicWSLoadBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class StandaloneLoadBuilderTestBase
extends WSMockTestBase {
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		WSMockTestBase.setUpClass();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMockTestBase.setUpClass();
	}
	//
	protected WSLoadBuilder loadBuilder;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		loadBuilder = new BasicWSLoadBuilder<>(RT_CONFIG);
	}
	//
	@After
	public void tearDown()
	throws Exception {
		loadBuilder.close();
		super.tearDown();
	}
}
