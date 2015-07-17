package com.emc.mongoose.common.conf;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by gusakk on 15.07.15.
 */
public class RunTimeConfigTest {

	@Test
	public void shouldHaveAliasesForNewConfParamNames()
	throws Exception {
		RunTimeConfig.initContext();
		final Map<String, String[]> mapOverride = RunTimeConfig.MAP_OVERRIDE;

		Assert.assertArrayEquals(mapOverride.get("api.s3.port"),
			new String[] { "api.type.s3.port" });
		Assert.assertArrayEquals(mapOverride.get("api.s3.auth.prefix"),
			new String[] { "api.type.s3.authPrefix" });
		Assert.assertArrayEquals(mapOverride.get("api.s3.bucket"),
			new String[] { "api.type.s3.bucket" });
		Assert.assertArrayEquals(mapOverride.get("api.s3.bucket.filesystem"),
			new String[] { "data.fsAccess" });
		Assert.assertArrayEquals(mapOverride.get("api.s3.bucket.versioning"),
			new String[] { "data.versioning" });

		Assert.assertArrayEquals(mapOverride.get("api.atmos.port"),
			new String[] { "api.type.atmos.port" });
		Assert.assertArrayEquals(mapOverride.get("api.atmos.subtenant"),
			new String[] { "api.type.atmos.subtenant" });

		Assert.assertArrayEquals(mapOverride.get("api.swift.port"),
			new String[] { "api.type.swift.port" });
		Assert.assertArrayEquals(mapOverride.get("api.swift.service.basepath"),
			new String[] { "api.type.swift.serviceBasepath" });
		Assert.assertArrayEquals(mapOverride.get("api.swift.auth.token"),
			new String[] { "api.type.swift.authToken" });
		Assert.assertArrayEquals(mapOverride.get("api.swift.container"),
			new String[] { "api.type.swift.container" });

		Assert.assertArrayEquals(mapOverride.get("storage.api"),
			new String[] { "api.name" });
		Assert.assertArrayEquals(mapOverride.get("storage.fsAccess"),
			new String[] { "data.fsAccess" });
		Assert.assertArrayEquals(mapOverride.get("storage.versioning"),
			new String[] { "data.versioning" });
		Assert.assertArrayEquals(mapOverride.get("storage.mock.data.offset.radix"),
			new String[] { "data.radix.offset" });
		Assert.assertArrayEquals(mapOverride.get("storage.mock.data.size.radix"),
			new String[] { "data.radix.size" });
		Assert.assertArrayEquals(mapOverride.get("storage.mock.head.count"),
			new String[] { "storage.mock.headCount" });
		Assert.assertArrayEquals(mapOverride.get("storage.mock.iothreads.persocket"),
			new String[] { "ioThreadsPerSocket" });
		Assert.assertArrayEquals(mapOverride.get("storage.mock.fault.sleep.msec"),
			new String[] { "storage.mock.fault.sleepMilliSec" });
		Assert.assertArrayEquals(mapOverride.get("storage.connection.timeout.millisec"),
			new String[] { "remote.connection.timeoutMilliSec" });
		Assert.assertArrayEquals(mapOverride.get("storage.connection.pool.timeout.millisec"),
			new String[] { "remote.connection.poolTimeoutMilliSec" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.timeout.millisec"),
			new String[] { "remote.socket.timeoutMilliSec" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.reuse.addr"),
			new String[] { "remote.socket.reuseAddr" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.keepalive"),
			new String[] { "remote.socket.keepalive" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.tcp.nodelay"),
			new String[] { "remote.socket.tcpNoDelay" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.linger"),
			new String[] { "remote.socket.linger" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.bind.backlog.size"),
			new String[] { "remote.socket.bindBacklogSize" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.interest.op.queued"),
			new String[] { "remote.socket.interestOpQueued" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.select.interval"),
			new String[] { "remote.socket.selectInterval" });
		Assert.assertArrayEquals(mapOverride.get("storage.socket.select.interval"),
			new String[] { "remote.socket.selectInterval" });

		Assert.assertArrayEquals(mapOverride.get("data.page.size"),
			new String[] { "data.buffer.size" });
		Assert.assertArrayEquals(mapOverride.get("data.ring.seed"),
			new String[] { "data.buffer.ring.seed" });
		Assert.assertArrayEquals(mapOverride.get("data.ring.size"),
			new String[] { "data.buffer.ring.size" });

		Assert.assertArrayEquals(mapOverride.get("http.sign.method"),
			new String[] { "http.signMethod" });

		Assert.assertArrayEquals(mapOverride.get("load.create.threads"),
			new String[] { "load.type.create.threads" });
		Assert.assertArrayEquals(mapOverride.get("load.read.threads"),
			new String[] { "load.type.read.threads" });
		Assert.assertArrayEquals(mapOverride.get("load.read.verify.content"),
			new String[] { "load.type.read.verifyContent" });
		Assert.assertArrayEquals(mapOverride.get("load.update.threads"),
			new String[] { "load.type.update.threads" });
		Assert.assertArrayEquals(mapOverride.get("load.update.per.item"),
			new String[] { "load.type.update.perItem" });
		Assert.assertArrayEquals(mapOverride.get("load.delete.threads"),
			new String[] { "load.type.delete.threads" });
		Assert.assertArrayEquals(mapOverride.get("load.append.threads"),
			new String[] { "load.type.append.threads" });

		Assert.assertArrayEquals(mapOverride.get("load.time"),
			new String[] { "load.limit.time" });
		Assert.assertArrayEquals(mapOverride.get("run.time"),
			new String[] { "load.limit.time" });
		Assert.assertArrayEquals(mapOverride.get("data.count"),
			new String[] { "load.limit.count" });

		Assert.assertArrayEquals(mapOverride.get("load.drivers"),
			new String[] { "load.servers" });
		Assert.assertArrayEquals(mapOverride.get("remote.servers"),
			new String[] { "load.servers" });
		Assert.assertArrayEquals(mapOverride.get("remote.control.port"),
			new String[] { "remote.port.control" });
		Assert.assertArrayEquals(mapOverride.get("remote.export.port"),
			new String[] { "remote.port.export" });
		Assert.assertArrayEquals(mapOverride.get("remote.import.port"),
			new String[] { "remote.port.import" });
		Assert.assertArrayEquals(mapOverride.get("remote.wuisvc.port"),
			new String[] { "remote.port.webui" });

		Assert.assertArrayEquals(mapOverride.get("run.metrics.period.sec"),
			new String[] { "load.metricsPeriodSec" });
		Assert.assertArrayEquals(mapOverride.get("run.scenario.name"),
			new String[] { "scenario.name" });
		Assert.assertArrayEquals(mapOverride.get("run.scenario.lang"),
			new String[] { "scenario.lang" });
		Assert.assertArrayEquals(mapOverride.get("run.scenario.dir"),
			new String[] { "scenario.dir" });
		Assert.assertArrayEquals(mapOverride.get("run.request.queue.size"),
			new String[] { "load.tasks.maxQueueSize" });

		Assert.assertArrayEquals(mapOverride.get("scenario.single.load"),
			new String[] { "scenario.type.single.load" });

		Assert.assertArrayEquals(mapOverride.get("scenario.chain.load"),
			new String[] { "scenario.type.chain.load" });
		Assert.assertArrayEquals(mapOverride.get("scenario.chain.concurrent"),
			new String[] { "scenario.type.chain.concurrent" });
		Assert.assertArrayEquals(mapOverride.get("scenario.chain.itemsbuffer"),
			new String[] { "scenario.type.chain.itemsbuffer" });

		Assert.assertArrayEquals(mapOverride.get("scenario.rampup.thread.counts"),
			new String[] { "scenario.type.rampup.threadCounts" });
		Assert.assertArrayEquals(mapOverride.get("scenario.rampup.sizes"),
			new String[] { "scenario.type.rampup.sizes" });

		Assert.assertArrayEquals(mapOverride.get("data.size"),
			new String[] { "data.size.min", "data.size.max" });
		Assert.assertArrayEquals(mapOverride.get("load.threads"),
			new String[] {
				"load.type.append.threads",
				"load.type.create.threads",
				"load.type.read.threads",
				"load.type.update.threads",
				"load.type.delete.threads"
			});
	}
}