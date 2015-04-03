package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
/**
 Created by kurila on 03.04.15.
 */
public final class MemUtil {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static Runtime RUNTIME = Runtime.getRuntime();
	private final static MBeanServer
		MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();
	private final static ObjectName OBJ_NAME_OS;
	static {
		ObjectName t = null;
		try {
			t = new ObjectName("java.lang", "type", "OperatingSystem");
		} catch(final MalformedObjectNameException e) {
			e.printStackTrace(System.err);
		}
		OBJ_NAME_OS = t;
	}
	private final static String PHYS_MEM_ATTR_NAME = "FreePhysicalMemorySize";
	//
	public static long getCurrentSafeFreeMemSize() {
		// determine the current max free heap size
		long
			freeMaxHeapSize = RUNTIME.maxMemory() - RUNTIME.totalMemory() + RUNTIME.freeMemory(),
			freeMaxPhysSize = freeMaxHeapSize;
		// determine the current max free physical memory size
		try {
			freeMaxPhysSize = Long.class.cast(
				MBEAN_SERVER.getAttribute(OBJ_NAME_OS, PHYS_MEM_ATTR_NAME)
			);
		} catch(final Exception e) {
			LogUtil.failure(
				LOG, Level.WARN, e, "Failed to determine the amount of free physical memory"
			);
		}
		LOG.debug(
			LogUtil.MSG, "Max free heap: {}, max free physical: {}, using the least value",
			RunTimeConfig.formatSize(freeMaxHeapSize), RunTimeConfig.formatSize(freeMaxPhysSize)
		);
		return Math.min(freeMaxHeapSize, freeMaxPhysSize);
	}

}
