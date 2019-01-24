import com.emc.mongoose.config.InitialConfigSchemaProvider;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.storage.driver.mock.DummyStorageDriverMockExtension;
import com.github.akurilov.confuse.SchemaProvider;

module com.emc.mongoose {
	requires com.github.akurilov.commons;
	requires com.github.akurilov.fiber4j;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires com.fasterxml.jackson.databind;
	requires log4j.api;
	requires log4j.core;
	requires simpleclient;
	requires simpleclient.common;
	requires simpleclient.servlet;
	requires org.eclipse.jetty.http;
	requires org.eclipse.jetty.rewrite;
	requires org.eclipse.jetty.server;
	requires org.eclipse.jetty.servlet;
	requires commons.lang;
	requires java.base;
	requires java.rmi;
	requires java.scripting;
	requires java.logging;
	requires javax.servlet.api;

	exports com.emc.mongoose;
	exports com.emc.mongoose.concurrent;
	exports com.emc.mongoose.config;
	exports com.emc.mongoose.data;
	exports com.emc.mongoose.env;
	exports com.emc.mongoose.exception;
	exports com.emc.mongoose.item;
	exports com.emc.mongoose.item.io;
	exports com.emc.mongoose.item.op;
	exports com.emc.mongoose.item.op.composite;
	exports com.emc.mongoose.item.op.composite.data;
	exports com.emc.mongoose.item.op.data;
	exports com.emc.mongoose.item.op.partial;
	exports com.emc.mongoose.item.op.partial.data;
	exports com.emc.mongoose.item.op.path;
	exports com.emc.mongoose.item.op.token;
	exports com.emc.mongoose.load.generator;
	exports com.emc.mongoose.load.step;
	exports com.emc.mongoose.load.step.client;
	exports com.emc.mongoose.load.step.client.metrics;
	exports com.emc.mongoose.load.step.file;
	exports com.emc.mongoose.load.step.local;
	exports com.emc.mongoose.load.step.local.context;
	exports com.emc.mongoose.load.step.service;
	exports com.emc.mongoose.load.step.service.file;
	exports com.emc.mongoose.logging;
	exports com.emc.mongoose.metrics;
	exports com.emc.mongoose.metrics.snapshot;
	exports com.emc.mongoose.metrics.context;
	exports com.emc.mongoose.metrics.type;
	exports com.emc.mongoose.metrics.util;
	exports com.emc.mongoose.storage;
	exports com.emc.mongoose.storage.driver;
	exports com.emc.mongoose.storage.driver.mock;
	exports com.emc.mongoose.supply;
	exports com.emc.mongoose.supply.async;
	exports com.emc.mongoose.svc;

	provides Extension with
		DummyStorageDriverMockExtension;
	provides SchemaProvider with
		InitialConfigSchemaProvider;

	uses Extension;
	uses SchemaProvider;
}
