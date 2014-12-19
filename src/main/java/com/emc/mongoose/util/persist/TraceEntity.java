package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
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
@Table(name = "Traces")
public final class TraceEntity
		implements Serializable{
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn(name = "dataobjectsize", nullable = false), @JoinColumn(name = "size", nullable = false) })
	private DataObjectEntity dataobject;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn(name = "num", nullable = false), @JoinColumn(name = "load", nullable = false) })
	private ConectionEntity conection;
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
	public TraceEntity(final DataObjectEntity dataobject, final ConectionEntity conection,
					   final StatusEntity status, final long tsReqStart,
					   final long reqDur){
		this.dataobject = dataobject;
		this.conection = conection;
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
	public final ConectionEntity getThread() {
		return conection;
	}
	public final void setThread(final ConectionEntity conection) {
		this.conection = conection;
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
