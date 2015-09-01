package com.emc.mongoose.core.impl.load.model;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.model.LoadState;
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 19.06.15.
 */
public class BasicLoadState implements LoadState {
	//
	private final int loadNumber;
	private final RunTimeConfig runTimeConfig;
	private final long countSucc;
	private final long countFail;
	private final long countBytes;
	private final long countSubm;
	private final long timeValue;
	private final TimeUnit timeUnit;
	private long durationValues[], latencyValues[];
	//
	@Override
	public int getLoadNumber() {
		return loadNumber;
	}
	//
	@Override
	public RunTimeConfig getRunTimeConfig() {
		return runTimeConfig;
	}
	//
	@Override
	public long getCountSucc() {
		return countSucc;
	}
	//
	@Override
	public long getCountFail() {
		return countFail;
	}
	//
	@Override
	public long getCountBytes() {
		return countBytes;
	}
	//
	@Override
	public long getCountSubm() {
		return countSubm;
	}
	//
	@Override
	public long [] getDurationValues() {
		return durationValues;
	}
	//
	@Override
	public long [] getLatencyValues() {
		return latencyValues;
	}
	//
	@Override
	public TimeUnit getLoadElapsedTimeUnit() {
		return timeUnit;
	}
	//
	@Override
	public long getLoadElapsedTimeValue() {
		return timeValue;
	}
	//
	@Override
	public boolean isLoadFinished(final RunTimeConfig rtConfig) {
		//  time limitations
		final long loadTimeMillis = (rtConfig.getLoadLimitTimeUnit().
			toMillis(rtConfig.getLoadLimitTimeValue())) > 0
			? (rtConfig.getLoadLimitTimeUnit().
			toMillis(rtConfig.getLoadLimitTimeValue())) : Long.MAX_VALUE;
		final long stateTimeMillis = getLoadElapsedTimeUnit().
			toMillis(getLoadElapsedTimeValue());
		//  count limitations
		final long counterResults = getCountSucc() + getCountFail();
		final long maxCount = rtConfig.getLoadLimitCount() > 0
			? rtConfig.getLoadLimitCount() : Long.MAX_VALUE;
		return (counterResults >= maxCount) || (stateTimeMillis >= loadTimeMillis);
	}
	//
	public static class Builder
	implements LoadState.Builder<BasicLoadState> {
		//
		private int loadNumber;
		private RunTimeConfig runTimeConfig;
		private long countSucc;
		private long countFail;
		private long countBytes;
		private long countSubm;
		private long timeValue;
		private TimeUnit timeUnit;
		private long durationValues[], latencyValues[];
		//
		@Override
		public Builder setLoadNumber(final int loadNumber) {
			this.loadNumber = loadNumber;
			return this;
		}
		//
		@Override
		public Builder setRunTimeConfig(final RunTimeConfig runTimeConfig) {
			this.runTimeConfig = runTimeConfig;
			return this;
		}
		//
		@Override
		public Builder setCountSucc(final long countSucc) {
			this.countSucc = countSucc;
			return this;
		}
		//
		@Override
		public Builder setCountFail(final long countFail) {
			this.countFail = countFail;
			return this;
		}
		//
		@Override
		public Builder setCountBytes(final long countBytes) {
			this.countBytes = countBytes;
			return this;
		}
		//
		@Override
		public Builder setCountSubm(final long countSubm) {
			this.countSubm = countSubm;
			return this;
		}
		//
		@Override
		public Builder setLoadElapsedTimeValue(final long timeValue) {
			this.timeValue = timeValue;
			return this;
		}
		//
		@Override
		public Builder setLoadElapsedTimeUnit(final TimeUnit timeUnit) {
			this.timeUnit = timeUnit;
			return this;
		}
		//
		@Override
		public Builder setDurationValues(final long durationValues[]) {
			this.durationValues = durationValues;
			return this;
		}
		//
		@Override
		public Builder setLatencyValues(final long latencyValues[]) {
			this.latencyValues = latencyValues;
			return this;
		}
		//
		@Override
		public BasicLoadState build() {
			return new BasicLoadState(this);
		}
		//
	}
	//
	private BasicLoadState(final Builder builder) {
		this.loadNumber = builder.loadNumber;
		this.runTimeConfig = builder.runTimeConfig;
		this.countSucc = builder.countSucc;
		this.countFail = builder.countFail;
		this.countBytes = builder.countBytes;
		this.countSubm = builder.countSubm;
		this.timeValue = builder.timeValue;
		this.timeUnit = builder.timeUnit;
		this.durationValues = builder.durationValues;
		this.latencyValues = builder.latencyValues;
	}
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	public static void restoreScenarioState(final RunTimeConfig rtConfig) {
		final String fullStateFileName = Paths.get(
			RunTimeConfig.DIR_ROOT, Constants.DIR_LOG, rtConfig.getRunId()
		).resolve(Constants.STATES_FILE).toString();
		//  if load states list is empty or file w/ load states doesn't exist, then init
		//  map entry value w/ empty list
		LoadExecutor.RESTORED_STATES_MAP.put(rtConfig.getRunId(), new ArrayList<LoadState>());
		if(isSavedStateOfRunExists(rtConfig.getRunId())) {
			final List<LoadState> loadStates =
				getScenarioStateFromFile(rtConfig.getRunId(), fullStateFileName);
			if(loadStates != null && !loadStates.isEmpty()) {
				//  check if immutable params were changed for load executors
				for(final LoadState state : loadStates) {
					if(rtConfig.isImmutableParamsChanged(state.getRunTimeConfig())) {
						LOG.warn(
							Markers.MSG,
							"Run \"{}\": configuration immutability violated. Starting new run",
							rtConfig.getRunId()
						);
						return;
					}
				}
				//  override load states list
				LoadExecutor.RESTORED_STATES_MAP.put(rtConfig.getRunId(), loadStates);
				LOG.info(Markers.MSG, "Run \"{}\" was resumed", rtConfig.getRunId());
				//  don't remove state file if load executor has been already finished
				for(final LoadState state : loadStates) {
					if (state.isLoadFinished(rtConfig))
						return;
				}
				//  remove state file when scenario's state was restored
				removePrevStateFile(fullStateFileName);
			}
		} else {
			LOG.info(Markers.MSG, "Could not find saved state of run \"{}\". Starting new run",
				rtConfig.getRunId());
		}
	}
	//
	public static boolean isSavedStateOfRunExists(final String runId) {
		final String fullStateFileName = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runId).resolve(Constants.STATES_FILE).toString();
		final File stateFile = new File(fullStateFileName);
		return stateFile.exists();
	}
	//
	@SuppressWarnings("unchecked")
	private static List<LoadState> getScenarioStateFromFile(
		final String runId, final String fileName
	) {
		try(final FileInputStream fis = new FileInputStream(fileName)) {
			try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
				return (List<LoadState>) ois.readObject();
			}
		} catch (final FileNotFoundException e) {
			LOG.debug(
				Markers.MSG, "Could not find saved state of run \"{}\". Starting new run", runId
			);
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"Failed to load state of run \"{}\" from \"{}\" file." +
					"Starting new run", runId, fileName);
		} catch (final ClassNotFoundException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to deserialize state of run." +
				"Starting new run");
		}
		return null;
	}
	//
	private static void removePrevStateFile(final String fileName) {
		try {
			Files.delete(Paths.get(fileName));
		} catch (final NoSuchFileException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"File \"{}\" with state of run wasn't found", fileName);
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"Failed to remove the file \"{}\"", fileName);
		}
	}
	//
	public static LoadState findStateByLoadNumber(
		final int loadNumber, final RunTimeConfig rtConfig
	) {
		final List<LoadState> loadStates =
			LoadExecutor.RESTORED_STATES_MAP.get(rtConfig.getRunId());
		for (final LoadState state : loadStates) {
			if (state.getLoadNumber() == loadNumber) {
				return state;
			}
		}
		return null;
	}
	//
	public static boolean isScenarioFinished(final RunTimeConfig rtConfig) {
		final Queue<LoadState> states = LoadCloseHook.LOAD_STATES.get(rtConfig.getRunId());
		//
		final long runTimeMillis = (rtConfig.getLoadLimitTimeUnit().
			toMillis(rtConfig.getLoadLimitTimeValue())) > 0
			? (rtConfig.getLoadLimitTimeUnit().
			toMillis(rtConfig.getLoadLimitTimeValue())) : Long.MAX_VALUE;
		final long maxItemsCountPerLoad = (rtConfig.getLoadLimitCount()) > 0
			? rtConfig.getLoadLimitCount() : Long.MAX_VALUE;
		//
		for (final LoadState state : states) {
			final long stateTimeMillis = state.getLoadElapsedTimeUnit()
				.toMillis(state.getLoadElapsedTimeValue());
			final long stateItemsCount = state.getCountSucc() + state.getCountFail();
			if ((stateTimeMillis < runTimeMillis) && (stateItemsCount < maxItemsCountPerLoad)
					&& (stateItemsCount < state.getCountSubm())) {
				return false;
			}
		}
		return true;
	}
	//
	public static void saveScenarioState(final RunTimeConfig rtConfig) {
		final String currRunId = rtConfig.getRunId();
		final String fullStateFileName = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, currRunId).resolve(Constants.STATES_FILE).toString();
		try (final FileOutputStream fos = new FileOutputStream(fullStateFileName, false)) {
			try (final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				oos.writeObject(new ArrayList<>(LoadCloseHook.LOAD_STATES.get(currRunId)));
			}
			LOG.info(Markers.MSG, "Successfully saved state of run \"{}\"",
				currRunId);
			LOG.debug(Markers.MSG, "State of run was successfully saved in \"{}\" file",
				fullStateFileName);
			LoadCloseHook.LOAD_STATES.remove(currRunId);
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e,
				"Failed to save state of run \"{}\"",
				currRunId);
		}
	}
}
