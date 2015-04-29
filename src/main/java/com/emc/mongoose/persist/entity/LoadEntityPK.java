package com.emc.mongoose.persist.entity;

import java.io.Serializable;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Load entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class LoadEntityPK
implements Serializable {
	//
	private long num;
	private long run;
	//
	public LoadEntityPK(){
	}
	public LoadEntityPK(final long number, final long runEntity){
		this.num = number;
		this.run = runEntity;
	}
	//
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public final long getRun() {
		return run;
	}
	public final void setRun(final long run) {
		this.run = run;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof LoadEntity)) return false;
		final LoadEntity other = (LoadEntity) o;
		return (this.num == other.getNum()) && (this.run == other.getRun().getId());

	}
	@Override
	public final int hashCode() {
		return (int) ( getNum() + getRun());
	}
}
