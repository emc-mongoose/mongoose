module com.emc.mongoose {

	requires com.github.akurilov.commons;
	requires com.github.akurilov.concurrent;
	requires log4j.api;
	requires java.base;
	requires java.rmi;
	requires java.scripting;

	exports com.emc.mongoose.concurrent;
	exports com.emc.mongoose.data;
	exports com.emc.mongoose.storage.driver;
}
