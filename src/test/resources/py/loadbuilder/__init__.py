#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
from sys import exit
#
from org.apache.logging.log4j import LogManager
LOG = LogManager.getLogger()
#
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.conf import RunTimeConfig
from com.emc.mongoose.util.logging import Markers
#
from java.util import NoSuchElementException
#
mode = None
try:
	mode = Main.RUN_TIME_CONFIG.getRunMode()
except NoSuchElementException:
	LOG.fatal(Markers.ERR, "Launch mode is not specified, use -Drun.mode=<VALUE> argument")
	exit()
LOG.info(Markers.MSG, "Launch mode is \"{}\"", mode)
#
INSTANCE = None
from org.apache.commons.configuration import ConversionException
if mode == Main.VALUE_RUN_MODE_CLIENT or mode == Main.VALUE_RUN_MODE_COMPAT_CLIENT:
	from com.emc.mongoose.object.load.client import WSLoadBuilderClientImpl
	from java.rmi import RemoteException
	try:
		try:
			INSTANCE = WSLoadBuilderClientImpl()
		except ConversionException:
			LOG.fatal(Markers.ERR, "Servers address list should be comma delimited")
			exit()
		except NoSuchElementException:  # no one server addr not specified, try 127.0.0.1
			LOG.fatal(Markers.ERR, "Servers address list not specified, try  arg -Dremote.servers=<LIST> to override")
			exit()
	except RemoteException as e:
		LOG.fatal(Markers.ERR, "Failed to create load builder client: {}", e)
		exit()
else: # standalone
	from com.emc.mongoose.object.load import WSLoadBuilderImpl
	INSTANCE = WSLoadBuilderImpl()
#
if INSTANCE is None:
	LOG.fatal(Markers.ERR, "No load builder instanced")
	exit()
INSTANCE.setProperties(RunTimeConfig())
