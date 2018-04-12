module com.emc.mongoose.storage.driver.net.http {

	requires com.emc.mongoose.storage.driver.net;
	requires com.emc.mongoose.api.common;
	requires com.emc.mongoose.api.model;
	requires com.emc.mongoose.ui;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires io.netty.buffer;
	requires io.netty.codec.http;
	requires io.netty.handler;
	requires io.netty.transport;
	requires log4j.api;
	requires java.base;

	exports com.emc.mongoose.storage.driver.net.http;
}