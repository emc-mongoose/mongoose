package com.emc.mongoose.client.impl.load.model.metrics;

import com.emc.mongoose.client.api.load.tasks.LoadStatsSnapshotTask;
import com.emc.mongoose.client.impl.load.tasks.LoadIntermediateStatsSnapshotTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.server.api.load.executor.LoadSvc;

import java.util.Map;

/**
 Created by kurila on 28.06.16.
 */
public class DistributedIntermediateIoStats<T extends Item, W extends LoadSvc<T>>
extends DistributedIoStats<T, W> {
	
	public DistributedIntermediateIoStats(
		final String name, final boolean serveJmxFlag, final Map<String, W> loadSvcMap
	) {
		super(name, serveJmxFlag, loadSvcMap);
	}

	@Override
	public void start() {
		LoadStatsSnapshotTask nextLoadStatsSnapshotTask;
		for(final String addr : loadSvcMap.keySet()) {
			nextLoadStatsSnapshotTask = new LoadIntermediateStatsSnapshotTask(
				loadSvcMap.get(addr), addr
			);
			loadStatsSnapshotMap.put(addr, nextLoadStatsSnapshotTask);
			statsLoader.submit(nextLoadStatsSnapshotTask);
		}
		statsLoader.shutdown();
		super.start();
	}
}
