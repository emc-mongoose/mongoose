module com.emc.mongoose.api.model {

	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires com.emc.mongoose.api.common;
	requires org.apache.commons.codec;
	requires slf4j.api;
	requires log4j.api;
	requires java.base;
	requires java.logging;
	requires java.rmi;

	exports com.emc.mongoose.api.model.concurrent;
	exports com.emc.mongoose.api.model.data;
	exports com.emc.mongoose.api.model.io;
	exports com.emc.mongoose.api.model.io.task;
	exports com.emc.mongoose.api.model.io.task.composite;
	exports com.emc.mongoose.api.model.io.task.composite.data;
	exports com.emc.mongoose.api.model.io.task.data;
	exports com.emc.mongoose.api.model.io.task.partial;
	exports com.emc.mongoose.api.model.io.task.partial.data;
	exports com.emc.mongoose.api.model.io.task.path;
	exports com.emc.mongoose.api.model.io.task.token;
	exports com.emc.mongoose.api.model.item;
	exports com.emc.mongoose.api.model.load;
	exports com.emc.mongoose.api.model.storage;
	exports com.emc.mongoose.api.model.svc;
}
