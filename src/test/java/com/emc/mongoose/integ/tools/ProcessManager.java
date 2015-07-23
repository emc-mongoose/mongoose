package com.emc.mongoose.integ.tools;
//
import sun.management.VMManagement;
//
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by olga on 23.07.15.
 */
public final class ProcessManager {

	public static long getProcessID()
	throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException{
		final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		final Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
		jvmField.setAccessible(true);
		final VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
		final Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
		getProcessIdMethod.setAccessible(true);
		return (long) getProcessIdMethod.invoke(vmManagement);
	}

	public static void callSIGINT(final long processID)
	throws IOException {
		Runtime.getRuntime().exec(String.format("kill -SIGINT %d", processID));
	}
}
