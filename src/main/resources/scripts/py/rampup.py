from loadbuilder import init as loadBuilderInit
from timeout import init as timeOutInit
from chain import build as chainBuild
from chain import execute as chainExecute
#
from java.lang import Long, Integer, Throwable, NumberFormatException, InterruptedException
from java.util.concurrent import TimeUnit
#
from org.apache.logging.log4j import Level, LogManager, ThreadContext
#
from com.emc.mongoose.common.conf import RunTimeConfig, SizeUtil
from com.emc.mongoose.common.log import LogUtil, Markers
#
LOG = LogManager.getLogger()
#
def init():
	runTimeConfig = RunTimeConfig.getContext()
	LOG.debug(Markers.MSG, "Setting the metric update period to zero for chain scenario")
	runTimeConfig.set(RunTimeConfig.KEY_LOAD_METRICS_PERIOD_SEC, 0)
	#
	loadTypesChain = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD)
	LOG.info(Markers.MSG, "Load types chain: {}", loadTypesChain)
	listSizes = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_RAMPUP_SIZES)
	LOG.info(Markers.MSG, "Data sizes: {}", listSizes)
	listConnCounts = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_RAMPUP_CONN_COUNTS)
	LOG.info(Markers.MSG, "Thread counts: {}", listConnCounts)
	return loadTypesChain, listSizes, listConnCounts
#
def execute(loadBuilder, rampupParams=((),(),()), timeOut=Long.MAX_VALUE, timeUnit=TimeUnit.DAYS):
	loadTypesChain = rampupParams[0]
	listSizes = rampupParams[1]
	listConnCounts = rampupParams[2]
	flagConcurrent = False
	for index, dataItemSizeStr in enumerate(listSizes):
		dataItemSize = Long(SizeUtil.toSize(dataItemSizeStr))
		for connCountStr in listConnCounts:
			try:
				connCount = Integer.parseInt(connCountStr)
				LOG.info(Markers.PERF_SUM, "---- Step {}x{} start ----", connCount, dataItemSizeStr)
				ThreadContext.put("currentSize", dataItemSizeStr + "-" + str(index))
				ThreadContext.put("currentConnCount", str(connCount))
				nextChain = chainBuild(
					loadBuilder, loadTypesChain, flagConcurrent, True, dataItemSize, dataItemSize,
					connCount
				)
				chainExecute(nextChain, flagConcurrent, timeOut, timeUnit)
				LOG.debug(Markers.MSG, "---- Step {}x{} finish ----", connCount, dataItemSizeStr)
			except InterruptedException as e:
				raise e
			except NumberFormatException as e:
				LogUtil.exception(Markers.ERR, Level.WARN, e, "Failed to parse the next thread count")
#
if __name__ == "__builtin__":
	loadBuilder = loadBuilderInit()
	stepTime = timeOutInit()
	try:
		execute(loadBuilder=loadBuilder, rampupParams=init(), timeOut=stepTime[0], timeUnit=stepTime[1])
	except InterruptedException as e:
		LOG.debug(Markers.MSG, "Rampup was interrupted")
	except Throwable as e:
		LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed")
	loadBuilder.close()
	LOG.info(Markers.MSG, "Scenario end")
