package com.emc.mongoose.model.storage;

import java.io.Serializable;

/**
 Created by andrey on 14.03.17.
 */
public interface Credential
extends Serializable {

	String getUid();

	String getSecret();
}
