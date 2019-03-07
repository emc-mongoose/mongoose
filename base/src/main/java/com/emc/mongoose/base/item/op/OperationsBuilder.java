package com.emc.mongoose.base.item.op;

import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.storage.Credential;
import com.github.akurilov.commons.io.Input;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Created by kurila on 14.07.16. */
public interface OperationsBuilder<I extends Item, O extends Operation<I>> extends AutoCloseable {

	int originIndex();

	OpType opType();

	OperationsBuilder<I, O> opType(final OpType opType);

	String inputPath();

	OperationsBuilder<I, O> inputPath(final String inputPath);

	OperationsBuilder<I, O> outputPathInput(final Input<String> outputPathSupplier);

	OperationsBuilder<I, O> credentialInput(final Input<Credential> credentialInput);

	OperationsBuilder<I, O> credentialsByPath(final Map<String, Credential> credentials);

	O buildOp(final I item) throws IOException, IllegalArgumentException;

	void buildOps(final List<I> items, final List<O> buff)
					throws IOException, IllegalArgumentException;

	@Override
	void close();
}
