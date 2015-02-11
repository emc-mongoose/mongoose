package com.emc.mongoose.util.persist;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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

/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Connection")
@IdClass(ConnectionEntityPK.class)
@Table(name = "connection")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class ConnectionEntity
implements Serializable{

	@Id
	@Column(name = "num")
	private long number;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
		@JoinColumn(name = "load", nullable = false),
		@JoinColumn(name = "run", nullable = false)
	})
	private LoadEntity load;
	//
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "node", nullable = false)
	private NodeEntity node;

	public ConnectionEntity(){
	}
	public ConnectionEntity(final LoadEntity load, final NodeEntity node, final long num){
		this.load = load;
		this.node = node;
		this.number = num;
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
	public long getNumber() {
		return number;
	}
	public void setNumber(long number) {
		this.number = number;
	}
}
/////////////////////////////////
class ConnectionEntityPK
implements Serializable{
	private long number;
	private LoadEntity load;
	public ConnectionEntityPK(){
	}
	public ConnectionEntityPK( final long number, final LoadEntity loadEntity){
		this.number = number;
		this.load = loadEntity;
	}

	public long getNumber() {
		return number;
	}
	public void setNumber(long number) {
		this.number = number;
	}
	public LoadEntity getLoad() {
		return load;
	}
	public void setLoad(LoadEntity load) {
		this.load = load;
	}

	@Override
	public boolean equals(Object o) {
		if(o == null) return false;
		if(!(o instanceof ConnectionEntity)) return false;
		ConnectionEntity other = (ConnectionEntity) o;
		return (this.number == other.getNumber()) && (this.load.getNumber() == other.getLoad().getNumber())
			&& (this.load.getRun().getId() == other.getLoad().getRun().getId());

	}
	@Override
	public int hashCode() {
		int hsCode;
		hsCode = Long.valueOf(this.number).hashCode();
		hsCode = 19 * hsCode + Long.valueOf(this.load.getNumber()).hashCode();
		hsCode = 19 * hsCode + Long.valueOf(this.load.getRun().getId()).hashCode();
		return hsCode;
	}
}