package com.emc.mongoose.item.op.token;

import com.emc.mongoose.item.TokenItem;
import com.emc.mongoose.item.op.OperationsBuilderImpl;
import com.emc.mongoose.storage.Credential;
import java.io.IOException;
import java.util.List;

/** Created by kurila on 14.07.16. */
public class TokenOperationsBuilderImpl<I extends TokenItem, O extends TokenOperation<I>>
    extends OperationsBuilderImpl<I, O> implements TokenOperationsBuilder<I, O> {

  public TokenOperationsBuilderImpl(final int originIndex) {
    super(originIndex);
  }

  @Override
  @SuppressWarnings("unchecked")
  public O buildOp(final I item) throws IOException {
    final String uid;
    return (O)
        new TokenOperationImpl<>(
            originIndex,
            opType,
            item,
            Credential.getInstance(uid = getNextUid(), getNextSecret(uid)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void buildOps(final List<I> items, final List<O> buff) throws IOException {
    String uid;
    for (final I item : items) {
      buff.add(
          (O)
              new TokenOperationImpl<>(
                  originIndex,
                  opType,
                  item,
                  Credential.getInstance(uid = getNextUid(), getNextSecret(uid))));
    }
  }
}
