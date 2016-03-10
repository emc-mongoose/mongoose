package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.generator.FormattingGenerator;
import com.emc.mongoose.common.generator.ValueGenerator;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
//
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.BasicFileItem;
import com.emc.mongoose.core.impl.item.data.DirectoryItemSrc;
import org.apache.commons.lang.StringUtils;
//
import java.io.IOException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicFileIOConfig<F extends FileItem, D extends Directory<F>>
extends IOConfigBase<F, D>
implements FileIOConfig<F, D> {
	//
	private int batchSize = RunTimeConfig.getContext().getBatchSize();
	//
	private ValueGenerator<String> pathGenerator;
	//
	public BasicFileIOConfig() {
		super();
		pathGenerator = new FormattingGenerator(super.getNamePrefix());
	}
	//
	public BasicFileIOConfig(final BasicFileIOConfig<F, D> another) {
		super(another);
		pathGenerator = another.pathGenerator;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicFileIOConfig<F, D> setRunTimeConfig(final RunTimeConfig rtConfig) {
		super.setRunTimeConfig(rtConfig);
		setContainer((D) new BasicDirectory<F>(getNamePrefix()));
		batchSize = rtConfig.getBatchSize();
		return this;
	}
	//
	@Override
	public ItemSrc<F> getContainerListInput(final long maxCount, final String addr) {
		return new DirectoryItemSrc<>(
			container, getItemClass(), maxCount, batchSize, contentSrc
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<D> getContainerClass() {
		return (Class) BasicDirectory.class;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<F> getItemClass() {
		return (Class<F>) BasicFileItem.class;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
	//
	@Override
	public String toString() {
	return "FS-" + StringUtils.capitalize(loadType.name().toLowerCase());
	}

	@Override
	public String getNamePrefix() {
		return pathGenerator.get();
	}

	@Override
	public BasicFileIOConfig<F, D> setNamePrefix(final String namePrefix) {
		pathGenerator = new FormattingGenerator(namePrefix);
		super.setNamePrefix(namePrefix);
		return this;
	}
}
