package com.emc.mongoose.base.item.io;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.ItemFactory;
import com.github.akurilov.commons.io.file.FileOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Created by kurila on 30.06.15. */
public class CsvFileItemOutput<I extends Item> extends CsvItemOutput<I> implements FileOutput<I> {

	protected Path itemsFilePath;

	public CsvFileItemOutput(final Path itemsFilePath, final ItemFactory<I> itemFactory)
					throws IOException {
		super(Files.newOutputStream(itemsFilePath, WRITE, CREATE), itemFactory);
		this.itemsFilePath = itemsFilePath;
	}

	public CsvFileItemOutput(final ItemFactory<I> itemFactory) throws IOException {
		this(Files.createTempFile(null, ".csv"), itemFactory);
		this.itemsFilePath.toFile().deleteOnExit();
	}

	@Override
	public CsvFileItemInput<I> getInput() {
		try {
			return new CsvFileItemInput<>(itemsFilePath, itemFactory);
		} catch (final NoSuchMethodException | IOException e) {
			throwUnchecked(e);
		}
		return null;
	}

	@Override
	public String toString() {
		return "csvFileItemOutput<" + itemsFilePath.getFileName() + ">";
	}

	@Override
	public final Path filePath() {
		return itemsFilePath;
	}
}
