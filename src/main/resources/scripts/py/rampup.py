from loadbuilder import init as loadBuilderInit
from chain import build as chainBuild
from chain import execute as chainExecute
#
from java.lang import Long, Integer, Throwable, NumberFormatException, InterruptedException
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
	listThreadCounts = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_RAMPUP_THREAD_COUNTS)
	LOG.info(Markers.MSG, "Thread counts: {}", listThreadCounts)
	return loadTypesChain, listSizes, listThreadCounts
#
def execute(loadBuilder, rampupParams=((),(),())):
	loadTypesChain = rampupParams[0]
	listSizes = rampupParams[1]
	listThreadCounts = rampupParams[2]
	for index, dataItemSizeStr in enumerate(listSizes):
		dataItemSize = Long(SizeUtil.toSize(dataItemSizeStr))
		interrupted = False
		for threadCountStr in listThreadCounts:
			if not interrupted:
				try:
					threadCount = Integer.parseInt(threadCountStr)
					LOG.info(Markers.PERF_SUM, "---- Step {}x{} start ----", threadCount, dataItemSizeStr)
					ThreadContext.put("currentSize", dataItemSizeStr + "-" + str(index))
					ThreadContext.put("currentThreadCount", str(threadCount))
					nextChain = chainBuild(
						loadBuilder, loadTypesChain, True, dataItemSize, dataItemSize, threadCount
					)
					chainExecute(nextChain, False)
					LOG.debug(Markers.MSG, "---- Step {}x{} finish ----", threadCount, dataItemSizeStr)
				except InterruptedException:
					interrupted = True
				except NumberFormatException as e:
					LogUtil.exception(Markers.ERR, Level.WARN, e, "Failed to parse the next thread count")
#
if __name__ == "__builtin__":
	loadBuilder = loadBuilderInit()
	try:
		execute(loadBuilder=loadBuilder, rampupParams=init())
	except InterruptedException as e:
		LOG.debug(Markers.MSG, "Rampup was interrupted")
	except Throwable as e:
		LogUtil.exception(LOG, Level.ERROR, e, "Scenario failed")
	loadBuilder.close()
	LOG.info(Markers.MSG, "Scenario end")
