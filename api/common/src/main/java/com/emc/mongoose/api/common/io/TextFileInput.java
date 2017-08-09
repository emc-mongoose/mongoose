package com.emc.mongoose.api.common.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 Created by kurila on 15.11.16.
 */
public class TextFileInput
extends LinesBufferedStreamInput {
	
	public TextFileInput(final Path filePath)
	throws IOException {
		super(Files.newInputStream(filePath, StandardOpenOption.READ));
	}
}
