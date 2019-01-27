import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.storage.driver.coop.netty.NettyStorageDriverExtension;
import com.emc.mongoose.storage.driver.coop.netty.mock.NettyStorageDriverMockExtension;

module com.emc.mongoose.storage.driver.coop.netty {
	requires com.emc.mongoose.storage.driver.coop;
	requires com.emc.mongoose.base;
	requires com.github.akurilov.commons;
	requires com.github.akurilov.confuse;
	requires com.github.akurilov.confuse.io.json;
	requires com.github.akurilov.fiber4j;
	requires com.github.akurilov.netty.connection.pool;
	requires log4j.api;
	requires io.netty.buffer;
	requires io.netty.codec;
	requires io.netty.common;
	requires io.netty.handler;
	requires io.netty.transport;
	requires io.netty.transport.epoll;
	requires io.netty.transport.kqueue;
	requires java.base;

	exports com.emc.mongoose.storage.driver.coop.netty;
	exports com.emc.mongoose.storage.driver.coop.netty.data;
	exports com.emc.mongoose.storage.driver.coop.netty.mock;

	provides Extension with
		NettyStorageDriverExtension,
		NettyStorageDriverMockExtension;
}
