from __future__ import print_function, absolute_import, with_statement
#
from java.lang import IllegalArgumentException, Integer
from java.util import NoSuchElementException
from java.util.concurrent import TimeUnit
#
from org.apache.logging.log4j import LogManager
#
from com.emc.mongoose.common.conf import RunTimeConfig
from com.emc.mongoose.common.logging import Markers
#
LOG = LogManager.getLogger()
#
def init():
    #
    runTimeValue = None  # tuple of (value, unit)
    try:
        runTimeValue = RunTimeConfig.getContext().getRunTime()
        runTimeValue = runTimeValue.split('.')
        runTimeValue = Integer.valueOf(runTimeValue[0]), TimeUnit.valueOf(runTimeValue[1].upper())
        # LOG.info(Markers.MSG, "Using time limit: {} {}", INSTANCE[0], INSTANCE[1].name().lower())
    except NoSuchElementException:
        LOG.error(Markers.ERR, "No timeout specified, try arg -Drun.time=<INTEGER>.<UNIT> to override")
    except IllegalArgumentException:
        LOG.error(Markers.ERR, "Timeout unit should be a name of a constant from TimeUnit enumeration")
    except IndexError:
        LOG.error(Markers.ERR, "Time unit should be specified with timeout value (following after \".\" separator)")
    return runTimeValue
