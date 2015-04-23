from timeout import init as timeOutInit
from loadbuilder import init as loadBuilderInit
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.common.conf import RunTimeConfig
from com.emc.mongoose.common.logging import LogUtil
#
from com.emc.mongoose.core.api.io.task import IOTask
from com.emc.mongoose.core.api.persist import DataItemBuffer
#
from java.lang import Long, String, Throwable, IllegalArgumentException, InterruptedException
#
LOG = LogManager.getLogger()
#
def build(
	loadBuilder, loadTypesChain, flagConcurrent=True, flagUseItemsBuffer=True,
	dataItemSizeMin=0, dataItemSizeMax=0, threadsPerNode=0
):
	#
	if flagUseItemsBuffer:
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(False)
	#
	chain = list()
	prevLoad = None
	for loadTypeStr in loadTypesChain:
		LOG.debug(LogUtil.MSG, "Next load type is \"{}\"", loadTypeStr)
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
				if flagConcurrent:
					if prevLoad is not None:
						prevLoad.setConsumer(load)
					chain.append(load)
				else:
					if prevLoad is not None:
						if flagUseItemsBuffer:
							mediatorBuff = loadBuilder.newDataItemBuffer()
							if mediatorBuff is not None:
								prevLoad.setConsumer(mediatorBuff)
								chain.append(mediatorBuff)
								mediatorBuff.setConsumer(load)
							else:
								LOG.error(LogUtil.ERR, "No mediator buffer instanced")
						else:
							prevLoad.setConsumer(load)
					chain.append(load)
			else:
				LOG.error(LogUtil.ERR, "No load executor instanced")
			if prevLoad is None:
				loadBuilder.setInputFile(None) # prevent the file list producer creation for next loads
			prevLoad = load
		except IllegalArgumentException as e:
			LogUtil.failure(
				LOG, Level.ERROR, e, String.format("Wrong load type \"%s\", skipping", loadTypeStr)
			)
		except Throwable as e:
			LogUtil.failure(LOG, Level.FATAL, e, "Unexpected failure")
	return chain
	#
def execute(chain=(), flagSimultaneous=True):
	runTimeOut = timeOutInit()
	try:
		if flagSimultaneous:
			LOG.info(LogUtil.MSG, "Execute load jobs in parallel")
			for load in reversed(chain):
				load.start()
			for load in chain:
				try:
					load.join(runTimeOut[1].toMillis(runTimeOut[0]))
				except InterruptedException:
					pass
				finally:
					load.close()
		else:
			LOG.info(LogUtil.MSG, "Execute load jobs sequentially")
			prevLoad, nextLoad = None, None
			for nextLoad in chain:
				if not isinstance(nextLoad, DataItemBuffer):
					LOG.debug(LogUtil.MSG, "Starting next load job: \"{}\"", nextLoad)
					nextLoad.start()
					if prevLoad is not None and isinstance(prevLoad, DataItemBuffer):
						LOG.debug(LogUtil.MSG, "Stop buffering the data items into \"{}\"", prevLoad)
						prevLoad.close()
						LOG.debug(LogUtil.MSG, "Start producing the data items from \"{}\"", prevLoad)
						prevLoad.start()
						try:
							LOG.debug(
								LogUtil.MSG, "Execute \"{}\" for up to {}[{}]",
								nextLoad, runTimeOut[0], runTimeOut[1]
							)
							nextLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
						except InterruptedException as e:
							raise e
						except Throwable as e:
							LogUtil.failure(
								LOG, Level.ERROR, e,
								String.format("Producer \"%s\" execution failure", prevLoad)
							)
						finally:
							LOG.debug(LogUtil.MSG, "Load job \"{}\" done", nextLoad)
							prevLoad.interrupt()
							LOG.debug(LogUtil.MSG, "Stop producing the data items from \"{}\"", prevLoad)
							nextLoad.close()
							LOG.debug(LogUtil.MSG, "Load job \"{}\" closed", nextLoad)
					else:
						try:
							LOG.debug(
								LogUtil.MSG, "Execute \"{}\" for up to {}[{}]",
								nextLoad, runTimeOut[0], runTimeOut[1]
							)
							nextLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
						except InterruptedException as e:
							raise e
						except Throwable as e:
							LogUtil.failure(
								LOG, Level.ERROR, e,
								String.format("Consumer \"%s\" execution failure", nextLoad)
							)
						finally:
							LOG.debug(LogUtil.MSG, "Load job \"{}\" done", nextLoad)
							nextLoad.close()
							LOG.debug(LogUtil.MSG, "Load job \"{}\" closed", nextLoad)
				prevLoad = nextLoad
	finally:
		if chain is not None:
			for loadJob in chain:
				del loadJob
			#del chain
#
if __name__ == "__builtin__":
	#
	dataItemSize, dataItemSizeMin, dataItemSizeMax, threadsPerNode = 0, 0, 0, 0
	runTimeConfig = RunTimeConfig.getContext()
	#
	try:
		dataItemSize = Long(runTimeConfig.getSizeBytes(RunTimeConfig.KEY_DATA_SIZE))
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		dataItemSizeMin = runTimeConfig.getDataSizeMin()
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		dataItemSizeMax = runTimeConfig.getDataSizeMax()
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_DATA_SIZE)
	try:
		threadsPerNode = Long(runTimeConfig.getShort(RunTimeConfig.KEY_LOAD_THREADS))
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_LOAD_THREADS)
	#
	loadTypesChain = ()
	try:
		loadTypesChain = runTimeConfig.getStringArray(RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD)
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_LOAD)
	#
	flagConcurrent, flagItemsBuffer = True, True
	try:
		flagConcurrent = runTimeConfig.getBoolean(RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT)
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_CONCURRENT)
	try:
		flagItemsBuffer = runTimeConfig.getBoolean(RunTimeConfig.KEY_SCENARIO_CHAIN_ITEMSBUFFER)
	except:
		LOG.debug(LogUtil.MSG, "No \"{}\" specified", RunTimeConfig.KEY_SCENARIO_CHAIN_ITEMSBUFFER)
	#
	loadBuilder = loadBuilderInit()
	loadBuilder.getRequestConfig().setAnyDataProducerEnabled(False)
	#
	chain = build(
		loadBuilder, loadTypesChain, flagConcurrent, flagItemsBuffer,
		dataItemSizeMin if dataItemSize == 0 else dataItemSize,
		dataItemSizeMax if dataItemSize == 0 else dataItemSize,
		threadsPerNode
	)
	if chain is None or len(chain) == 0:
		LOG.error(LogUtil.ERR, "Empty chain has been build, nothing to do")
	else:
		try:
			execute(chain, flagConcurrent)
		except Throwable as e:
			LogUtil.failure(LOG, Level.WARN, e, "Chain execution failure")
	#
	loadBuilder.close()
	#
	LOG.info(LogUtil.MSG, "Scenario end")
