from __future__ import print_function, absolute_import, with_statement
from sys import exit
from loadbuilder import INSTANCE as loadBuilder
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.base.api import Request
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.logging import ExceptionHandler, Markers
#
from java.lang import Throwable, IllegalArgumentException
from java.util import NoSuchElementException
#
LOG = LogManager.getLogger()
chain = list()
#
loadTypes = None
try:
	loadTypes = Main.RUN_TIME_CONFIG.getStringArray("scenario.chain.load")
	LOG.info(Markers.MSG, "Load chain: {}", loadTypes)
except NoSuchElementException:
	LOG.error(Markers.ERR, "No load type specified, try arg -Dscenario.chain.load=<VALUE> to override")
#
prevLoad = None
for loadTypeStr in loadTypes:
	LOG.debug(Markers.MSG, "Next load type is \"{}\"", loadTypeStr)
	try:
		load = loadBuilder.setLoadType(Request.Type.valueOf(loadTypeStr.upper())).build()
		if prevLoad is not None:
			prevLoad.setConsumer(load)
		if load is not None:
			chain.append(load)
		else:
			LOG.error(Markers.ERR, "No load executor instanced")
		if prevLoad is None:
			loadBuilder.setInputFile(None) # prevent the file list producer creation for next loads
		prevLoad = load
	except IllegalArgumentException:
		LOG.error(Markers.ERR, "Wrong load type \"{}\", skipping", loadTypeStr)
	except Throwable as e:
		ExceptionHandler.trace(LOG, Level.FATAL, e, "Unexpected failure")
#
from java.lang import Integer
from java.util.concurrent import TimeUnit
timeOut = None  # tuple of (value, unit)
try:
	timeOut = Main.RUN_TIME_CONFIG.getRunTime()
	timeOut = timeOut.split('.')
	timeOut = Integer.valueOf(timeOut[0]), TimeUnit.valueOf(timeOut[1].upper())
	LOG.info(Markers.MSG, "Using time limit: {} {}", timeOut[0], timeOut[1].name().lower())
except NoSuchElementException:
	LOG.error(Markers.ERR, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override")
except IllegalArgumentException:
	LOG.error(Markers.ERR, "Timeout unit should be a name of a constant from TimeUnit enumeration")
	exit()
except IndexError:
	LOG.error(Markers.ERR, "Time unit should be specified with timeout value (following after \".\" separator)")
	exit()
#
for load in chain:
	load.start()
# noinspection PyBroadException
try:
	chain[0].join(timeOut[1].toMillis(timeOut[0]))
except:
	LOG.error(Markers.ERR, "No 1st load executor in the chain")
for load in chain:
	load.close()
LOG.info(Markers.MSG, "Scenario end")
