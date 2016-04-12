package com.emc.mongoose.core.impl.load.generator;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
import com.emc.mongoose.core.api.load.generator.LoadState;
import com.emc.mongoose.core.api.load.metrics.IoStats;
//
import com.emc.mongoose.core.impl.load.tasks.LoadCloseHook;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
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
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 19.06.15.
 */
public class BasicLoadState<T extends Item>
implements LoadState<T> {
	//
	private final AppConfig appConfig;
	private final IoStats.Snapshot ioStatsSnapshot;
	private final T lastDataItem;
	//
	@Override
	public AppConfig getAppConfig() {
		return appConfig;
	}
	//
	@Override
	public IoStats.Snapshot getStatsSnapshot() {
		return ioStatsSnapshot;
	}
	//
	@Override
	public T getLastDataItem() {
		return lastDataItem;
	}
	//
	@Override
	public boolean isLimitReached(
		final long countLimit, final long timeLimit
	) {
		//  time limitations
		final TimeUnit loadLimitTimeUnit = TimeUnit.SECONDS;
		final long loadTimeMicroSec = timeLimit > 0 ?
			loadLimitTimeUnit.toMicros(timeLimit) : Long.MAX_VALUE;
		final long stateTimeMicroSec = ioStatsSnapshot.getElapsedTime();
		//  count limitations
		final long counterResults = ioStatsSnapshot.getSuccCount() + ioStatsSnapshot.getFailCount();
		final long maxCount = countLimit > 0 ?
			appConfig.getLoadLimitCount() : Long.MAX_VALUE;
		return (counterResults >= maxCount) || (stateTimeMicroSec >= loadTimeMicroSec);
	}
	//
	public static class Builder<T extends Item, U extends LoadState<T>>
	implements LoadState.Builder<T, U> {
		//
		private AppConfig appConfig;
		private IoStats.Snapshot ioStatsSnapshot;
		private T lastDataItem;
		//
		@Override
		public Builder<T, U> setAppConfig(final AppConfig appConfig) {
			this.appConfig = appConfig;
			return this;
		}
		//
		@Override
		public Builder<T, U> setStatsSnapshot(final IoStats.Snapshot ioStatsSnapshot) {
			this.ioStatsSnapshot = ioStatsSnapshot;
			return this;
		}
		//
		@Override
		public Builder<T, U> setLastItem(final T lastDataItem) {
			this.lastDataItem = lastDataItem;
			return this;
		}
		//
		@Override
		@SuppressWarnings("unchecked")
		public U build() {
			return (U) new BasicLoadState<>((Builder<T, LoadState<T>>) this);
		}
		//
	}
	//
	private BasicLoadState(final Builder<T, LoadState<T>> builder) {
		this.appConfig = builder.appConfig;
		this.ioStatsSnapshot = builder.ioStatsSnapshot;
		this.lastDataItem = builder.lastDataItem;
	}
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	public static void restoreScenarioState(
		final String runId, final String runMode, final String runVersion,
	    final long countLimit, final long timeLimitMicroSeconds
	) {
		final String fullStateFileName = Paths
			.get(BasicConfig.getRootDir(), Constants.DIR_LOG, runId)
			.resolve(Constants.STATES_FILE).toString();
		// if load states list is empty or file w/ load states doesn't exist, then init map entry
		// value w/ empty list
		LoadGenerator.RESTORED_STATES.put(
			runId, new ArrayList<LoadState<? extends Item>>()
		);
		if(isSavedStateOfRunExists(runId)) {
			final List<LoadState<? extends Item>>
				loadStates = getRunStateFromFile(runId, fullStateFileName);
			if(loadStates != null && !loadStates.isEmpty()) {
				// check if immutable params were changed for load generators
				for(final LoadState state : loadStates) {
					if(
						!runMode.equals(state.getAppConfig().getRunMode())
							||
						!runVersion.equals(state.getAppConfig().getRunVersion())
					) {
						LOG.warn(
							Markers.MSG,
							"Run \"{}\": configuration immutability violated. Starting new run",
							runId
						);
						return;
					}
				}
				// override load states list
				LoadGenerator.RESTORED_STATES.put(runId, loadStates);
				LOG.info(Markers.MSG, "Run \"{}\" was resumed", runId);
				// don't remove state file if load generator has been already finished
				for(final LoadState state : loadStates) {
					if(state.isLimitReached(countLimit, timeLimitMicroSeconds))
						return;
				}
				// remove state file when scenario's state was restored
				removePrevStateFile(fullStateFileName);
			}
		} else {
			LOG.info(
				Markers.MSG, "Could not find saved state of run \"{}\". Starting new run", runId
			);
		}
	}
	//
	public static boolean isSavedStateOfRunExists(final String runId) {
		final String fullStateFileName = Paths
			.get(BasicConfig.getRootDir(), Constants.DIR_LOG, runId)
			.resolve(Constants.STATES_FILE)
			.toString();
		final File stateFile = new File(fullStateFileName);
		return stateFile.exists();
	}
	//
	@SuppressWarnings("unchecked")
	private static List<LoadState<? extends Item>> getRunStateFromFile(
		final String runId, final String fileName
	) {
		try(final FileInputStream fis = new FileInputStream(fileName)) {
			try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
				return (List<LoadState<?>>) ois.readObject();
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
	public static boolean isRunFinished(final AppConfig appConfig, final List<LoadState> states) {
		final TimeUnit loadLimitTimeUnit = TimeUnit.SECONDS;
		final long loadLimitTimeValue = appConfig.getLoadLimitTime();
		final long timeLimitMicroSec = loadLimitTimeValue > 0 ?
			loadLimitTimeUnit.toMicros(appConfig.getLoadLimitTime()) : Long.MAX_VALUE;
		final long loadLimitCount = appConfig.getLoadLimitCount() > 0 ?
			appConfig.getLoadLimitCount() : Long.MAX_VALUE;
		//
		for(final LoadState state : states) {
			final IoStats.Snapshot statsSnapshot = state.getStatsSnapshot();
			if (statsSnapshot == null) {
				return true;
			}
			final long elapsedTimeMicroSec = statsSnapshot.getElapsedTime();
			final long stateItemsCount = statsSnapshot.getSuccCount() + statsSnapshot.getFailCount();
			if(elapsedTimeMicroSec >= timeLimitMicroSec) {
				LOG.debug(
					Markers.MSG, "Elapsed time {} is not less than the limit {}",
					elapsedTimeMicroSec, timeLimitMicroSec
				);
				return true;
			}
			if(stateItemsCount >= loadLimitCount) {
				LOG.debug(
					Markers.MSG, "Processed items count {} is not less than the limit {}",
					elapsedTimeMicroSec, timeLimitMicroSec
				);
				return true;
			}
		}
		return false;
	}
	//
	public static void saveRunState(final String runId, final List<LoadState> loadStates) {
		final String fullStateFileName = Paths
			.get(BasicConfig.getRootDir(), Constants.DIR_LOG, runId)
			.resolve(Constants.STATES_FILE)
			.toString();
		try(final FileOutputStream fos = new FileOutputStream(fullStateFileName, false)) {
			try(final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				synchronized(LoadCloseHook.LOAD_STATES_MAP) {
					oos.writeObject(loadStates);
				}
			}
			LOG.info(Markers.MSG, "Successfully saved state of run \"{}\"", runId);
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to save state of run \"{}\"", runId);
		}
	}
}
