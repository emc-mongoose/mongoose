package com.emc.mongoose.util.persist;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Connection")
@IdClass(ConnectionEntityPK.class)
@Table(name = "connection")
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
	ConnectionEntityPK(){
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
}