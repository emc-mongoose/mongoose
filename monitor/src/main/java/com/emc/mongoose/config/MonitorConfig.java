package com.emc.mongoose.config;

/**
 Created on 11.07.16.
 */
public class MonitorConfig {

	public static final String KEY_JOB = "job";
	public static final String KEY_METRICS = "metrics";
	public static final String KEY_RUN = "run";
	private final Job job;
	private final Metrics metrics;
	private final Run run;

	public MonitorConfig(final Job job, final Metrics metrics, final Run run) {
		this.job = job;
		this.metrics = metrics;
		this.run = run;
	}

	public Job job() {
		return job;
	}

	public Metrics metrics() {
		return metrics;
	}

	public Run run() {
		return run;
	}

	public static class Job {

		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_LIMIT = "limit";
		private final boolean circular;
		private final Limit limit;

		public Job(final boolean circular, final Limit limit) {
			this.circular = circular;
			this.limit = limit;
		}

		public boolean getCircular() {
			return circular;
		}

		public Limit limit() {
			return limit;
		}

		public static class Limit {

			public static final String KEY_COUNT = "count";
			public static final String KEY_RATE = "rate";
			public static final String KEY_SIZE = "size";
			public static final String KEY_TIME = "time";
			private int count;
			private int rate;
			private int size;
			private String time;

			public Limit(final int count, final int rate, final int size, final String time) {
				this.count = count;
				this.rate = rate;
				this.size = size;
				this.time = time;
			}

			public int getCount() {
				return count;
			}

			public int getRate() {
				return rate;
			}

			public int getSize() {
				return size;
			}

			public String getTime() {
				return time;
			}
		}
	}

	public static class Metrics {

		public static final String KEY_INTERMEDIATE = "intermediate";
		public static final String KEY_PERIOD = "period";
		public static final String KEY_PRECONDITION= "precondition";
		private final boolean intermediate;
		private final String period;
		private final boolean precondition;

		public Metrics(
			final boolean intermediate, final String period, final boolean precondition
		) {
			this.intermediate = intermediate;
			this.period = period;
			this.precondition = precondition;
		}

		public boolean getIntermediate() {
			return intermediate;
		}

		public String getPeriod() {
			return period;
		}

		public boolean getPrecondition() {
			return precondition;
		}
	}

	public static class Run {

		public static final String KEY_FILE = "file";
		public static final String KEY_ID = "id";
		private final String file;
		private final String id;

		public Run(final String file, final String id) {
			this.file = file;
			this.id = id;
		}

		public String getFile() {
			return file;
		}

		public String getId() {
			return id;
		}
	}

}
