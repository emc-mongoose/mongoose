package com.emc.mongoose.base.item.op;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.config.ConstStringInput;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.storage.Credential;
import com.github.akurilov.commons.io.Input;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Created by kurila on 14.07.16. */
public class OperationsBuilderImpl<I extends Item, O extends Operation<I>>
    implements OperationsBuilder<I, O> {

  protected final int originIndex;

  protected OpType opType = OpType.CREATE; // by default
  protected String inputPath = null;

  protected Input<String> outputPathSupplier;
  protected boolean constantOutputPathFlag;
  protected String constantOutputPath;

  protected Input<String> uidInput;
  protected boolean constantUidFlag;
  protected String constantUid;

  protected Input<String> secretInput;
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
  public final OperationsBuilderImpl<I, O> outputPathSupplier(final Input<String> ops) {
    this.outputPathSupplier = ops;
    if (outputPathSupplier == null) {
      constantOutputPathFlag = true;
      constantOutputPath = null;
    } else if (outputPathSupplier instanceof ConstStringInput) {
      constantOutputPathFlag = true;
      constantOutputPath = outputPathSupplier.get();
    } else {
      constantOutputPathFlag = false;
    }
    return this;
  }

  @Override
  public final OperationsBuilderImpl<I, O> uidInput(final Input<String> uidInput) {
    this.uidInput = uidInput;
    if (uidInput == null) {
      constantUidFlag = true;
      constantUid = null;
    } else if (uidInput instanceof ConstStringInput) {
      constantUidFlag = true;
      constantUid = uidInput.get();
    } else {
      constantUidFlag = false;
    }
    return this;
  }

  @Override
  public final OperationsBuilderImpl<I, O> secretInput(final Input<String> secretInput) {
    this.secretInput = secretInput;
    if (secretInput == null) {
      constantSecretFlag = true;
      constantSecret = null;
    } else if (secretInput instanceof ConstStringInput) {
      constantSecretFlag = true;
      constantSecret = secretInput.get();
    } else {
      constantSecretFlag = false;
    }
    return this;
  }

  @Override
  public OperationsBuilderImpl<I, O> credentialsMap(final Map<String, String> credentials) {
    if (credentials != null) {
      this.credentialsMap = credentials;
      secretInput(null);
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
    return constantUidFlag ? constantUid : uidInput.get();
  }

  protected final String getNextSecret(final String uid) {
    if (uid != null && credentialsMap != null) {
      return credentialsMap.get(uid);
    } else if (constantSecretFlag) {
      return constantSecret;
    } else {
      return secretInput.get();
    }
  }

  @Override
  public void close() throws IOException {
    inputPath = null;
    try {
      if (outputPathSupplier != null) {
        outputPathSupplier.close();
        outputPathSupplier = null;
      }
      if (uidInput != null) {
        uidInput.close();
        uidInput = null;
      }
      if (secretInput != null) {
        secretInput.close();
        secretInput = null;
      }
      if (credentialsMap != null) {
        credentialsMap.clear();
        credentialsMap = null;
      }
    } catch (final Exception e) {
      throwUnchecked(e);
    }
  }
}
