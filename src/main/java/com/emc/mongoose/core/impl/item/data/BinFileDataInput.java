package com.emc.mongoose.core.impl.item.data;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.FileDataItemInput;
//
import com.emc.mongoose.core.impl.item.base.BinFileInput;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
/**
 Created by kurila on 20.10.15.
 */
public class BinFileDataInput<T extends DataItem>
extends BinFileInput<T>
implements FileDataItemInput<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	/**
	 @param itemsSrcPath the path to the file which should be used to restore the serialized items
	 @throws IOException if unable to open the file for reading */
	public BinFileDataInput(final Path itemsSrcPath) throws IOException {
		super(itemsSrcPath);
	}
	//
	@Override
	public long getAvgDataSize(final int maxCount) {
		long sumSize = 0;
		int actualCount = 0;
		try(final FileDataItemInput<T> nestedItemSrc = new BinFileDataInput<>(itemsSrcPath)) {
			final List<T> firstItemsBatch = new ArrayList<>(maxCount);
			actualCount = nestedItemSrc.get(firstItemsBatch, maxCount);
			for(final T nextItem : firstItemsBatch) {
				sumSize += nextItem.getSize();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to get approx data items size");
		}
		return actualCount > 0 ? sumSize / actualCount : 0;
	}

}
