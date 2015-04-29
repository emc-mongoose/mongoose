package com.emc.mongoose.persist.entity;
//

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
//
import java.io.Serializable;
/**
 * Created by olga on 28.10.14.
 */
@Entity
@IdClass(TraceEntityPK.class)
@Table(name = "trace")
public class TraceEntity
implements Serializable{
	//
	@Id
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumns({
		@JoinColumn(name = "dataobjectId", referencedColumnName = "identifier", nullable = false),
		@JoinColumn(name = "dataobjectSize", referencedColumnName = "size", nullable = false)
	})
	private DataObjectEntity dataobject;
	@Id
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumns({
		@JoinColumn(name = "loadNum", referencedColumnName = "load", nullable = false),
		@JoinColumn(name = "runId", referencedColumnName = "run", nullable = false),
		@JoinColumn(name = "connectionNum", referencedColumnName = "num", nullable = false)
	})
	private ConnectionEntity connection;
	//
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "status", referencedColumnName = "code", nullable = false)
	private StatusEntity status;
	@Column(name = "tsReqStart", nullable = false)
	private long tsReqStart;
	@Column(name = "latency", nullable = false)
	private long latency;
	@Column(name = "reqDur", nullable = false)
	private long reqDur;
	//
	public TraceEntity(){
	}
	public TraceEntity(
		final DataObjectEntity dataobject, final ConnectionEntity connection,
		final StatusEntity status, final long tsReqStart, final long latency, final long reqDur)
	{
		this.dataobject = dataobject;
		this.connection = connection;
		this.status  = status;
		this.tsReqStart = tsReqStart;
		this.latency = latency;
		this.reqDur = reqDur;
	}
	//
	public final DataObjectEntity getDataobject() {
		return dataobject;
	}
	public final void setDataobject(final DataObjectEntity dataobject) {
		this.dataobject = dataobject;
	}
	public final ConnectionEntity getConnection() {
		return connection;
	}
	public final void setConnection(final ConnectionEntity connection) {
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
	public final long getLatency() {
		return latency;
	}
	public final void setLatency(final long latency) {
		this.latency = latency;
	}
}
