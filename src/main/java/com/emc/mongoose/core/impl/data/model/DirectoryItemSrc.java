package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.NoSuchElementException;
/**
 Created by andrey on 22.11.15.
 */
public class DirectoryItemSrc<T extends FileItem, C extends Directory<T>>
extends GenericContainerItemSrcBase<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static DirectoryStream.Filter<Path>
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
	public DirectoryItemSrc(
		final C dir, final Class<T> itemCls, final long maxCount,
		final int batchSize, final ContentSource contentSrc
	) throws IllegalStateException {
		super(dir, itemCls, maxCount);
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
		T nextFileItem;
		try {
			for(int i = 0; i < batchSize; i++) {
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
		} catch(final NoSuchElementException e) {
			throw new EOFException(e.toString());
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
}
