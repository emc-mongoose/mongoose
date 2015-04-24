package com.emc.mongoose.persist.db.entity;

import java.io.Serializable;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Data object entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class DataObjectEntityPK
implements Serializable {
	//
	private String identifier;
	private long size;
	//
	public DataObjectEntityPK(){}
	public DataObjectEntityPK(final String identifier, final long size){
		this.identifier = identifier;
		this.size = size;
	}
	//
	public final String getIdentifier() {
		return identifier;
	}
	public final void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}
	public final long getSize() {
		return size;
	}
	public final void setSize(final long size) {
		this.size = size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof DataObjectEntity)) return false;
		final DataObjectEntity other = (DataObjectEntity) o;
		return (this.identifier.equals(other.getIdentifier())) && (this.size == other.getSize());

	}
	@Override
	public final int hashCode() {
		return (int) (this.size + this.identifier.hashCode());
	}
}
