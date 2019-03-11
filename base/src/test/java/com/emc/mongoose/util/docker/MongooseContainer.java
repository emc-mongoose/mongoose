package com.emc.mongoose.util.docker;

import com.github.akurilov.commons.system.SizeInBytes;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface MongooseContainer {
	String IMAGE_NAME = "emcmongoose/mongoose";
	String ENTRYPOINT = "/opt/mongoose/entrypoint.sh";
	String ENTRYPOINT_DEBUG = "/opt/mongoose/entrypoint_debug.sh";
	String ENTRYPOINT_LIMIT_HEAP_1GB = "/opt/mongoose/entrypoint_limit_heap_1GB.sh";
	int PORT_DEBUG = 5005;
	int PORT_JMX = 9010;
	String CONTAINER_SHARE_PATH = MongooseEntryNodeContainer.CONTAINER_HOME_PATH + "/share";
	Path HOST_SHARE_PATH = Paths.get(MongooseEntryNodeContainer.APP_HOME_DIR, "share");
	String IMAGE_VERSION = System.getenv("MONGOOSE_IMAGE_VERSION");
	long ENDURANCE_TEST_MEMORY_LIMIT = SizeInBytes.toFixedSize("3GB");
}
