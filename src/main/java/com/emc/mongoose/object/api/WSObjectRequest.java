package com.emc.mongoose.object.api;
//
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.http.client.ResponseHandler;
/**
 Created by kurila on 29.09.14.
 */
public interface WSObjectRequest<T extends WSDataObject>
extends DataObjectRequest<T>, ResponseHandler<WSObjectRequest<T>> {
}
