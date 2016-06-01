package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.load.builder.DirectoryLoadBuilder;
import com.emc.mongoose.core.api.load.executor.DirectoryLoadExecutor;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIoConfig;
import com.emc.mongoose.core.impl.load.executor.BasicDirectoryLoadExecutor;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;

import static com.emc.mongoose.common.io.value.PatternDefinedInput.PATTERN_SYMBOL;

/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadBuilder<
	T extends FileItem,
	C extends Directory<T>,
	U extends DirectoryLoadExecutor<T, C>
>
extends ContainerLoadBuilderBase<T, C, U>
implements DirectoryLoadBuilder<T, C, U> {
	//
	public BasicDirectoryLoadBuilder(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	protected FileIoConfig<T, C> getIoConfig(final AppConfig appConfig) {
		return new BasicFileIoConfig<>(appConfig);
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		// create parent directories
		final Container d = ioConfig.getDstContainer();
		final String p = d == null ? null : d.getName();
		if(p != null && !p.isEmpty() && p.indexOf(PATTERN_SYMBOL) < 0) {
			try {
				Files.createDirectories(Paths.get(p));
			} catch(final IOException e) {
				throw new IllegalStateException(
					"Failed to create target directories @ \"" + p + "\""
				);
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually()
	throws CloneNotSupportedException {
		final FileIoConfig ioConfigCopy = (FileIoConfig) ioConfig.clone();
		return (U) new BasicDirectoryLoadExecutor<>(
			appConfig, ioConfigCopy, threadCount, selectItemInput(ioConfigCopy), countLimit,
			sizeLimit, rateLimit
		);
	}
}
