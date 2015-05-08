package com.emc.mongoose.core.impl.load.model.reader;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by olga on 08.05.15.
 */
public abstract class FileReader {

	public BufferedReader fReader;

	public FileReader(final Path fPath)
	throws IOException
	{
		this.fReader = Files.newBufferedReader(fPath, StandardCharsets.UTF_8);
	}

	public abstract String getDataItemString()
	throws IOException;
}
