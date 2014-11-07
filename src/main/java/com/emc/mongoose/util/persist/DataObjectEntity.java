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
	private BigInteger id;
	@Column(name = "identifier")
	private String identifier;
	@Column(name = "ringOffset")
	private String ringOffset;
	@Column(name = "size")
	private BigInteger size;
	@Column(name = "layer")
	private BigInteger layer;
	@Column(name = "mask")
	private BigInteger mask;
	@OneToMany(targetEntity=TraceEntity.class, fetch = FetchType.LAZY, mappedBy = "dataitem")
	private Set<TraceEntity> traceSet = new HashSet<TraceEntity>();
	//
	public DataObjectEntity(){
	}
	public DataObjectEntity(final String identifier, final String ringOffset, final BigInteger size, final BigInteger layer, final BigInteger mask){
		this.identifier = identifier;
		this.ringOffset = ringOffset;
		this.layer = layer;
		this.size = size;
		this.mask = mask;
	}
	//
	public final BigInteger getId() {
		return id;
	}
	public final void setId(final BigInteger id) {
		this.id = id;
	}
	public final BigInteger getSize() {
		return size;
	}
	public final void setSize(final BigInteger size) {
		this.size = size;
	}
	public final BigInteger getLayer() {
		return layer;
	}
	public final void setLayer(final BigInteger layer) {
		this.layer = layer;
	}
	public final BigInteger getMask() {
		return mask;
	}
	public final void setMask(final BigInteger mask) {
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
