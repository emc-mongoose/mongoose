#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
from sys import exit
#
from loadbuilder import loadbuilder_init
#from loadbuilder import INSTANCE as LOAD_BUILDER
#
from com.emc.mongoose.base.api import AsyncIOTask
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.logging import Markers
#
from org.apache.logging.log4j import LogManager
#
from java.lang import IllegalArgumentException
from java.util import NoSuchElementException
#
LOAD_BUILDER = loadbuilder_init()
LOG = LogManager.getLogger()
#
try:
	loadType = AsyncIOTask.Type.valueOf(Main.RUN_TIME_CONFIG.get().getString("scenario.rampup-create.load").upper())
	LOG.info(Markers.MSG, "Using load type: {}", loadType.name())
	LOAD_BUILDER.setLoadType(loadType)
except NoSuchElementException:
	LOG.info(Markers.MSG, "No load type specified, try arg -Dscenario.rampup-create.load=<VALUE> to override")
except IllegalArgumentException:
	LOG.fatal(Markers.ERR, "No such load type, it should be a constant from Load.Type enumeration")
	exit()
#
from java.lang import Integer
from java.util.concurrent import TimeUnit
timeOut = None  # tuple of (value, unit)
try:
	timeOut = Main.RUN_TIME_CONFIG.get().getRunTime()
	timeOut = timeOut.split('.')
	timeOut = Integer.valueOf(timeOut[0]), TimeUnit.valueOf(timeOut[1].upper())
	LOG.info(Markers.MSG, "Using time limit: {} {}", timeOut[0], timeOut[1].name().lower())
except NoSuchElementException:
	LOG.info(Markers.MSG, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override")
except IllegalArgumentException:
	LOG.error(Markers.ERR, "Timeout unit should be a name of a constant from TimeUnit enumeration")
	exit()
except IndexError:
	LOG.error(Markers.ERR, "Time unit should be specified with timeout value (following after \".\" separator)")
	exit()
#
threadCountList = Main.RUN_TIME_CONFIG.get().getStringArray("scenario.rampup-create.threads")
objectSizeList = Main.RUN_TIME_CONFIG.get().getStringArray("scenario.rampup-create.objectsizes")
from java.lang import Long
from java.lang import Integer
from java.lang import InterruptedException
for threadCount in threadCountList:
	LOAD_BUILDER.setThreadsPerNodeDefault(Long.valueOf(threadCount))
	for objectSize in objectSizeList:
		LOG.info(Markers.MSG, "Threadcount = {}", threadCount)
		LOG.info(Markers.MSG, "Object size = {} {}", objectSize, "bytes")
		LOAD_BUILDER.setMinObjSize(Integer.valueOf(objectSize))
		LOAD_BUILDER.setMaxObjSize(Integer.valueOf(objectSize))
		load = LOAD_BUILDER.build()
		if load is None:
			LOG.fatal(Markers.ERR, "No load executor instanced")
			continue
		load.start()
		try:
			load.join(timeOut[1].toMillis(timeOut[0]))
		except InterruptedException:
			pass
		finally:
			load.close()

#
LOG.info(Markers.MSG, "Scenario end")
