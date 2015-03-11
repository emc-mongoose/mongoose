from __future__ import print_function, absolute_import, with_statement
#
from timeout import init as timeOutInit
from loadbuilder import init as loadBuilderInit
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.core.api.io.task import IOTask
from com.emc.mongoose.core.api.persist import Markers
from com.emc.mongoose.core.api.persist import DataItemBuffer
from com.emc.mongoose.core.impl.persist import TraceLogger
from com.emc.mongoose.core.impl.util import RunTimeConfig
#
from java.lang import Long, String, Throwable, IllegalArgumentException, InterruptedException
#
LOG = LogManager.getLogger()
#
def build(
	loadBuilder, loadTypesChain, flagSimultaneous=True, flagItemsBuffer=True,
	dataItemSizeMin=0, dataItemSizeMax=0, threadsPerNode=0
):
	#
	if flagItemsBuffer:
		loadBuilder.getRequestConfig().setAnyDataProducerEnabled(False)
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
				if flagSimultaneous:
					if prevLoad is not None:
						prevLoad.setConsumer(load)
					chain.append(load)
				else:
					if prevLoad is not None:
						if flagItemsBuffer:
							mediatorBuff = loadBuilder.newDataItemBuffer()
							if mediatorBuff is not None:
								prevLoad.setConsumer(mediatorBuff)
								chain.append(mediatorBuff)
								mediatorBuff.setConsumer(load)
							else:
								LOG.error(Markers.ERR, "No mediator buffer instanced")
						else:
							prevLoad.setConsumer(load)
					chain.append(load)
			else:
				LOG.error(Markers.ERR, "No load executor instanced")
			if prevLoad is None:
				loadBuilder.setInputFile(None) # prevent the file list producer creation for next loads
			prevLoad = load
		except IllegalArgumentException as e:
			e.printStackTrace()
			LOG.error(Markers.ERR, "Wrong load type \"{}\", skipping", loadTypeStr)
		except Throwable as e:
			TraceLogger.failure(LOG, Level.FATAL, e, "Unexpected failure")
	return chain
	#
def execute(chain=(), flagSimultaneous=True):
	runTimeOut = timeOutInit()
	try:
		if flagSimultaneous:
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
			prevLoad, nextLoad = None, None
			for nextLoad in chain:
				if not isinstance(nextLoad, DataItemBuffer):
					LOG.debug(Markers.MSG, "Starting next load job: \"{}\"", nextLoad)
					nextLoad.start()
					if prevLoad is not None and isinstance(prevLoad, DataItemBuffer):
						LOG.debug(Markers.MSG, "Stop buffering the data items into \"{}\"", prevLoad)
						prevLoad.close()
						LOG.debug(Markers.MSG, "Start producing the data items from \"{}\"", prevLoad)
						prevLoad.start()
						try:
							nextLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
						except InterruptedException as e:
							raise e
						except Throwable as e:
							TraceLogger.failure(
								LOG, Level.ERROR, e,
								String.format("Producer \"%s\" execution failure", prevLoad)
							)
						finally:
							LOG.debug(Markers.MSG, "Load job \"{}\" done", nextLoad)
							prevLoad.interrupt()
							LOG.debug(Markers.MSG, "Stop producing the data items from \"{}\"", prevLoad)
							nextLoad.close()
							LOG.debug(Markers.MSG, "Load job \"{}\" closed", nextLoad)
					else:
						try:
							nextLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
						except InterruptedException as e:
							raise e
						except Throwable as e:
							TraceLogger.failure(
								LOG, Level.ERROR, e,
								String.format("Consumer \"%s\" execution failure", nextLoad)
							)
						finally:
							LOG.debug(Markers.MSG, "Load job \"{}\" done", nextLoad)
							nextLoad.close()
							LOG.debug(Markers.MSG, "Load job \"{}\" closed", nextLoad)
				prevLoad = nextLoad
	finally:
		if chain is not None:
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
		dataItemSize = Long(runTimeConfig.getSizeBytes("data.size"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		dataItemSizeMin = Long(runTimeConfig.getSizeBytes("data.size.min"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		dataItemSizeMax = Long(runTimeConfig.getSizeBytes("data.size.max"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		threadsPerNode = Long(runTimeConfig.getShort("load.threads"))
	except:
		LOG.debug(Markers.MSG, "No \"load.threads\" specified")
	#
	loadTypesChain = ()
	try:
		loadTypesChain = runTimeConfig.getStringArray("scenario.chain.load")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.chain.load\" specified")
	#
	flagSimultaneous, flagItemsBuffer = True, False
	try:
		flagSimultaneous = runTimeConfig.getBoolean("scenario.chain.simultaneous")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.chain.simultaneous\" specified")
	try:
		flagItemsBuffer = runTimeConfig.getBoolean("scenario.chain.itemsbuffer")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.chain.itemsbuffer\" specified")
	#
	loadBuilder = loadBuilderInit()
	loadBuilder.getRequestConfig().setAnyDataProducerEnabled(False)
	#
	chain = build(
		loadBuilder, loadTypesChain, flagSimultaneous, flagItemsBuffer,
		dataItemSizeMin if dataItemSize == 0 else dataItemSize,
		dataItemSizeMax if dataItemSize == 0 else dataItemSize,
		threadsPerNode
	)
	if chain is None or len(chain) == 0:
		LOG.error(Markers.ERR, "Empty chain has been build, nothing to do")
	else:
		try:
			execute(chain, flagSimultaneous)
		except Throwable as e:
			TraceLogger.failure(LOG, Level.WARN, e, "Chain execution failure")
	#
	LOG.info(Markers.MSG, "Scenario end")
