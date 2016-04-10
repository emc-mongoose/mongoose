package com.emc.mongoose.core.api.load.generator;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.item.base.Item;
/**
 Created by kurila on 09.05.14.
 A producer feeding the generated items to its consumer.
 May be linked with particular consumer, started and interrupted.
 */
public interface ItemGenerator<T extends Item> {
	//
	Input<T> getInput();
	//
	void setInput(final Input<T> itemInput);
	//
	long getCountLimit();
	//
	void setCountLimit(final long countLimit);
	//
	Output<T> getOutput();
	//
	void setOutput(final Output<T> itemOutput);
	//
	void setSkipCount(final long itemsCount);
	//
	void setLastItem(final T item);
	//
	void reset();
}
