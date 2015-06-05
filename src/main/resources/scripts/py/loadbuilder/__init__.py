#!/usr/bin/env python
from org.apache.logging.log4j import Level, LogManager
#
from org.apache.commons.configuration import ConversionException
#
from com.emc.mongoose.common.conf import Constants, RunTimeConfig
from com.emc.mongoose.common.logging import LogUtil
#
from java.lang import IllegalStateException
from java.rmi import RemoteException
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
		LOG.fatal(LogUtil.ERR, "Launch mode is not specified, use -Drun.mode=<VALUE> argument")
	#
	loadBuilderInstance = None
	#
	if mode == Constants.RUN_MODE_CLIENT or mode == Constants.RUN_MODE_COMPAT_CLIENT:
		############################################################################################
		from com.emc.mongoose.client.impl.load.builder import BasicWSLoadBuilderClient
		############################################################################################
		try:
			try:
				loadBuilderInstance = BasicWSLoadBuilderClient(localRunTimeConfig)
			except ConversionException:
				LOG.fatal(LogUtil.ERR, "Servers address list should be comma delimited")
			except NoSuchElementException:  # no one server addr not specified, try 127.0.0.1
				LOG.fatal(LogUtil.ERR, "Servers address list not specified, try  arg -Dremote.servers=<LIST> to override")
		except RemoteException as e:
			LOG.fatal(LogUtil.ERR, "Failed to create load builder client: {}", e)
	else: # standalone
		############################################################################################
		from com.emc.mongoose.core.impl.load.builder import BasicWSLoadBuilder
		############################################################################################
		try:
			loadBuilderInstance = BasicWSLoadBuilder(localRunTimeConfig)
		except IllegalStateException as e:
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to create load builder client")
	#
	if loadBuilderInstance is None:
		LOG.fatal(LogUtil.ERR, "No load builder instanced")
	return loadBuilderInstance
