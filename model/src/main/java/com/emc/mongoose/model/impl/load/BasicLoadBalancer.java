package com.emc.mongoose.model.impl.load;

import com.emc.mongoose.model.api.load.LoadBalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 08.12.15.
 */
public final class BasicLoadBalancer<S>
implements LoadBalancer<S> {
	
	private final static ThreadLocal<List<Object>> BEST_CHOICES = new ThreadLocal<List<Object>>() {
		@Override
		protected final List<Object> initialValue() {
			return new ArrayList<>();
		}
	};
	private AtomicInteger rrc = new AtomicInteger(0);
	private final S options[];
	private final Map<S, AtomicInteger> leaseMap;
	
	public BasicLoadBalancer(final S options[]) {
		this.options = options;
		if(options == null) {
			this.leaseMap = null;
		} else {
			this.leaseMap = new HashMap<>(options.length);
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
	public final int getLeasedCount() {
		int sum = 0;
		for(final S option : options) {
			sum += leaseMap.get(option).get();
		}
		return sum;
	}
	
	@Override
	public final S get() {
		if(options == null) {
			return null;
		}
		if(options.length == 1) {
			return options[0];
		}
		final List<S> bestChoices = (List<S>) this.BEST_CHOICES.get();
		bestChoices.clear();
		int minLeaseCount = Integer.MAX_VALUE, nextLeaseCount;
		for(final S nextChoice : options) {
			nextLeaseCount = leaseMap.get(nextChoice).get();
			if(nextLeaseCount < minLeaseCount) {
				minLeaseCount = nextLeaseCount;
				bestChoices.clear();
				bestChoices.add(nextChoice);
			} else if(nextLeaseCount == minLeaseCount) {
				bestChoices.add(nextChoice);
			}
		}
		// round robin counter
		if(!rrc.compareAndSet(Short.MAX_VALUE, 0)) {
			rrc.incrementAndGet();
		}
		// select using round robin counter if there are more than 1 best options
		return bestChoices.get(rrc.get() % bestChoices.size());
	}
	
	@Override
	public final int get(final List<S> buffer, final int limit) {
		if(options == null) {
			return 0;
		}
		if(options.length == 1) {
			final S choice = options[0];
			for(int i = 0; i < limit; i ++) {
				buffer.set(0, choice);
			}
		}
		final List<S> bestChoices = (List<S>) this.BEST_CHOICES.get();
		bestChoices.clear();
		int minLeaseCount = Integer.MAX_VALUE, nextLeaseCount;
		for(final S nextChoice : options) {
			nextLeaseCount = leaseMap.get(nextChoice).get();
			if(nextLeaseCount < minLeaseCount) {
				minLeaseCount = nextLeaseCount;
				bestChoices.clear();
				bestChoices.add(nextChoice);
			} else if(nextLeaseCount == minLeaseCount) {
				bestChoices.add(nextChoice);
			}
		}
		final int bestChoicesCount = bestChoices.size();
		for(int i = 0; i < limit; i ++) {
			buffer.add(bestChoices.get(i % bestChoicesCount));
		}
		return limit;
	}
	
	@Override
	public final void skip(final long count) {
		rrc.incrementAndGet();
	}
	
	@Override
	public final void reset() {
		rrc.set(0);
	}
	
	@Override
	public final void close() {
		for(int i = 0; i < options.length; i ++) {
			options[i] = null;
		}
		leaseMap.clear();
	}
}
