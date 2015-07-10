package com.emc.mongoose.integ.integTestTools;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by olga on 03.07.15.
 */
public final class IntegLogManager {

	//Patterns
	private static final Pattern DATA_ITEMS_FILE_PATTERN = Pattern.compile("^[a-zA-Z0-9]+,[a-zA-Z0-9]+,[0-9]+,[0-9]+/[0-9a-fA-F]+$");
	private static final Pattern PERF_SUM_FILE_PATTERN = Pattern.compile(
		//data and time pattern
		"^\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\"," +
		//load ID, type API, load type and connection count pattern
		"[0-9]+,(S3|ATMOS|SWIFT),(Create|Read|Update|Append|Delete),[0-9]+," +
		//node count, load servers count, count of success and count af fail pattern
		"[0-9]+,[0-9]*+,[0-9]+,[0-9]+," +
		//LatencyAvg[us], LatencyMin[us],LatencyMed[us],LatencyMax[us] pattern
		"[0-9]+,[0-9]+,[0-9]+,[0-9]+," +
		//TPAvg,TP1Min,TP5Min,TP15Min pattern
		"[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3}," +
		//BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s] pattern
		"[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3}$");
	private static final Pattern PERF_AVG_FILE_PATTERN = Pattern.compile(
		//data and time pattern
		"^\"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}\"," +
		//load ID, type API, load type and connection count pattern
		"[0-9]+,(S3|ATMOS|SWIFT),(Create|Read|Update|Append|Delete),[0-9]+," +
		//node count, load servers count, count of success, count of pending and count af fail pattern
		"[0-9]+,[0-9]*+,[0-9]+,[0-9]+,[0-9]+," +
		//LatencyAvg[us], LatencyMin[us],LatencyMed[us],LatencyMax[us] pattern
		"[0-9]+,[0-9]+,[0-9]+,[0-9]+," +
		//TPAvg,TP1Min,TP5Min,TP15Min pattern
		"[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3}," +
		//BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s] pattern
		"[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3},[0-9]+\\.[0-9]{3}$");
	private static final Pattern PERF_TRACE_FILE_PATTERN = Pattern.compile(
		// thread name pattern
		"^[0-9]+-(S3|ATMOS|SWIFT)-(Create|Read|Update|Append|Delete)([0-9]+)?+-[0-9]+x[0-9]+#[0-9]+," +
		// TargetNode pattern
		"[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}:[0-9]{1,5}," +
		// DataItemId,DataItemSize pattern
		"[a-zA-Z0-9]+,[0-9]+," +
		// StatusCode,ReqTimeStart[us],Latency[us],Duration[us]
		"[0-9]+,[0-9]+,[0-9]+,[0-9]+$");

	//Headers
	public static final String HEADER_PERF_TRACE_FILE = "Thread,TargetNode,DataItemId,DataItemSize,StatusCode," +
		"ReqTimeStart[us],Latency[us],Duration[us]";
	public static final String HEADER_PERF_AVG_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode," +
		"CountLoadServer,CountSucc,CountPending,CountFail,LatencyAvg[us],LatencyMin[us],LatencyMed[us]," +
		"LatencyMax[us],TPAvg,TP1Min,TP5Min,TP15Min,BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s]";
	public static final String HEADER_PERF_SUM_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode," +
		"CountLoadServer,CountSucc,CountFail,LatencyAvg[us],LatencyMin[us],LatencyMed[us]," +
		"LatencyMax[us],TPAvg,TP1Min,TP5Min,TP15Min,BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s]";

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

	public static File getErrorsFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, IntegConstants.ERR_FILE_NAME).toString());
	}

	public static boolean matchWithDataItemsFilePattern(final String line) {
		final Matcher matcher = DATA_ITEMS_FILE_PATTERN.matcher(line);
		return matcher.find();
	}

	public static boolean matchWithPerfSumFilePattern(final String line) {
		final Matcher matcher = PERF_SUM_FILE_PATTERN.matcher(line);
		return matcher.find();
	}

	public static boolean matchWithPerfAvgFilePattern(final String line) {
		final Matcher matcher = PERF_AVG_FILE_PATTERN.matcher(line);
		return matcher.find();
	}

	public static boolean matchWithPerfTraceFilePattern(final String line) {
		final Matcher matcher = PERF_TRACE_FILE_PATTERN.matcher(line);
		return matcher.find();
	}

	public static void waitLogger()
	throws InterruptedException {
		if(LogUtil.LOAD_HOOKS_COUNT.get() != 0) {

			LogUtil.HOOKS_LOCK.tryLock(10, TimeUnit.SECONDS);

			try {
				LogUtil.HOOKS_COND.await(10, TimeUnit.SECONDS);
			} finally {
				LogUtil.HOOKS_LOCK.unlock();
			}
		}
	}
}
