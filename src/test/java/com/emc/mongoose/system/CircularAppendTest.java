package com.emc.mongoose.system;

import com.emc.mongoose.system.base.params.Concurrency;
import com.emc.mongoose.system.base.params.EnvParams;
import com.emc.mongoose.system.base.params.ItemSize;
import com.emc.mongoose.system.base.params.RunMode;
import com.emc.mongoose.system.base.params.StorageType;
import com.emc.mongoose.system.util.docker.NodeSvcContainer;
import com.emc.mongoose.system.util.docker.StorageMockContainer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class CircularAppendTest {

	@Parameterized.Parameters(name = "{0}, {1}, {2}, {3}")
	public static List<Object[]> envParams() {
		return EnvParams.PARAMS;
	}

	private static final List<StorageMockContainer> storageMocks = new ArrayList<>();
	private static final List<NodeSvcContainer> nodeSvcs = new ArrayList<>();

	private final StorageType storageType;
	private final RunMode runMode;
	private final Concurrency concurrency;
	private final ItemSize itemSize;

	public CircularAppendTest(
		final StorageType storageType, final RunMode runMode, final Concurrency concurrency,
		final ItemSize itemSize
	) {
		this.storageType = storageType;
		this.runMode = runMode;
		this.concurrency = concurrency;
		this.itemSize = itemSize;
	}

	@BeforeClass
	public static void setUp()
	throws Exception {

		for(int i = 0; i < 2; i ++) {
			final StorageMockContainer storageMock = new StorageMockContainer(
				8000 + i, false, null, null, Character.MAX_RADIX, 1_000_000, 1_000_000, 1_000_000,
				0, 0, 0
			);
			storageMock.start();
			storageMocks.add(storageMock);
			final NodeSvcContainer nodeSvc = new NodeSvcContainer("4.0.0", 10000 + i);
			nodeSvc.start();
			nodeSvcs.add(nodeSvc);
		}
	}

	@AfterClass
	public static void tearDown()
	throws Exception {
		nodeSvcs.parallelStream().forEach(
			storageMock -> {
				try {
					storageMock.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		);
		storageMocks.parallelStream().forEach(
			storageMock -> {
				try {
					storageMock.close();
				} catch(final Throwable t) {
					t.printStackTrace(System.err);
				}
			}
		);
	}

	@Test
	public void test()
	throws Exception {
		System.out.println("yohoho");
	}
}
