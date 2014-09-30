#!/usr/bin/env python
from __future__ import print_function, absolute_import, with_statement
from sys import exit
#
from org.apache.logging.log4j import LogManager
LOG = LogManager.getLogger()
#
from com.emc.mongoose.util.conf import RunTimeConfig
from com.emc.mongoose.util.logging import Markers
#
from java.util import NoSuchElementException
#
mode = None
try:
	mode = RunTimeConfig.getString("run.mode")
except NoSuchElementException:
	LOG.fatal(Markers.ERR, "Launch mode is not specified, use -Drun.mode=<VALUE> argument")
	exit()
LOG.info(Markers.MSG, "Launch mode is \"{}\"", mode)
#
INSTANCE = None
from org.apache.commons.configuration import ConversionException
if mode == "controller":
	from com.emc.mongoose.object.load.controller.impl import WSLoadBuilderClient
	from java.rmi import RemoteException
	try:
		try:
			INSTANCE = WSLoadBuilderClient()
		except ConversionException:
			LOG.fatal(Markers.ERR, "Drivers address list should be comma delimited")
			exit()
		except NoSuchElementException:  # no one driver addr not specified, try 127.0.0.1
			LOG.fatal(Markers.ERR, "Drivers address list not specified, try  arg -Drun.drivers=<LIST> to override")
			exit()
	except RemoteException as e:
		LOG.fatal(Markers.ERR, "Failed to create load builder client: {}", e)
		exit()
else: # standalone
	from com.emc.mongoose.object.load.impl import WSLoadBuilder
	INSTANCE = WSLoadBuilder()
#
INSTANCE.setProperties(RunTimeConfig())

