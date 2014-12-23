package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
//
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="TraceEntity")
@IdClass(TraceEntityPK.class)
@Table(name = "trace")
public final class TraceEntity
		implements Serializable{
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "dataobject_id", nullable = false),
			@JoinColumn(name = "dataobject_size", nullable = false)
	})
	private DataObjectEntity dataobject;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "load", nullable = false),
			@JoinColumn(name = "run", nullable = false),
			@JoinColumn(name = "connection", nullable = false)
	})
	private ConnectionEntity connection;
	//
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "status", nullable = false)
	private StatusEntity status;
	@Column(name = "tsReqStart")
	private long tsReqStart;
	@Column(name = "reqDur")
	private long reqDur;
	//
	public TraceEntity(){
	}
	public TraceEntity(final DataObjectEntity dataobject, final ConnectionEntity connection,
					   final StatusEntity status, final long tsReqStart,
					   final long reqDur){
		this.dataobject = dataobject;
		this.connection = connection;
		this.status  = status;
		this.tsReqStart = tsReqStart;
		this.reqDur = reqDur;
	}
	//
	public final DataObjectEntity getDataobject() {
		return dataobject;
	}
	public final void setDataobject(final DataObjectEntity dataobject) {
		this.dataobject = dataobject;
	}
	public final ConnectionEntity getThread() {
		return connection;
	}
	public final void setThread(final ConnectionEntity connection) {
		this.connection = connection;
	}
	public final StatusEntity getStatus() {
		return status;
	}
	public final void setStatus(final StatusEntity status) {
		this.status = status;
	}
	public final long getTsReqStart() {
		return tsReqStart;
	}
	public final void setTsReqStart(final long tsReqStart) {
		this.tsReqStart = tsReqStart;
	}
	public final long getReqDur() {
		return reqDur;
	}
	public final void setReqDur(final long reqDur) {
		this.reqDur = reqDur;
	}
}
/////////////////////////////////
class TraceEntityPK
implements Serializable{
	private DataObjectEntity dataobject;
	private ConnectionEntity connection;
	//
	public TraceEntityPK(){
	}
	public TraceEntityPK(final DataObjectEntity dataObjectEntity, final ConnectionEntity connectionEntity){
		this.dataobject = dataObjectEntity;
		this.connection = connectionEntity;
	}
	//
	public DataObjectEntity getDataobject() {
		return dataobject;
	}
	public void setDataobject(DataObjectEntity dataobject) {
		this.dataobject = dataobject;
	}
	public ConnectionEntity getConnection() {
		return connection;
	}
	public void setConnection(ConnectionEntity connection) {
		this.connection = connection;
	}
}
