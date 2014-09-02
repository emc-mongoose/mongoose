#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
from sys import exit
#
from loadbuilder import INSTANCE as loadBuilder
#
from com.emc.mongoose.api import Request
from com.emc.mongoose.conf import RunTimeConfig
from com.emc.mongoose.logging import Markers
#
from org.apache.logging.log4j import LogManager
#
from java.lang import IllegalArgumentException
from java.util import NoSuchElementException
#
LOG = LogManager.getLogger()
#
try:
	loadType = Request.Type.valueOf(RunTimeConfig.getString("scenario.rampup-create.load").upper())
	LOG.info(Markers.MSG, "Using load type: {}", loadType.name())
	loadBuilder.setLoadType(loadType)
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
	timeOut = RunTimeConfig.getString("run.time")
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
threadCountList=RunTimeConfig.getStringArray("scenario.rampup-create.threads")
objectSizeList=RunTimeConfig.getStringArray("scenario.rampup-create.objectsizes")
from java.lang import Long
from java.lang import Integer
for threadCount in threadCountList:
	loadBuilder.setThreadsPerNodeDefault(Long.valueOf(threadCount))
	for objectSize in objectSizeList:
		LOG.info(Markers.MSG,"Threadcount = {}",threadCount)
		LOG.info(Markers.MSG,"Object size = {} {}", objectSize, "bytes")
		loadBuilder.setMinObjSize(Integer.valueOf(objectSize))
		loadBuilder.setMaxObjSize(Integer.valueOf(objectSize))
		load=loadBuilder.build()
		load.start()
		load.join(timeOut[1].toMillis(timeOut[0]))
		load.close()

#

LOG.info(Markers.MSG, "Scenario end")
