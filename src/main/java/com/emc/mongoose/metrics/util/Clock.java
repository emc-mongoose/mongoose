package com.emc.mongoose.metrics.util;

/**
 * Copied from the dropwizard metrics library 4.0.3.
 * An abstraction for how time passes.
 */
public abstract class Clock {
	/**
	 * Returns the current time tick.
	 *
	 * @return time tick in nanoseconds
	 */
	public abstract long getTick();

	/**
	 * Returns the current time in milliseconds.
	 *
	 * @return time in milliseconds
	 */
	public long getTime() {
		return System.currentTimeMillis();
	}

	/**
	 * The default clock to use.
	 *
	 * @return the default {@link Clock} instance
	 * @see Clock.UserTimeClock
	 */
	public static Clock defaultClock() {
		return Clock.UserTimeClockHolder.DEFAULT;
	}

	/**
	 * A clock implementation which returns the current time in epoch nanoseconds.
	 */
	public static class UserTimeClock extends Clock {
		@Override
		public final long getTick() {
			return System.nanoTime();
		}
	}

	private static class UserTimeClockHolder {
		private static final Clock DEFAULT = new UserTimeClock();
	}
}
