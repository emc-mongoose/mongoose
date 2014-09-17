package com.emc.mongoose.remote;
//
import com.emc.mongoose.data.UniformData;
import com.emc.mongoose.data.UniformDataSource;
//
import java.rmi.RemoteException;
import java.util.List;
/**
 Created by kurila on 25.06.14.
 */
public interface RecordFrameBuffer<T extends UniformData> {
	//
	List<T> takeFrame()
	throws RemoteException;
	//
}
