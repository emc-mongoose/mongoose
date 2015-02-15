from __future__ import print_function, absolute_import, with_statement
from sys import exit
from timeout import timeout_init
from loadbuilder import loadbuilder_init
#from loadbuilder import INSTANCE as LOAD_BUILDER
#from timeout import INSTANCE as RUN_TIME
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.base.api import AsyncIOTask
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.logging import TraceLogger, Markers
from com.emc.mongoose.base.load import DataItemBuffer
#
from java.lang import Long, Throwable, IllegalArgumentException
from java.util import NoSuchElementException
#
RUN_TIME = timeout_init()
LOCAL_RUN_TIME_CONFIG = Main.RUN_TIME_CONFIG.get()
LOAD_BUILDER = loadbuilder_init()
LOG = LogManager.getLogger()
#
LOAD_CHAIN = None
try:
	LOAD_CHAIN = LOCAL_RUN_TIME_CONFIG.getStringArray("scenario.chain.load")
except NoSuchElementException:
	LOG.error(Markers.ERR, "No load type specified, try arg -Dscenario.chain.load=<VALUE> to override")
#
FLAG_SIMULTANEOUS = False
try:
	FLAG_SIMULTANEOUS = LOCAL_RUN_TIME_CONFIG.getBoolean("scenario.chain.simultaneous")
except NoSuchElementException:
	LOG.error(Markers.ERR, "No chain simultaneous flag specified, try arg -Dscenario.chain.simultaneous=<VALUE> to override")
LOG.info(
	Markers.MSG,
	("Simultaneous" if FLAG_SIMULTANEOUS else "Sequential") + " load chain: {}",
	LOAD_CHAIN
)
#
FLAG_USE_ITEMS_BUFFER = True
try:
	FLAG_USE_ITEMS_BUFFER = LOCAL_RUN_TIME_CONFIG.getBoolean("scenario.chain.itemsbuffer")
except NoSuchElementException:
	LOG.error(Markers.ERR, "No items buffer flag specified, try arg -Dscenario.chain.itemsbuffer=<VALUE> to override")
if FLAG_USE_ITEMS_BUFFER:
	LOG.info(Markers.MSG, "Going to use persistent internal items buffer")
#
def build(flagSimultaneous=True, flagItemsBuffer=True, dataItemSizeMin=0, dataItemSizeMax=0, threadsPerNode=0):
	#
	if flagItemsBuffer:
		LOAD_BUILDER.getRequestConfig().setAnyDataProducerEnabled(False)
	#
	chain = list()
	prevLoad = None
	for loadTypeStr in LOAD_CHAIN:
		LOG.debug(Markers.MSG, "Next load type is \"{}\"", loadTypeStr)
		try:
			LOAD_BUILDER.setLoadType(AsyncIOTask.Type.valueOf(loadTypeStr.upper()))
			if dataItemSizeMin > 0:
				LOAD_BUILDER.setMinObjSize(dataItemSizeMin)
			if dataItemSizeMax > 0:
				LOAD_BUILDER.setMaxObjSize(dataItemSizeMax)
			if threadsPerNode > 0:
				LOAD_BUILDER.setThreadsPerNodeDefault(threadsPerNode)
			load = LOAD_BUILDER.build()
			#
			if load is not None:
				if flagSimultaneous:
					if prevLoad is not None:
						prevLoad.setConsumer(load)
					chain.append(load)
				else:
					if prevLoad is not None:
						if flagItemsBuffer:
							mediatorBuff = LOAD_BUILDER.newDataItemBuffer()
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
				LOAD_BUILDER.setInputFile(None) # prevent the file list producer creation for next loads
			prevLoad = load
		except IllegalArgumentException:
			LOG.error(Markers.ERR, "Wrong load type \"{}\", skipping", loadTypeStr)
		except Throwable as e:
			TraceLogger.failure(LOG, Level.FATAL, e, "Unexpected failure")
	return chain
	#
def execute(chain=(), flagSimultaneous=True):
	if flagSimultaneous:
		for load in chain:
			load.start()
		for load in chain:
			try:
				load.join(RUN_TIME[1].toMillis(RUN_TIME[0]))
			finally:
				load.close()
	else:
		prevLoad, nextLoad = None, None
		for nextLoad in chain:
			if not isinstance(nextLoad, DataItemBuffer):
				nextLoad.start()
				if prevLoad is not None and isinstance(prevLoad, DataItemBuffer):
					prevLoad.close()
					prevLoad.start()
					try:
						prevLoad.join(RUN_TIME[1].toMillis(RUN_TIME[0]))
					except Throwable as e:
						TraceLogger.failure(
							LOG, Level.ERROR, e, "Producer \"{}\" execution failure", prevLoad
						)
					finally:
						prevLoad.interrupt()
				try:
					nextLoad.join(RUN_TIME[1].toMillis(RUN_TIME[0]))
				except Throwable as e:
					TraceLogger.failure(
						LOG, Level.ERROR, e, "Consumer \"{}\" execution failure", nextLoad
					)
				finally:
					nextLoad.close()
			prevLoad = nextLoad
#
if __name__=="__builtin__":
	#
	dataItemSize, dataItemSizeMin, dataItemSizeMax, threadsPerNode = 0, 0, 0, 0
	#
	try:
		dataItemSize = Long(LOCAL_RUN_TIME_CONFIG.getSizeBytes("data.size"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		dataItemSizeMin = Long(LOCAL_RUN_TIME_CONFIG.getSizeBytes("data.size.min"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		dataItemSizeMax = Long(LOCAL_RUN_TIME_CONFIG.getSizeBytes("data.size.max"))
	except:
		LOG.debug(Markers.MSG, "No \"data.size\" specified")
	try:
		threadsPerNode = Long(LOCAL_RUN_TIME_CONFIG.getShort("load.threads"))
	except:
		LOG.debug(Markers.MSG, "No \"load.threads\" specified")
	#
	chain = build(
		FLAG_SIMULTANEOUS, FLAG_USE_ITEMS_BUFFER,
		dataItemSizeMin if dataItemSize == 0 else dataItemSize,
		dataItemSizeMax if dataItemSize == 0 else dataItemSize,
		threadsPerNode
	)
	try:
		execute(chain, FLAG_SIMULTANEOUS)
	except Throwable as e:
		TraceLogger.failure(LOG, Level.WARN, e, "Chain execution failure")
	#
	LOG.info(Markers.MSG, "Scenario end")
