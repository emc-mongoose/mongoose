package com.emc.mongoose.core.impl.io.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.io.conf.SecureRequestConfig;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.core.impl.item.data.BasicDataItem;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 19.12.15.
 */
public abstract class SecureRequestConfigBase<T extends DataItem, C extends Container<T>>
extends RequestConfigBase<T, C>
implements SecureRequestConfig<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected String secret, userName;
	//
	protected SecureRequestConfigBase() {
		secret = runTimeConfig.getAuthSecret();
		userName = runTimeConfig.getAuthId();
	}
	//
	protected SecureRequestConfigBase(final SecureRequestConfigBase<T, C> reqConf2Clone) {
		super(reqConf2Clone);
		if(reqConf2Clone != null) {
			setUserName(reqConf2Clone.getUserName());
			setSecret(reqConf2Clone.getSecret());
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public SecureRequestConfigBase<T, C> clone()
	throws CloneNotSupportedException {
		final SecureRequestConfigBase<T, C>
			requestConfigBranch = (SecureRequestConfigBase<T, C>) super.clone();
		requestConfigBranch.setUserName(userName);
		requestConfigBranch.setSecret(secret);
		LOG.debug(
			Markers.MSG, "Forked conf conf #{} from #{}", requestConfigBranch.hashCode(), hashCode()
		);
		return requestConfigBranch;
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(userName);
		out.writeObject(secret);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setUserName((String) in.readObject());
		setSecret((String) in.readObject());
	}
	//
	@Override
	public final String getUserName() {
		return userName;
	}
	@Override
	public SecureRequestConfigBase<T, C> setUserName(final String userName) {
		this.userName = userName;
		return this;
	}
	//
	@Override
	public final String getSecret() {
		return secret;
	}
	@Override
	public SecureRequestConfigBase<T, C> setSecret(final String secret) {
		this.secret = secret;
		return this;
	}
	//
	@Override
	public SecureRequestConfigBase<T, C> setRunTimeConfig(final RunTimeConfig runTimeConfig) {
		super.setRunTimeConfig(runTimeConfig);
		setUserName(this.runTimeConfig.getAuthId());
		setSecret(this.runTimeConfig.getAuthSecret());
		return this;
	}
	//
	@Override
	public ItemSrc<T> getContainerListInput(final long maxCount, final String addr) {
		return null;
	}
	//
	@Override
	public Class<C> getContainerClass() {
		return null;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public Class<T> getItemClass() {
		return (Class<T>) BasicDataItem.class;
	}
}
