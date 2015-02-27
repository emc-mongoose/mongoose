from __future__ import print_function, absolute_import, with_statement
#
from timeout import init as timeOutInit
from loadbuilder import init as loadBuilderInit
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.base.api import AsyncIOTask
from com.emc.mongoose.util.conf import RunTimeConfig
from com.emc.mongoose.util.logging import TraceLogger, Markers
from com.emc.mongoose.base.load import DataItemBuffer
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
			loadBuilder.setLoadType(AsyncIOTask.Type.valueOf(loadTypeStr.upper()))
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
				LOG.debug(Markers.MSG, "Starting next chain element: \"{}\"", nextLoad)
				nextLoad.start()
				if prevLoad is not None and isinstance(prevLoad, DataItemBuffer):
					prevLoad.close()
					prevLoad.start()
					try:
						prevLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
					except InterruptedException:
						pass
					except Throwable as e:
						TraceLogger.failure(
							LOG, Level.ERROR, e,
							String.format("Producer \"%s\" execution failure", prevLoad)
						)
					finally:
						prevLoad.interrupt()
				try:
					nextLoad.join(runTimeOut[1].toMillis(runTimeOut[0]))
				except InterruptedException as e:
					nextLoad.close()
					raise e
				except Throwable as e:
					TraceLogger.failure(
						LOG, Level.ERROR, e,
						String.format("Consumer \"%s\" execution failure", nextLoad)
					)
				finally:
					nextLoad.close()
			prevLoad = nextLoad
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
		loadTypesChain = RunTimeConfig.getStringArray("scenario.chain.load")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.load.chain\" specified")
	#
	flagSimultaneous, flagItemsBuffer = True, False
	try:
		flagSimultaneous = RunTimeConfig.getBoolean("scenario.chain.simultaneous")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.load.simultaneous\" specified")
	try:
		flagItemsBuffer = RunTimeConfig.getBoolean("scenario.chain.itemsbuffer")
	except:
		LOG.debug(Markers.MSG, "No \"scenario.load.itemsbuffer\" specified")
	#
	loadBuilder = loadBuilderInit()
	#
	chain = build(
		loadBuilder, loadTypesChain, flagSimultaneous, flagItemsBuffer,
		dataItemSizeMin if dataItemSize == 0 else dataItemSize,
		dataItemSizeMax if dataItemSize == 0 else dataItemSize,
		threadsPerNode
	)
	try:
		execute(chain, flagSimultaneous)
	except Throwable as e:
		TraceLogger.failure(LOG, Level.WARN, e, "Chain execution failure")
	#
	LOG.info(Markers.MSG, "Scenario end")
