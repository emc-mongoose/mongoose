package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
/**
 * Created by olga on 28.10.14.
 */
@Entity
@IdClass(DataObjectEntityPK.class)
@Table(name = "dataobject")
public final class DataObjectEntity
implements Serializable{
	@Id
	@Column(name = "identifier")
	private String identifier;
	@Id
	@Column(name = "size")
	private long size;
	@Column(name = "ringOffset", updatable = true)
	private String ringOffset;
	@Column(name = "layer", updatable = true)
	private long layer;
	@Column(name = "mask", updatable = true)
	private long mask;
	//
	public DataObjectEntity(){
	}
	public DataObjectEntity(
		final String identifier, final String ringOffset, final long size,
		final long layer, final long mask)
	{
		this.identifier = identifier;
		this.ringOffset = ringOffset;
		this.layer = layer;
		this.size = size;
		this.mask = mask;
	}
	public DataObjectEntity(final String identifier, final long size){
		this.identifier = identifier;
		this.size = size;
	}
	//
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
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Data object entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
final class DataObjectEntityPK
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