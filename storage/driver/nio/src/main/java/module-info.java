module com.emc.mongoose.storage.driver.nio {

	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.storage.driver.cooperative;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires log4j.api;
	requires java.base;
	requires java.rmi;

	exports com.emc.mongoose.storage.driver.nio;
}