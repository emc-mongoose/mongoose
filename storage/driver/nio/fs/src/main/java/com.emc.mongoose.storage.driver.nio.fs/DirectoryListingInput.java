package com.emc.mongoose.storage.driver.nio.fs;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import static com.emc.mongoose.storage.driver.nio.fs.FileStorageDriver.FS;
import static com.emc.mongoose.storage.driver.nio.fs.FileStorageDriver.FS_PROVIDER;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Iterator;
import java.util.List;

/**
 Created by andrey on 01.12.16.
 */
public final class DirectoryListingInput<I extends Item>
implements Input<I> {

	private static final DirectoryStream.Filter<Path> ACCEPT_ALL_PATHS_FILTER = entry -> true;

	private final DirectoryStream<Path> dirStream;
	private volatile Iterator<Path> filesIter;
	private final ItemFactory<I> itemFactory;
	private final int idRadix;
	private final int idPrefixLength;

	public DirectoryListingInput(
		final String path, final ItemFactory<I> itemFactory, final int idRadix,
		final String idPrefix
	) throws IOException {
		if(idPrefix == null || idPrefix.isEmpty()) {
			dirStream = FS_PROVIDER.newDirectoryStream(
				FS.getPath(path), ACCEPT_ALL_PATHS_FILTER
			);
			idPrefixLength = 0;
		} else {
			final PathMatcher pathPrefixMatcher = FS.getPathMatcher("glob:" + idPrefix + "*");
			dirStream = FS_PROVIDER.newDirectoryStream(
				FS.getPath(path), new PathMatchDirectoryStreamFilter(pathPrefixMatcher)
			);
			idPrefixLength = idPrefix.length();
		}
		filesIter = dirStream.iterator();
		this.itemFactory = itemFactory;
		this.idRadix = idRadix;
	}

	private static final class PathMatchDirectoryStreamFilter
	implements DirectoryStream.Filter<Path> {

		private final PathMatcher pathMatcher;

		public PathMatchDirectoryStreamFilter(final PathMatcher pathMatcher) {
			this.pathMatcher = pathMatcher;
		}

		@Override
		public final boolean accept(final Path entry)
		throws IOException {
			return pathMatcher.matches(entry.getFileName());
		}
	}

	@Override
	public final I get()
	throws EOFException {
		if(filesIter.hasNext()) {
			final Path nextFilePath = filesIter.next();
			final String nextFullPath = nextFilePath.toString();
			final File nextFile = new File(nextFullPath);
			final String nextFileName = nextFile.getName();
			try {
				final long offset;
				if(idPrefixLength > 0) {
					// only items with the prefix are passed so it's safe to invoke the substring
					offset = Long.parseLong(nextFileName.substring(idPrefixLength), idRadix);
				} else {
					offset = Long.parseLong(nextFileName, idRadix);
				}
				return itemFactory.getItem(nextFullPath, offset, nextFile.length());
			} catch(final NumberFormatException e) {
				// try to not use the offset (verification may be disabled)
				return itemFactory.getItem(nextFullPath, 0, nextFile.length());
			}
		} else {
			throw new EOFException();
		}
	}

	@Override
	public final int get(final List<I> buffer, final int limit)
	throws IOException {
		int i = 0;
		Path nextFilePath;
		File nextFile;
		String nextFullPath;
		String nextFileName;
		I nextItem;
		while(i < limit && filesIter.hasNext()) {
			nextFilePath = filesIter.next();
			nextFullPath = nextFilePath.toString();
			nextFile = new File(nextFullPath);
			nextFileName = nextFile.getName();
			try {
				final long offset;
				if(idPrefixLength > 0) {
					// only items with the prefix are passed so it's safe to invoke the substring
					offset = Long.parseLong(nextFileName.substring(idPrefixLength), idRadix);
				} else {
					offset = Long.parseLong(nextFileName, idRadix);
				}
				nextItem = itemFactory.getItem(nextFullPath, offset, nextFile.length());
			} catch(final NumberFormatException e) {
				// try to not use the offset (verification may be disabled)
				nextItem = itemFactory.getItem(nextFullPath, 0, nextFile.length());
			}
			buffer.add(nextItem);
			i ++;
		}
		return i;
	}

	@Override
	public final long skip(final long count)
	throws IOException {
		for(long i = 0; i < count; i ++) {
			if(filesIter.hasNext()) {
				filesIter.next();
			} else {
				return i;
			}
		}
		return count;
	}

	@Override
	public final void reset()
	throws IOException {
		filesIter = dirStream.iterator();
	}

	@Override
	public final void close()
	throws IOException {
		dirStream.close();
		filesIter = null;
	}
}
