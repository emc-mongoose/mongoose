package com.emc.mongoose.model.impl.load;

import com.emc.mongoose.model.api.load.Balancer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 08.12.15.
 */
public final class BasicBalancer<S>
implements Balancer<S> {
	
	private final static ThreadLocal<List<Object>> BEST_CHOICES = new ThreadLocal<List<Object>>() {
		@Override
		protected final List<Object> initialValue() {
			return new ArrayList<>();
		}
	};
	private AtomicInteger rrc = new AtomicInteger(0);
	private final S nodes[];
	private final Map<S, AtomicInteger> leaseMap;
	
	public BasicBalancer(final S options[]) {
		this.nodes = options;
		if(options == null) {
			this.leaseMap = null;
		} else {
			this.leaseMap = new ConcurrentHashMap<>(options.length);
			for(final S node : options) {
				leaseMap.put(node, new AtomicInteger(0));
			}
		}
	}
	
	@Override
	public final void lease(final S subject)
	throws NullPointerException {
		leaseMap.get(subject).incrementAndGet();
	}
	
	@Override
	public final void leaseBatch(final S subject, final int n)
	throws NullPointerException {
		leaseMap.get(subject).addAndGet(n);
	}
	
	@Override
	public void release(final S subject)
	throws NullPointerException {
		leaseMap.get(subject).decrementAndGet();
	}
	
	@Override
	public void releaseBatch(final S subject, final int n)
	throws NullPointerException {
		leaseMap.get(subject).addAndGet(-n);
	}
	
	@Override
	public final S get() {
		if(nodes == null) {
			return null;
		}
		final List<S> bestChoices = (List<S>) this.BEST_CHOICES.get();
		bestChoices.clear();
		int minLeaseCount = Integer.MAX_VALUE, nextLeaseCount;
		for(final S nextNode : nodes) {
			nextLeaseCount = leaseMap.get(nextNode).get();
			if(nextLeaseCount < minLeaseCount) {
				minLeaseCount = nextLeaseCount;
				bestChoices.clear();
				bestChoices.add(nextNode);
			} else if(nextLeaseCount == minLeaseCount) {
				bestChoices.add(nextNode);
			}
		}
		// round robin counter
		rrc.compareAndSet(Short.MAX_VALUE)
		rrc = rrc == Short.MAX_VALUE ? 0 : rrc + 1;
		// select using round robin counter if there are more than 1 best nodes
		return bestChoices.get(rrc % bestChoices.size());
	}
	
	@Override
	public int get(final List<S> buffer, final int limit)
	throws IOException {
		return 0;
	}
	
	@Override
	public void skip(final long count)
	throws IOException {
		rrc.incrementAndGet();
	}
	
	@Override
	public void reset()
	throws IOException {
		rrc.set(0);
	}
	
	@Override
	public final void close()
	throws IOException {
	}
}
