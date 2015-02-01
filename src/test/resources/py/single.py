#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
from sys import exit
#
from timeout import timeout_init
from loadbuilder import loadbuilder_init
#from loadbuilder import INSTANCE as LOAD_BUILDER
#from timeout import INSTANCE as RUN_TIME
#
from com.emc.mongoose.base.api import AsyncIOTask
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.logging import Markers
#
from org.apache.logging.log4j import Level, LogManager
#
from java.lang import IllegalArgumentException
from java.util import NoSuchElementException
#
RUN_TIME = timeout_init()
LOCAL_RUN_TIME_CONFIG = Main.RUN_TIME_CONFIG.get()
LOAD_BUILDER = loadbuilder_init()
LOG = LogManager.getLogger()
#
try:
	loadType = AsyncIOTask.Type.valueOf(LOCAL_RUN_TIME_CONFIG.getString("scenario.single.load").upper())
	LOG.debug(Markers.MSG, "Using load type: {}", loadType.name())
	LOAD_BUILDER.setLoadType(loadType)
except NoSuchElementException:
	LOG.error(Markers.ERR, "No load type specified, try arg -Dscenario.single.load=<VALUE> to override")
except IllegalArgumentException:
	LOG.error(Markers.ERR, "No such load type, it should be a constant from Load.Type enumeration")
	exit()
#
from java.lang import Exception
from com.emc.mongoose.util.logging import TraceLogger
load = None
try:
	load = LOAD_BUILDER.build()
except Exception as e:
	TraceLogger.failure(LOG, Level.FATAL, e, "Failed to instantiate the load executor")
	e.printStackTrace()
#
if load is None:
	LOG.fatal(Markers.ERR, "No load executor instanced")
	exit()
#
from java.lang import InterruptedException
if __name__=="__builtin__":
	load.start()
	try:
		load.join(RUN_TIME[1].toMillis(RUN_TIME[0]))
	except InterruptedException:
		pass
	finally:
		load.close()
		LOG.info(Markers.MSG, "Scenario end")
