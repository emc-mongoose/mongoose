module com.emc.mongoose.node {

	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.api.metrics;
	requires com.emc.mongoose.scenario;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires log4j.api;
	requires log4j.core;
	requires java.base;
	requires java.rmi;

	exports com.emc.mongoose.node;
}