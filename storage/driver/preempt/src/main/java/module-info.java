module com.emc.mongoose.storage.driver.preempt {

	requires com.github.akurilov.commons;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.emc.mongoose.storage.driver.base;
	requires log4j.api;

	exports com.emc.mongoose.storage.driver.preempt;
}