package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.io.conf.SSRequestConfig;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.data.BasicDataItem;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 22.12.15.
 */
public class BasicSSRequestConfig<T extends DataItem>
extends RequestConfigBase<T, Container<T>>
implements SSRequestConfig<T> {
	//
	protected int partStart, partEnd;
	//
	public BasicSSRequestConfig() {
		port = runTimeConfig.getSSPort();
		partStart = runTimeConfig.getSSPartitionStart();
		partEnd = runTimeConfig.getSSPartitionEnd();
	}
	//
	public BasicSSRequestConfig(final BasicSSRequestConfig<T> ssReqConf) {
		super(ssReqConf);
		if(ssReqConf != null) {
			setPort(ssReqConf.getPort());
			setStartPartition(ssReqConf.getStartPartition());
			setEndPartition(ssReqConf.getEndPartition());
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public BasicSSRequestConfig<T> clone()
	throws CloneNotSupportedException {
		final BasicSSRequestConfig<T> ssReqConf = (BasicSSRequestConfig<T>) super.clone();
		ssReqConf.setStartPartition(getStartPartition());
		ssReqConf.setEndPartition(getEndPartition());
		return ssReqConf;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(getStartPartition());
		out.writeInt(getEndPartition());
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setStartPartition(in.readInt());
		setEndPartition(in.readInt());
	}
	//
	@Override
	public int getStartPartition() {
		return partStart;
	}
	//
	@Override
	public BasicSSRequestConfig<T> setStartPartition(final int n) {
		partStart = n;
		return this;
	}
	//
	@Override
	public int getEndPartition() {
		return partEnd;
	}
	//
	@Override
	public BasicSSRequestConfig<T> setEndPartition(final int n) {
		partEnd = n;
		return this;
	}
	//
	@Override
	public BasicSSRequestConfig<T> setRunTimeConfig(final RunTimeConfig rtConfig) {
		//
		super.setRunTimeConfig(rtConfig);
		//
		setPort(rtConfig.getSSPort());
		//
		int n = rtConfig.getSSPartitionStart();
		if(n < 0) {
			throw new IllegalArgumentException("Start partition number shouldn't be less than 0");
		} else {
			setStartPartition(n);
		}
		//
		n = rtConfig.getSSPartitionEnd();
		if(n < partStart) {
			throw new IllegalArgumentException(
				"End partition number shouldn't be less than start " + partStart
			);
		} else {
			setEndPartition(n);
		}
		//
		return this;
	}
	//
	@Override
	public ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return null;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<Container<T>> getContainerClass() {
		return (Class) BasicContainer.class;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<T> getItemClass() {
		return (Class<T>) BasicDataItem.class;
	}
	//
	@Override
	public void configureStorage(final String[] storageAddrs)
	throws IllegalStateException {

	}
}
