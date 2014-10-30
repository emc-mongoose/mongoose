from __future__ import print_function, absolute_import, with_statement
#
import chain
#
from java.lang import Short, Throwable, NumberFormatException
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.conf import RunTimeConfig
from com.emc.mongoose.util.logging import ExceptionHandler, Markers
#
LOG = LogManager.getLogger()
#
listSizes = Main.RUN_TIME_CONFIG.getStringArray("scenario.rampup.sizes")
listThreadCounts = Main.RUN_TIME_CONFIG.getStringArray("scenario.rampup.thread.counts")
#
if __name__=="__builtin__":
	LOG.info(Markers.MSG, "Data sizes: {}", listSizes)
	LOG.info(Markers.MSG, "Thread counts: {}", listThreadCounts)
	for dataItemSizeStr in listSizes:
		try:
			dataItemSize = RunTimeConfig.toSize(dataItemSizeStr)
			LOG.info(Markers.MSG, "Next data item size: {}", dataItemSize)
			for threadCountStr in listThreadCounts:
				try:
					threadCount = Short.valueOf(threadCountStr)
				except NumberFormatException as e:
					LOG.error(Markers.ERR, "")
				try:
					LOG.info(Markers.MSG, "Next thread count: {}", threadCount)
					LOG.info(Markers.MSG, type(long(dataItemSize)))
					nextChain = chain.build(
						chain.FLAG_SIMULTANEOUS, dataItemSize, dataItemSize, threadCount
					)
					chain.execute(nextChain, chain.FLAG_SIMULTANEOUS)
				except Throwable as e:
					ExceptionHandler.trace(LOG, Level.ERROR, e, "Chain execution failure")
					e.printStackTrace()
		except Throwable as e:
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Determining the next data item size failure")
			e.printStackTrace()
	LOG.info(Markers.MSG, "Scenario end")
