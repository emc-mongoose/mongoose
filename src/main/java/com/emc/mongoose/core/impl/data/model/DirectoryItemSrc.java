package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import com.emc.mongoose.core.api.data.model.ContainerHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
/**
 Created by andrey on 22.11.15.
 */
public class DirectoryItemSrc<F extends FileItem, D extends Directory<F>>
extends GenericContainerItemSrcBase<F, D> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static DirectoryStream.Filter<Path>
		DEFAULT_DIRECTORY_STREAM_FILTER = new DirectoryStream.Filter<Path>() {
			@Override
			public final boolean accept(final Path entry)
			throws IOException {
				return entry.toFile().isFile();
			}
		};
	//
	private final DirectoryStream<Path> dirStream;
	private final int batchSize;
	private final ContentSource contentSrc;
	//
	private Iterator<Path> dirIterator;
	//
	private final static class DummyDirectoryHelper<F extends FileItem, D extends Directory<F>>
	implements ContainerHelper<F, D> {
		@Override
		public boolean exists(final String addr)
		throws IllegalStateException {
			return false;
		}
		@Override
		public void create(final String addr)
		throws IllegalStateException {
		}
		@Override
		public void delete(final String addr)
		throws IllegalStateException {
		}
		@Override
		public void setVersioning(final String addr, final boolean enabledFlag)
		throws IllegalStateException {
		}
		@Override
		public F buildItem(
			final Constructor<F> itemConstructor, final String rawId, final long size
		) throws IllegalStateException {
			return null;
		}
		@Override
		public void close()
		throws IOException {
		}
	}
	//
	public DirectoryItemSrc(
		final D dir, final Class<F> itemCls, final long maxCount,
		final int batchSize, final ContentSource contentSrc
	) throws IllegalStateException {
		super(new DummyDirectoryHelper<F, D>(), itemCls, maxCount);
		this.batchSize = batchSize;
		this.contentSrc = contentSrc;
		try {
			dirStream = Files.newDirectoryStream(
				Paths.get(dir.getName()), DEFAULT_DIRECTORY_STREAM_FILTER
			);
			reset();
		} catch(final IOException e) {
			throw new IllegalStateException("Failed to list the directory \"" + dir + "\"");
		}
	}
	//
	@Override
	protected final void loadNextPage()
	throws EOFException, IOException {
		Path nextFilePath;
		String nextFileName;
		long nextContentSrcOffset;
		F nextFileItem;
		try {
			for(int i = 0; i < batchSize && dirIterator.hasNext(); i++) {
				nextFilePath = dirIterator.next();
				nextFileName = nextFilePath.getFileName().toString();
				try {
					nextContentSrcOffset = Long.parseLong(nextFileName, Character.MAX_RADIX);
				} catch(final NumberFormatException e) {
					nextContentSrcOffset = 0;
				}
				nextFileItem = itemConstructor.newInstance(
					nextFileName, nextContentSrcOffset, nextFilePath.toFile().length(), 0,
					contentSrc
				);
				items.add(nextFileItem);
			}
		} catch(final IllegalAccessException|InstantiationException|InvocationTargetException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to build file item instance");
		}
	}
	//
	@Override
	public final void reset()
	throws IOException {
		super.reset();
		dirIterator = dirStream.iterator();
	}
	//
	@Override
	public final void close()
	throws IOException {
		dirStream.close();
	}
}
