module com.emc.mongoose.storage.driver.coop {

	requires com.emc.mongoose.storage.driver.base;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires log4j.api;
	requires java.rmi;

	exports com.emc.mongoose.storage.driver.coop;
}