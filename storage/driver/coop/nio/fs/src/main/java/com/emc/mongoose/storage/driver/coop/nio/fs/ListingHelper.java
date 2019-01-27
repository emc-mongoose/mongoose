package com.emc.mongoose.storage.driver.coop.nio.fs;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public interface ListingHelper {

	DirectoryStream.Filter<Path> ACCEPT_ALL_PATHS_FILTER = entry -> true;

	final class PrefixDirectoryStreamFilter
		implements DirectoryStream.Filter<Path> {

		private final PathMatcher pathPrefixMatcher;

		public PrefixDirectoryStreamFilter(final String prefix) {
			pathPrefixMatcher = FsConstants.FS.getPathMatcher("glob:" + prefix + "*");
		}

		@Override
		public final boolean accept(final Path entry)
		throws IOException {
			return pathPrefixMatcher.matches(entry.getFileName());
		}
	}

	static <I extends Item> List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {

		final DirectoryStream.Filter<Path> filter = (prefix == null || prefix.isEmpty()) ?
			ACCEPT_ALL_PATHS_FILTER : new PrefixDirectoryStreamFilter(prefix);
		final List<I> buff = new ArrayList<>(count);

		try(
			final DirectoryStream<Path> dirStream = FsConstants.FS_PROVIDER.newDirectoryStream(Paths.get(path), filter)
		) {
			final int prefixLength = (prefix == null || prefix.isEmpty()) ? 0 : prefix.length();

			File nextFile;
			String nextFileName;
			I nextItem;

			final String lastPrevItemName;
			boolean lastPrevItemNameFound;
			if(lastPrevItem == null) {
				lastPrevItemName = null;
				lastPrevItemNameFound = true;
			} else {
				lastPrevItemName = lastPrevItem.name();
				lastPrevItemNameFound = false;
			}

			for(final Path nextPath : dirStream) {
				nextFile = new File(nextPath.toString());
				nextFileName = nextFile.getAbsolutePath();
				if(lastPrevItemNameFound) {
					try {
						final long offset;
						if(prefixLength > 0) {
							// only items with the prefix are passed so it's safe
							offset = Long.parseLong(nextFileName.substring(prefixLength), idRadix);
						} else {
							offset = Long.parseLong(nextFileName, idRadix);
						}
						nextItem = itemFactory.getItem(nextFile.getAbsolutePath(), offset, nextFile.length());
					} catch(final NumberFormatException e) {
						// try to not use the offset (read verification should be disabled)
						nextItem = itemFactory.getItem(nextFile.getAbsolutePath(), 0, nextFile.length());
					}
					buff.add(nextItem);
					if(count == buff.size()) {
						break;
					}
				} else {
					lastPrevItemNameFound = nextFileName.equals(lastPrevItemName);
				}
			}
		} catch(final DirectoryIteratorException e) {
			throw e.getCause(); // according the JDK documentation
		}

		return buff;
	}
}
