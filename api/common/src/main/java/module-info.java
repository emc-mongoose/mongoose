module com.emc.mongoose.api.common {

	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires commons.lang;
	requires java.base;
	requires java.logging;
	requires java.rmi;

	exports com.emc.mongoose.api.common;
	exports com.emc.mongoose.api.common.env;
	exports com.emc.mongoose.api.common.exception;
	exports com.emc.mongoose.api.common.supply;
	exports com.emc.mongoose.api.common.supply.async;
}
