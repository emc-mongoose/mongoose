from __future__ import print_function, absolute_import, with_statement
from sys import exit
from java.lang import Integer
from java.util.concurrent import TimeUnit
from org.apache.logging.log4j import LogManager
#
from com.emc.mongoose.run import Main
from com.emc.mongoose.util.logging import Markers
#
from java.lang import IllegalArgumentException
from java.util import NoSuchElementException

def timeout_init():
    #
    LOG = LogManager.getLogger()
    #
    INSTANCE = None  # tuple of (value, unit)
    try:
        INSTANCE = Main.RUN_TIME_CONFIG.get().getRunTime()
        INSTANCE = INSTANCE.split('.')
        INSTANCE = Integer.valueOf(INSTANCE[0]), TimeUnit.valueOf(INSTANCE[1].upper())
        # LOG.info(Markers.MSG, "Using time limit: {} {}", INSTANCE[0], INSTANCE[1].name().lower())
    except NoSuchElementException:
        LOG.error(Markers.ERR, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override")
    except IllegalArgumentException:
        LOG.error(Markers.ERR, "Timeout unit should be a name of a constant from TimeUnit enumeration")
        exit()
    except IndexError:
        LOG.error(Markers.ERR, "Time unit should be specified with timeout value (following after \".\" separator)")
        exit()
    return INSTANCE
