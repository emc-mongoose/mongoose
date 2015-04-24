package com.emc.mongoose.persist.db.entity;

import java.io.Serializable;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Connection entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class ConnectionEntityPK
implements Serializable {
	//
	private long num;
	private LoadEntityPK load;
	//
	public ConnectionEntityPK(){
	}
	public ConnectionEntityPK(final long number, final LoadEntityPK load){
		this.num = number;
		this.load = load;
	}
	//
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public final LoadEntityPK getLoad() {
		return load;
	}
	public final void setLoad(final LoadEntityPK load) {
		this.load = load;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof ConnectionEntity)) return false;
		final ConnectionEntity other = (ConnectionEntity) o;
		return (this.num == other.getNum()) && (this.load.getNum() == other.getLoad().getNum()
		&& (this.load.getRun() == other.getLoad().getRun().getId()));

	}
	@Override
	public final int hashCode() {
		return (int) (getNum()+ load.getNum()+load.getRun());
	}
}
