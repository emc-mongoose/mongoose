package com.emc.mongoose.load.step;

import java.io.IOException;

public interface StepFileManager {
	/**
	 @param path the path of the file, may be null (new temporary file path is used in this case)
	 @return the name of the new file service
	 */
	String createFile(final String path)
	throws IOException;

	String createLogFile(final String loggerName, final String testStepId)
	throws IOException;
}
