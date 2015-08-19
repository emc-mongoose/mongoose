from timeout import init as timeOutInit
from loadbuilder import init as loadBuilderInit
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.common.concurrent import GroupThreadFactory
from com.emc.mongoose.common.conf import RunTimeConfig
from com.emc.mongoose.common.log import LogUtil, Markers
#
from com.emc.mongoose.core.api.io.task import IOTask
#
from com.emc.mongoose.core.impl.load.tasks import AwaitLoadJobTask
#
from java.lang import Long, String, Throwable, IllegalArgumentException, InterruptedException
from java.util.concurrent import Executors, TimeUnit
#
LOG = LogManager.getLogger()
#
def build(
	loadBuilder, loadTypesChain, flagConcurrent=True, flagUseLocalItemList=True,
	dataItemSizeMin=0, dataItemSizeMax=0, threadsPerNode=0
):
	#
	chain = list()
	prevLoad = None
	for loadTypeStr in loadTypesChain:
		LOG.debug(Markers.MSG, "Next load type is \"{}\"", loadTypeStr)
		try:
			loadBuilder.setLoadType(IOTask.Type.valueOf(loadTypeStr.upper()))
			if dataItemSizeMin > 0:
				loadBuilder.setMinObjSize(dataItemSizeMin)
			if dataItemSizeMax > 0:
				loadBuilder.setMaxObjSize(dataItemSizeMax)
			if threadsPerNode > 0:
				loadBuilder.setThreadsPerNodeDefault(threadsPerNode)
			load = loadBuilder.build()
			#
			if load is not None:
				if prevLoad is not None:
					prevLoad.setConsumer(load)
				chain.append(load)
			else:
				LOG.error(Markers.ERR, "No load executor instanced")
			if prevLoad is None:
				# prevent any item source creation for next loads
				loadBuilder.setInputFile(None)
				if flagConcurrent or flagUseLocalItemList:
					loadBuilder.getRequestConfig().setContainerInputEnabled(False)
			prevLoad = load
		except IllegalArgumentException as e:
			LogUtil.exception(
				LOG, Level.ERROR, e, String.format("Wrong load type \"%s\", skipping", loadTypeStr)
			)
		except Throwable as e:
			LogUtil.exception(LOG, Level.FATAL, e, "Unexpected failure")
	return chain
	#
def execute(chain=(), flagConcurrent=True, timeOut=Long.MAX_VALUE, timeUnit=TimeUnit.DAYS):
	try:
		if flagConcurrent:
			LOG.info(Markers.MSG, "Execute load jobs in parallel")
			for load in reversed(chain):
				load.start()
			chainWaitExecSvc = Executors.newFixedThreadPool(
				len(chain), GroupThreadFactory("chainFinishAwait")
			)
			for load in chain:
				chainWaitExecSvc.submit(
					AwaitLoadJobTask(load, timeOut, timeUnit)
				)
			chainWaitExecSvc.shutdown()
			try:
				if chainWaitExecSvc.awaitTermination(timeOut, timeUnit):
					LOG.debug(Markers.MSG, "Load jobs are finished in time")
			finally:
				LOG.debug(
					Markers.MSG, "{} load jobs are not finished in time",
					chainWaitExecSvc.shutdownNow().size()
				)
				for load in chain:
					load.close()
		else:
			LOG.info(Markers.MSG, "Execute load jobs sequentially")
			interrupted = False
			for nextLoad in chain:
				if not interrupted:
					try:
						LOG.debug(Markers.MSG, "Starting next load job: \"{}\"", nextLoad)
						nextLoad.start()
						try:
							LOG.debug(
								Markers.MSG, "Execute \"{}\" for up to {}[{}]",
								nextLoad, timeOut, timeUnit
							)
							nextLoad.await(timeOut, timeUnit)
						except InterruptedException as e:
							LOG.debug("{}: interrupted", nextLoad)
							interrupted = True
							raise e
						finally:
							LOG.debug(Markers.MSG, "Load job \"{}\" done", nextLoad)
					finally:
						nextLoad.close()
						LOG.debug(Markers.MSG, "Load job \"{}\" closed", nextLoad)
	finally:
		for loadJob in chain:
			del loadJob
		del chain
#
if __name__ == "__builtin__":
	#
	dataItemSize, dataItemSizeMin, dataItemSizeMax, threadsPerNode = 0, 0, 0, 0
	runTimeConfig = RunTimeConfig.getContext()
	#
	try:
		dataItemSize = Long(runTimeConfig.getSizeBytes(RunTimeConfig.KEY_DATA_SIZE))
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		dataItemSizeMin = runTimeConfig.getDataSizeMin()
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		dataItemSizeMax = runTimeConfig.getDataSizeMax()
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		threadsPerNode = Long(runTimeConfig.getInt(RunTimeConfig.KEY_LOAD_THREADS))
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_LOAD_THREADS)
	#
	loadTypesChain = ()
	try:
		loadTypesChain = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD)
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD)
	#
	flagConcurrent, flagUseLocalItemList = True, True
	try:
		flagConcurrent = runTimeConfig.getBoolean(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT)
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT)
	try:
		flagUseLocalItemList = runTimeConfig.getBoolean(RunTimeConfig.KEY_SCENARIO_CHAIN_ITEMSBUFFER)
	except:
		LOG.debug(Markers.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_ITEMSBUFFER)
	#
	loadBuilder = loadBuilderInit()
	runTime = timeOutInit()
	#
	chain = build(
		loadBuilder, loadTypesChain, flagConcurrent, flagUseLocalItemList,
		dataItemSizeMin if dataItemSize == 0 else dataItemSize,
		dataItemSizeMax if dataItemSize == 0 else dataItemSize,
		threadsPerNode
	)
	if chain is None or len(chain) == 0:
		LOG.error(Markers.ERR, "Empty chain has been build, nothing to do")
	else:
		try:
			execute(chain, flagConcurrent, runTime[0], runTime[1])
		except InterruptedException as e:
			LOG.debug(Markers.MSG, "Chain was interrupted")
		except Throwable as e:
			LogUtil.exception(LOG, Level.WARN, e, "Chain execution failure")
	#
	loadBuilder.close()
	#
	LOG.info(Markers.MSG, "Scenario end")
