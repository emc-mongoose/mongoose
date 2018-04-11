module com.emc.mongoose.api.common {

	requires java.base;
	requires java.commons;
	requires java.concurrent;

	exports com.emc.mongoose.api.common;
	exports com.emc.mongoose.api.common.env;
	exports com.emc.mongoose.api.common.exception;
	exports com.emc.mongoose.api.common.supply;
	exports com.emc.mongoose.api.common.supply.async;
}
