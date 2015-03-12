package com.emc.mongoose.core.impl.util.log;
//
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
//
import java.io.OutputStream;
/**
 Created by andrey on 13.03.15.
 */
public final class RunIdFileManager
extends OutputStreamManager {
	protected RunIdFileManager(
		final OutputStream os, final String fileName, final Layout<?> layout
	) {
		super(os, fileName, layout);
	}
}
