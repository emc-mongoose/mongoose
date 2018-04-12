module com.emc.mongoose.storage.driver.net {

	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.storage.driver.cooperative;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires com.github.akurilov.netty.connection.pool;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.storage.driver.net;
	exports com.emc.mongoose.storage.driver.net.data;
}