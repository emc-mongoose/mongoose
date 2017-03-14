package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.common.supply.ConstantStringSupplier;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.storage.Credential;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 Created by kurila on 14.07.16.
 */
public class BasicIoTaskBuilder<I extends Item, O extends IoTask<I>>
implements IoTaskBuilder<I, O> {
	
	protected final int originCode = hashCode();
	
	protected IoType ioType = IoType.CREATE; // by default
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
	
	@Override
	public final int getOriginCode() {
		return originCode;
	}
	
	@Override
	public final IoType getIoType() {
		return ioType;
	}

	@Override
	public final BasicIoTaskBuilder<I, O> setIoType(final IoType ioType) {
		this.ioType = ioType;
		return this;
	}

	public final String getInputPath() {
		return inputPath;
	}

	@Override
	public final BasicIoTaskBuilder<I, O> setInputPath(final String inputPath) {
		this.inputPath = inputPath;
		return this;
	}
	
	@Override
	public final BasicIoTaskBuilder<I, O> setOutputPathSupplier(final BatchSupplier<String> ops) {
		this.outputPathSupplier = ops;
		if(outputPathSupplier == null) {
			constantOutputPathFlag = true;
			constantOutputPath = null;
		} else if(outputPathSupplier instanceof ConstantStringSupplier) {
			constantOutputPathFlag = true;
			constantOutputPath = outputPathSupplier.get();
		} else {
			constantOutputPathFlag = false;
		}
		return this;
	}
	
	@Override
	public final BasicIoTaskBuilder<I, O> setUidSupplier(final BatchSupplier<String> uidSupplier) {
		this.uidSupplier = uidSupplier;
		if(uidSupplier == null) {
			constantUidFlag = true;
			constantUid = null;
		} else if(uidSupplier instanceof ConstantStringSupplier) {
			constantUidFlag = true;
			constantUid = uidSupplier.get();
		} else {
			constantUidFlag = false;
		}
		return this;
	}
	
	@Override
	public final BasicIoTaskBuilder<I, O> setSecretSupplier(
		final BatchSupplier<String> secretSupplier
	) {
		this.secretSupplier = secretSupplier;
		if(secretSupplier == null) {
			constantSecretFlag = true;
			constantSecret = null;
		} else if(secretSupplier instanceof ConstantStringSupplier) {
			constantSecretFlag = true;
			constantSecret = secretSupplier.get();
		} else {
			constantSecretFlag = false;
		}
		return this;
	}
	
	@Override
	public BasicIoTaskBuilder<I, O> setCredentialsMap(final Map<String, String> credentials) {
		if(credentials != null) {
			this.credentialsMap = credentials;
			setSecretSupplier(null);
		}
		return this;
	}
	
	@Override @SuppressWarnings("unchecked")
	public O getInstance(final I item)
	throws IOException {
		final String uid;
		return (O) new BasicIoTask<>(
			originCode, ioType, item, inputPath, getNextOutputPath(),
			Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
		);
	}

	@Override @SuppressWarnings("unchecked")
	public List<O> getInstances(final List<I> items)
	throws IOException {
		final List<O> tasks = new ArrayList<>(items.size());
		String uid;
		for(final I item : items) {
			tasks.add(
				(O) new BasicIoTask<>(
					originCode, ioType, item, inputPath, getNextOutputPath(),
					Credential.getInstance(uid = getNextUid(), getNextSecret(uid))
				)
			);
		}
		return tasks;
	}
	
	protected final String getNextOutputPath() {
		return constantOutputPathFlag ? constantOutputPath : outputPathSupplier.get();
	}
	
	protected final String getNextUid() {
		return constantUidFlag ? constantUid : uidSupplier.get();
	}
	
	protected final String getNextSecret(final String uid) {
		if(uid != null && credentialsMap != null) {
			return credentialsMap.get(uid);
		} else if(constantSecretFlag) {
			return constantSecret;
		} else {
			return secretSupplier.get();
		}
	}
	
	@Override
	public void close()
	throws IOException {
		inputPath = null;
		if(outputPathSupplier != null) {
			outputPathSupplier.close();
			outputPathSupplier = null;
		}
		if(uidSupplier != null) {
			uidSupplier.close();
			uidSupplier = null;
		}
		if(secretSupplier != null) {
			secretSupplier.close();
			secretSupplier = null;
		}
		if(credentialsMap != null) {
			credentialsMap.clear();
			credentialsMap = null;
		}
	}
}
