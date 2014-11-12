package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="DataItem")
@Table(name = "dataitems", uniqueConstraints = {
		@UniqueConstraint(columnNames =	"identifier"),
		@UniqueConstraint(columnNames = "ringOffset"),
		@UniqueConstraint(columnNames = "size"),
		@UniqueConstraint(columnNames = "layer"),
		@UniqueConstraint(columnNames = "mask")})
public final class DataObjectEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@Column(name = "identifier")
	private String identifier;
	@Column(name = "ringOffset")
	private String ringOffset;
	@Column(name = "size")
	private long size;
	@Column(name = "layer")
	private long layer;
	@Column(name = "mask")
	private long mask;
	@OneToMany(targetEntity=TraceEntity.class, fetch = FetchType.LAZY, mappedBy = "dataitem")
	private Set<TraceEntity> traceSet = new HashSet<TraceEntity>();
	//
	public DataObjectEntity(){
	}
	public DataObjectEntity(final String identifier, final String ringOffset, final long size, final long layer, final long mask){
		this.identifier = identifier;
		this.ringOffset = ringOffset;
		this.layer = layer;
		this.size = size;
		this.mask = mask;
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final long getSize() {
		return size;
	}
	public final void setSize(final long size) {
		this.size = size;
	}
	public final long getLayer() {
		return layer;
	}
	public final void setLayer(final long layer) {
		this.layer = layer;
	}
	public final long getMask() {
		return mask;
	}
	public final void setMask(final long mask) {
		this.mask = mask;
	}
	public final Set<TraceEntity> getTraceSet() {
		return traceSet;
	}
	public final void setTraceSet(final Set<TraceEntity> traceSet) {
		this.traceSet = traceSet;
	}
	public final String getIdentifier() {
		return identifier;
	}
	public final void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}
	public final String getRingOffset() {
		return ringOffset;
	}
	public final void setRingOffset(final String ringOffset) {
		this.ringOffset = ringOffset;
	}
}
