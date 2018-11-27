package com.emc.mongoose.item.op.composite;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.partial.PartialOperation;
import java.util.List;

/** Created by andrey on 25.11.16. Marker interface */
public interface CompositeOperation<I extends Item> extends Operation<I> {

  @Override
  I item();

  String get(final String key);

  void put(final String key, final String value);

  List<? extends PartialOperation> subOperations();

  /** Should be invoked only after subOperations() * */
  void markSubTaskCompleted();

  boolean allSubOperationsDone();
}
