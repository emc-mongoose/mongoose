import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.http.s3.AmzS3StorageDriverExtension;
import com.github.akurilov.confuse.SchemaProvider;

module com.emc.mongoose.storage.driver.coop.netty.http.s3 {
	requires com.emc.mongoose.storage.driver.coop.netty.http;
	requires com.emc.mongoose.base;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires io.netty.buffer;
	requires io.netty.common;
	requires io.netty.codec.http;
	requires io.netty.transport;
	requires log4j.api;
	requires java.base;
	requires java.xml;

	exports com.emc.mongoose.storage.driver.coop.netty.http.s3;

	provides Extension with
		AmzS3StorageDriverExtension;

	uses Extension;
	uses SchemaProvider;
}
