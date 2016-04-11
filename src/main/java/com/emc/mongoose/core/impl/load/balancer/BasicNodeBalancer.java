package com.emc.mongoose.core.impl.load.balancer;
//
import com.emc.mongoose.core.api.load.balancer.Balancer;
//
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
/**
 Created by kurila on 08.12.15.
 */
public final class BasicNodeBalancer
implements Balancer<String> {
	//
	private final String nodes[];
	private final Map<String, AtomicInteger> activeTasksMap;
	private int rrc = 0;
	//
	public BasicNodeBalancer(final String nodes[]) {
		this.nodes = nodes;
		if(nodes == null) {
			this.activeTasksMap = null;
		} else {
			this.activeTasksMap = new HashMap<>(nodes.length);
			for(String node : nodes) {
				activeTasksMap.put(node, new AtomicInteger(0));
			}
		}
	}
	//
	@Override
	public final void markTaskStart(final String subject)
	throws NullPointerException {
		activeTasksMap.get(subject).incrementAndGet();
	}
	//
	@Override
	public final void markTasksStart(final String subject, final int n)
	throws NullPointerException {
		activeTasksMap.get(subject).addAndGet(n);
	}
	//
	@Override
	public void markTaskFinish(final String subject)
	throws NullPointerException {
		activeTasksMap.get(subject).decrementAndGet();
	}
	//
	@Override
	public void markTasksFinish(final String subject, final int n)
	throws NullPointerException {
		activeTasksMap.get(subject).addAndGet(-n);
	}
	//
	private final static ThreadLocal<List<String>> BEST_NODES = new ThreadLocal<List<String>>() {
		@Override
		protected final List<String> initialValue() {
			return new ArrayList<>();
		}
	};
	//
	@Override
	public final String getNext() {
		if(nodes == null) {
			return null;
		}
		final List<String> bestNodes = BEST_NODES.get();
		bestNodes.clear();
		int minActiveTaskCount = Integer.MAX_VALUE, nextActiveTaskCount;
		for(final String nextNode : nodes) {
			nextActiveTaskCount = activeTasksMap.get(nextNode).get();
			if(nextActiveTaskCount < minActiveTaskCount) {
				minActiveTaskCount = nextActiveTaskCount;
				bestNodes.clear();
				bestNodes.add(nextNode);
			} else if(nextActiveTaskCount == minActiveTaskCount) {
				bestNodes.add(nextNode);
			}
		}
		// round robin counter
		rrc = rrc == Short.MAX_VALUE ? 0 : rrc + 1;
		// select using round robin counter if there are more than 1 best nodes
		return bestNodes.get(rrc % bestNodes.size());
	}
}
