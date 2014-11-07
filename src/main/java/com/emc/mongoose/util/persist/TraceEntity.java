package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="TraceEntity")
@Table(name = "Traces", uniqueConstraints = {
		@UniqueConstraint(columnNames = "dataItem"),
		@UniqueConstraint(columnNames = "thread"),
		@UniqueConstraint(columnNames = "status"),
		@UniqueConstraint(columnNames = "tsReqStart"),
		@UniqueConstraint(columnNames = "reqDur")})
public final class TraceEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "dataItem", nullable = false)
	private DataObjectEntity dataitem;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "thread", nullable = false)
	private ThreadEntity thread;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "status", nullable = false)
	private StatusEntity status;
	@Column(name = "tsReqStart")
	private BigInteger tsReqStart;
	@Column(name = "reqDur")
	private BigInteger reqDur;
	//
	public TraceEntity(){
	}
	public TraceEntity(final DataObjectEntity dataitem, final ThreadEntity thread,
					   final StatusEntity status, final BigInteger tsReqStart,
					   final BigInteger reqDur){
		this.dataitem = dataitem;
		dataitem.getTraceSet().add(this);
		this.thread = thread;
		thread.getTraceSet().add(this);
		this.status  = status;
		status.getTraceSet().add(this);
		this.tsReqStart = tsReqStart;
		this.reqDur = reqDur;
	}
	//
	public final BigInteger getId() {
		return id;
	}
	public final void setId(final BigInteger id) {
		this.id = id;
	}
	public final DataObjectEntity getDataitem() {
		return dataitem;
	}
	public final void setDataitem(final DataObjectEntity dataitem) {
		this.dataitem = dataitem;
	}
	public final ThreadEntity getThread() {
		return thread;
	}
	public final void setThread(final ThreadEntity thread) {
		this.thread = thread;
	}
	public final StatusEntity getStatus() {
		return status;
	}
	public final void setStatus(final StatusEntity status) {
		this.status = status;
	}
	public final BigInteger getTsReqStart() {
		return tsReqStart;
	}
	public final void setTsReqStart(final BigInteger tsReqStart) {
		this.tsReqStart = tsReqStart;
	}
	public final BigInteger getReqDur() {
		return reqDur;
	}
	public final void setReqDur(final BigInteger reqDur) {
		this.reqDur = reqDur;
	}
}
