package com.emc.mongoose.monitor.config;

/**
 Created on 11.07.16.
 */
public class MonitorConfig {

	public static final String KEY_JOB = "job";
	public static final String KEY_METRICS = "metrics";
	public static final String KEY_RUN = "run";
	private final JobConfig jobConfig;
	private final MetricsConfig metricsConfig;
	private final RunConfig runConfig;

	public MonitorConfig(final JobConfig jobConfig, final MetricsConfig metricsConfig, final RunConfig runConfig) {
		this.jobConfig = jobConfig;
		this.metricsConfig = metricsConfig;
		this.runConfig = runConfig;
	}

	public JobConfig getJobConfig() {
		return jobConfig;
	}

	public MetricsConfig getMetricsConfig() {
		return metricsConfig;
	}

	public RunConfig getRunConfig() {
		return runConfig;
	}

	public static class JobConfig {

		public static final String KEY_CIRCULAR = "circular";
		public static final String KEY_LIMIT = "limit";
		private final boolean circular;
		private final LimitConfig limitConfig;

		public JobConfig(final boolean circular, final LimitConfig limitConfig) {
			this.circular = circular;
			this.limitConfig = limitConfig;
		}

		public boolean getCircular() {
			return circular;
		}

		public LimitConfig getLimitConfig() {
			return limitConfig;
		}

		public static class LimitConfig {

			public static final String KEY_COUNT = "count";
			public static final String KEY_RATE = "rate";
			public static final String KEY_SIZE = "size";
			public static final String KEY_TIME = "time";
			private final int count;
			private final int rate;
			private final int size;
			private final String time;

			public LimitConfig(final int count, final int rate, final int size, final String time) {
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

	public static class MetricsConfig {

		public static final String KEY_INTERMEDIATE = "intermediate";
		public static final String KEY_PERIOD = "period";
		public static final String KEY_PRECONDITION= "precondition";
		private final boolean intermediate;
		private final String period;
		private final boolean precondition;

		public MetricsConfig(
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

	public static class RunConfig {

		public static final String KEY_FILE = "file";
		public static final String KEY_ID = "id";
		private final String file;
		private final String id;

		public RunConfig(final String file, final String id) {
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
