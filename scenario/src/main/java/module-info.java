module com.emc.mongoose.scenario {

	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.emc.mongoose.load.controller;
	requires com.emc.mongoose.load.generator;
	requires com.emc.mongoose.storage.driver;
	requires log4j.api;
	requires java.base;
	requires java.rmi;
	requires java.scripting;

	exports com.emc.mongoose.scenario;
}