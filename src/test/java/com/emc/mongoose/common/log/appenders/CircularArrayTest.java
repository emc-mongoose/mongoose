package com.emc.mongoose.common.log.appenders;

import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;

import static org.junit.Assert.*;

/**
 * Created on 05.05.16.
 */
public class CircularArrayTest {

	private static class FakeLogEvent {

		private final long time;

		public FakeLogEvent(final long time) {
			this.time = time;
		}

		public static class FleComparator implements Comparator<FakeLogEvent> {

			@Override
			public int compare(FakeLogEvent fle1, FakeLogEvent fle2) {
				return Long.compare(fle1.time, fle2.time);
			}

		}

	}

	private CircularArray<FakeLogEvent> circularArray;

	@Before
	public void init() {
		circularArray = new CircularArray<>(10, new FakeLogEvent.FleComparator());
	}

	@Test
	public void searchItem() throws Exception {
		for (int i = 0; i < circularArray.size(); i++) {
			circularArray.addItem(new FakeLogEvent(System.currentTimeMillis()));
		}

	}
}