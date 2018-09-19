package com.emc.mongoose.storage.driver.coop.nio.fs;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.Set;

public interface FsConstants {

	FileSystem FS = FileSystems.getDefault();
	FileSystemProvider FS_PROVIDER = FS.provider();

	Set<OpenOption> CREATE_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.CREATE);
			add(StandardOpenOption.TRUNCATE_EXISTING);
			add(StandardOpenOption.WRITE);
		}
	};
	Set<OpenOption> READ_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.READ);
		}
	};
	Set<OpenOption> WRITE_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.WRITE);
		}
	};
}
