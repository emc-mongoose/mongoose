package com.emc.mongoose.item.op;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.supply.BatchSupplier;
import com.emc.mongoose.supply.ConstantStringSupplier;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Created by kurila on 14.07.16. */
public class OperationsBuilderImpl<I extends Item, O extends Operation<I>>
    implements OperationsBuilder<I, O> {

  protected final int originIndex;

  protected OpType opType = OpType.CREATE; // by default
  protected String inputPath = null;

  protected BatchSupplier<String> outputPathSupplier;
  protected boolean constantOutputPathFlag;
  protected String constantOutputPath;

  protected BatchSupplier<String> uidSupplier;
  protected boolean constantUidFlag;
  protected String constantUid;

  protected BatchSupplier<String> secretSupplier;
  protected boolean constantSecretFlag;
  protected String constantSecret;

  protected Map<String, String> credentialsMap = null;

  public OperationsBuilderImpl(final int originIndex) {
    this.originIndex = originIndex;
  }

  @Override
  public final int originIndex() {
    return originIndex;
  }

  @Override
  public final OpType opType() {
    return opType;
  }

  @Override
  public final OperationsBuilderImpl<I, O> opType(final OpType opType) {
    this.opType = opType;
    return this;
  }

  public final String inputPath() {
    return inputPath;
  }

  @Override
  public final OperationsBuilderImpl<I, O> inputPath(final String inputPath) {
    this.inputPath = inputPath;
    return this;
  }

  @Override
  public final OperationsBuilderImpl<I, O> outputPathSupplier(final BatchSupplier<String> ops) {
    this.outputPathSupplier = ops;
    if (outputPathSupplier == null) {
      constantOutputPathFlag = true;
      constantOutputPath = null;
    } else if (outputPathSupplier instanceof ConstantStringSupplier) {
      constantOutputPathFlag = true;
      constantOutputPath = outputPathSupplier.get();
    } else {
      constantOutputPathFlag = false;
    }
    return this;
  }

  @Override
  public final OperationsBuilderImpl<I, O> uidSupplier(final BatchSupplier<String> uidSupplier) {
    this.uidSupplier = uidSupplier;
    if (uidSupplier == null) {
      constantUidFlag = true;
      constantUid = null;
    } else if (uidSupplier instanceof ConstantStringSupplier) {
      constantUidFlag = true;
      constantUid = uidSupplier.get();
    } else {
      constantUidFlag = false;
    }
    return this;
  }

  @Override
  public final OperationsBuilderImpl<I, O> secretSupplier(
      final BatchSupplier<String> secretSupplier) {
    this.secretSupplier = secretSupplier;
    if (secretSupplier == null) {
      constantSecretFlag = true;
      constantSecret = null;
    } else if (secretSupplier instanceof ConstantStringSupplier) {
      constantSecretFlag = true;
      constantSecret = secretSupplier.get();
    } else {
      constantSecretFlag = false;
    }
    return this;
  }

  @Override
  public OperationsBuilderImpl<I, O> credentialsMap(final Map<String, String> credentials) {
    if (credentials != null) {
      this.credentialsMap = credentials;
      secretSupplier(null);
    }
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public O buildOp(final I item) throws IOException {
    final String uid;
    return (O)
        new OperationImpl<>(
            originIndex,
            opType,
            item,
            inputPath,
            getNextOutputPath(),
            Credential.getInstance(uid = getNextUid(), getNextSecret(uid)));
  }

  @Override
  @SuppressWarnings("unchecked")
  public void buildOps(final List<I> items, final List<O> buff) throws IOException {
    String uid;
    for (final I item : items) {
      buff.add(
          (O)
              new OperationImpl<>(
                  originIndex,
                  opType,
                  item,
                  inputPath,
                  getNextOutputPath(),
                  Credential.getInstance(uid = getNextUid(), getNextSecret(uid))));
    }
  }

  protected final String getNextOutputPath() {
    return constantOutputPathFlag ? constantOutputPath : outputPathSupplier.get();
  }

  protected final String getNextUid() {
    return constantUidFlag ? constantUid : uidSupplier.get();
  }

  protected final String getNextSecret(final String uid) {
    if (uid != null && credentialsMap != null) {
      return credentialsMap.get(uid);
    } else if (constantSecretFlag) {
      return constantSecret;
    } else {
      return secretSupplier.get();
    }
  }

  @Override
  public void close() throws IOException {
    inputPath = null;
    if (outputPathSupplier != null) {
      outputPathSupplier.close();
      outputPathSupplier = null;
    }
    if (uidSupplier != null) {
      uidSupplier.close();
      uidSupplier = null;
    }
    if (secretSupplier != null) {
      secretSupplier.close();
      secretSupplier = null;
    }
    if (credentialsMap != null) {
      credentialsMap.clear();
      credentialsMap = null;
    }
  }
}
