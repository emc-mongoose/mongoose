package com.emc.mongoose.base.item.op;

import com.emc.mongoose.base.item.Item;
import com.github.akurilov.commons.io.Input;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Created by kurila on 14.07.16. */
public interface OperationsBuilder<I extends Item, O extends Operation<I>> extends Closeable {

  int originIndex();

  OpType opType();

  OperationsBuilder<I, O> opType(final OpType opType);

  String inputPath();

  OperationsBuilder<I, O> inputPath(final String inputPath);

  OperationsBuilder<I, O> outputPathSupplier(final Input<String> outputPathSupplier);

  OperationsBuilder<I, O> uidInput(final Input<String> uidInput);

  OperationsBuilder<I, O> secretInput(final Input<String> secretInput);

  OperationsBuilder<I, O> credentialsMap(final Map<String, String> credentials);

  O buildOp(final I item) throws IOException, IllegalArgumentException;

  void buildOps(final List<I> items, final List<O> buff)
      throws IOException, IllegalArgumentException;
}
