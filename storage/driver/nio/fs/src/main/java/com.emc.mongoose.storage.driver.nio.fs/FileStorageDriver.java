package com.emc.mongoose.storage.driver.nio.fs;

import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import com.emc.mongoose.model.io.task.data.mutable.MutableDataIoTask;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.model.storage.StorageDriver;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;

/**
 Created by andrey on 01.12.16.
 */
public interface FileStorageDriver<
	I extends MutableDataItem, O extends MutableDataIoTask<I, R>, R extends DataIoResult
> extends StorageDriver<I, O, R> {

	FileSystem FS = FileSystems.getDefault();
	FileSystemProvider FS_PROVIDER = FS.provider();
}
