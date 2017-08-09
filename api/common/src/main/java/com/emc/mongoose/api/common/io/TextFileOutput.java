package com.emc.mongoose.api.common.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 Created by kurila on 15.11.16.
 */
public class TextFileOutput
extends LinesBufferedStreamOutput {
	
	private final Path filePath;
	
	public TextFileOutput(final Path filePath)
	throws IOException {
		super(Files.newOutputStream(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
		this.filePath = filePath;
	}
	
	@Override
	public Input<String> getInput()
	throws IOException {
		return new TextFileInput(filePath);
	}
}
