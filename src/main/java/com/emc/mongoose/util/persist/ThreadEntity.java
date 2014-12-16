package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
//
import static javax.persistence.GenerationType.IDENTITY;
//
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Thread")
@Table(name = "threads", uniqueConstraints = {
	@UniqueConstraint(columnNames = "load"),
	@UniqueConstraint(columnNames = "node"),
	@UniqueConstraint(columnNames = "num")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class ThreadEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "load", nullable = false)
	private LoadEntity load;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "node", nullable = false)
	private NodeEntity node;
	@Column(name = "num")
	private long num;
	@OneToMany(targetEntity=TraceEntity.class, fetch = FetchType.LAZY, mappedBy = "thread")
	private Set<TraceEntity> traceSet = new HashSet<TraceEntity>();
	//
	public ThreadEntity(){
	}
	public ThreadEntity(final LoadEntity load, final NodeEntity node, final long num){
		this.load = load;
		load.getThreadSet().add(this);
		this.node = node;
		node.getThreadSet().add(this);
		this.num = num;
	}
	//
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final LoadEntity getLoad() {
		return load;
	}
	public final void setLoad(final LoadEntity load) {
		this.load = load;
	}
	public final NodeEntity getNode() {
		return node;
	}
	public final void setNode(final NodeEntity node) {
		this.node = node;
	}
	public final Set<TraceEntity> getTraceSet() {
		return traceSet;
	}
	public final void setTraceSet(final Set<TraceEntity> traceSet) {
		this.traceSet = traceSet;
	}
}
