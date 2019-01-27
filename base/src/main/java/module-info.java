import com.emc.mongoose.base.config.InitialConfigSchemaProvider;
import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.storage.driver.mock.DummyStorageDriverMockExtension;
import com.github.akurilov.confuse.SchemaProvider;

module com.emc.mongoose.base {
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
	requires java.rmi;
	requires java.scripting;
	requires java.logging;
	requires javax.servlet.api;

	exports com.emc.mongoose.base;
	exports com.emc.mongoose.base.concurrent;
	exports com.emc.mongoose.base.config;
	exports com.emc.mongoose.base.data;
	exports com.emc.mongoose.base.env;
	exports com.emc.mongoose.base.exception;
	exports com.emc.mongoose.base.item;
	exports com.emc.mongoose.base.item.io;
	exports com.emc.mongoose.base.item.op;
	exports com.emc.mongoose.base.item.op.composite;
	exports com.emc.mongoose.base.item.op.composite.data;
	exports com.emc.mongoose.base.item.op.data;
	exports com.emc.mongoose.base.item.op.partial;
	exports com.emc.mongoose.base.item.op.partial.data;
	exports com.emc.mongoose.base.item.op.path;
	exports com.emc.mongoose.base.item.op.token;
	exports com.emc.mongoose.base.load.generator;
	exports com.emc.mongoose.base.load.step;
	exports com.emc.mongoose.base.load.step.client;
	exports com.emc.mongoose.base.load.step.client.metrics;
	exports com.emc.mongoose.base.load.step.file;
	exports com.emc.mongoose.base.load.step.local;
	exports com.emc.mongoose.base.load.step.local.context;
	exports com.emc.mongoose.base.load.step.service;
	exports com.emc.mongoose.base.load.step.service.file;
	exports com.emc.mongoose.base.logging;
	exports com.emc.mongoose.base.metrics;
	exports com.emc.mongoose.base.metrics.snapshot;
	exports com.emc.mongoose.base.metrics.context;
	exports com.emc.mongoose.base.metrics.type;
	exports com.emc.mongoose.base.metrics.util;
	exports com.emc.mongoose.base.storage;
	exports com.emc.mongoose.base.storage.driver;
	exports com.emc.mongoose.base.storage.driver.mock;
	exports com.emc.mongoose.base.supply;
	exports com.emc.mongoose.base.supply.async;
	exports com.emc.mongoose.base.svc;

	provides Extension with
		DummyStorageDriverMockExtension;
	provides SchemaProvider with
		InitialConfigSchemaProvider;

	uses Extension;
	uses SchemaProvider;
}
