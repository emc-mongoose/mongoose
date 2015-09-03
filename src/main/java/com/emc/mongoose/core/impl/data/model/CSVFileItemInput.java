package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.FileDataItemInput;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
/**
 Created by kurila on 30.06.15.
 */
public class CSVFileItemInput<T extends DataItem>
extends CSVItemInput<T>
implements FileDataItemInput<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final Path itemsFilePath;
	/**
	 @param itemsFilePath the input stream to read the data item records from
	 @param itemCls the particular data item implementation class used to parse the records
	 @throws java.io.IOException
	 @throws NoSuchMethodException */
	public CSVFileItemInput(final Path itemsFilePath, final Class<? extends T> itemCls)
	throws IOException, NoSuchMethodException {
		super(Files.newInputStream(itemsFilePath, StandardOpenOption.READ), itemCls);
		this.itemsFilePath = itemsFilePath;
	}
	//
	@Override
	public String toString() {
		return "csvFileItemInput<" + itemsFilePath.getFileName() + ">";
	}
	//
	@Override
	public Path getFilePath() {
		return itemsFilePath;
	}
	//
	@Override
	public long getApproxDataItemsSize(final int maxCount) {
		long sumSize = 0;
		int actualCount = 0;
		try(
			final FileDataItemInput<T> nestedItemSrc = new CSVFileItemInput<>(
				itemsFilePath, itemConstructor.getDeclaringClass()
			)
		) {
			final List<T> firstItemsBatch = new ArrayList<>(maxCount);
			actualCount = nestedItemSrc.read(firstItemsBatch, maxCount);
			for(final T nextItem : firstItemsBatch) {
				sumSize += nextItem.getSize();
			}
		} catch(final IOException | NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to get approx data items size");
		}
		return actualCount > 0 ? sumSize / actualCount : 0;
	}
	//
	@Override
	public void reset()
	throws IOException {
		if (itemsSrc != null) {
			itemsSrc.close();
		}
		itemsSrc = Files.newBufferedReader(itemsFilePath, StandardCharsets.UTF_8);
	}
}
