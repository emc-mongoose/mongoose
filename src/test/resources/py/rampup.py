from __future__ import print_function, absolute_import, with_statement
from sys import exit
from loadbuilder import INSTANCE as loadBuilder
#
from org.apache.logging.log4j import LogManager
#
from com.emc.mongoose.logging import Markers
from com.emc.mongoose.object.http.api import WSRequestConfig
#
from java.util import NoSuchElementException
#
LOG = LogManager.getLogger()
loadBuilder.setRequestConfig(WSRequestConfig.useItem())
# TODO
