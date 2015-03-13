#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
#
from org.apache.logging.log4j import Level, LogManager
#
from com.emc.mongoose.run import Main
from com.emc.mongoose.core.impl.util import RunTimeConfig
from com.emc.mongoose.core.api.util.log import Markers
from com.emc.mongoose.core.impl.util.log import TraceLogger
#
from java.lang import IllegalStateException
from java.util import NoSuchElementException
#
LOG = LogManager.getLogger()
#
def init():
	localRunTimeConfig = RunTimeConfig.getContext()
	#
	mode = None
	try:
		mode = localRunTimeConfig.getRunMode()
	except NoSuchElementException:
		LOG.fatal(Markers.ERR, "Launch mode is not specified, use -Drun.mode=<VALUE> argument")
	#
	loadBuilderInstance = None
	#
	from org.apache.commons.configuration import ConversionException
	if mode == Main.RUN_MODE_CLIENT or mode == Main.RUN_MODE_COMPAT_CLIENT:
		from com.emc.mongoose.client.impl.load.builder import BasicWSLoadBuilderClient
		from java.rmi import RemoteException
		try:
			try:
				loadBuilderInstance = BasicWSLoadBuilderClient(localRunTimeConfig)
			except ConversionException:
				LOG.fatal(Markers.ERR, "Servers address list should be comma delimited")
			except NoSuchElementException:  # no one server addr not specified, try 127.0.0.1
				LOG.fatal(Markers.ERR, "Servers address list not specified, try  arg -Dremote.servers=<LIST> to override")
		except RemoteException as e:
			LOG.fatal(Markers.ERR, "Failed to create load builder client: {}", e)
	else: # standalone
		from com.emc.mongoose.core.impl.load.builder import BasicWSLoadBuilder
		#
		try:
			loadBuilderInstance = BasicWSLoadBuilder(localRunTimeConfig)
		except IllegalStateException as e:
			TraceLogger(LOG, Level.FATAL, e, "Failed to create load builder client")
	#
	if loadBuilderInstance is None:
		LOG.fatal(Markers.ERR, "No load builder instanced")
	return loadBuilderInstance
