module com.emc.mongoose.api.metrics {

	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires log4j.api;
	requires log4j.core;
	requires java.base;

	exports com.emc.mongoose.api.metrics;
	exports com.emc.mongoose.api.metrics.logging;
}
