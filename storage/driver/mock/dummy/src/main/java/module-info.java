module com.emc.mongoose.storage.driver.mock.dummy {

	requires com.emc.mongoose.storage.driver.base;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.storage.driver.mock.dummy;

	provides com.emc.mongoose.storage.driver.base.StorageDriverFactory
		with com.emc.mongoose.storage.driver.mock.dummy.DummyStorageDriverMockFactory;
}