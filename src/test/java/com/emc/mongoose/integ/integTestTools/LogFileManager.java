package com.emc.mongoose.integ.integTestTools;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;

import java.io.File;
import java.nio.file.Paths;

/**
 * Created by olga on 03.07.15.
 */
public final class LogFileManager {

	public static File getMessageFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.MESSAGE_FILE_NAME).toString());
	}

	public static File getPerfAvgFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.PERF_AVG_FILE_NAME).toString());
	}

	public static File getPerfSumFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.PERF_SUM_FILE_NAME).toString());
	}

	public static File getPerfTraceFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.PERF_TRACE_FILE_NAME).toString());
	}

	public static File getDataItemsFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.DATA_ITEMS_FILE_NAME).toString());
	}
}
