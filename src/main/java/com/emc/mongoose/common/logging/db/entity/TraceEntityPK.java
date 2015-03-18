package com.emc.mongoose.common.logging.db.entity;

import com.emc.mongoose.common.logging.db.entity.ConnectionEntityPK;
import com.emc.mongoose.common.logging.db.entity.DataObjectEntityPK;
import com.emc.mongoose.common.logging.db.entity.TraceEntity;

import java.io.Serializable;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Trace Entity Primary Key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class TraceEntityPK
implements Serializable {
	private DataObjectEntityPK dataobject;
	private ConnectionEntityPK connection;
	//
	public TraceEntityPK(){
	}
	public TraceEntityPK(final DataObjectEntityPK dataObjectEntity, final ConnectionEntityPK connectionEntity){
		this.dataobject = dataObjectEntity;
		this.connection = connectionEntity;
	}
	//
	public final DataObjectEntityPK getDataobject() {
		return dataobject;
	}
	public final void setDataobject(final DataObjectEntityPK dataobject) {
		this.dataobject = dataobject;
	}
	public final ConnectionEntityPK getConnection() {
		return connection;
	}
	public final void setConnection(final ConnectionEntityPK connection) {
		this.connection = connection;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof TraceEntity)) return false;
		final TraceEntity other = (TraceEntity) o;
		return (this.dataobject.getIdentifier().equals(other.getDataobject().getIdentifier()))
			&& (this.dataobject.getSize() == other.getDataobject().getSize()
			&& (this.connection.getNum() == other.getConnection().getNum())
			&& (this.connection.getLoad().getNum() == other.getConnection().getLoad().getNum())
			&& (this.connection.getLoad().getRun() == other.getConnection().getLoad().getRun().getId()));
	}
	@Override
	public final int hashCode() {
		return (this.dataobject.hashCode() + this.connection.hashCode());
	}
}
