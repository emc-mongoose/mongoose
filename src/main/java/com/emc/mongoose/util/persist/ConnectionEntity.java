package com.emc.mongoose.util.persist;
//
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.io.Serializable;
//
/**
 * Created by olga on 28.10.14.
 */
@Entity
@IdClass(ConnectionEntityPK.class)
@Table(name = "connection")
public class ConnectionEntity
implements Serializable{
	@Id
	@Column(name = "num")
	private long num;
	@Id
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumns ({
		@JoinColumn(name = "load", referencedColumnName = "num", nullable = false),
		@JoinColumn(name = "run", referencedColumnName = "run", nullable = false)
	})
	private LoadEntity load;
	//
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "node", referencedColumnName = "id", nullable = false)
	private NodeEntity node;
	//
	public ConnectionEntity(){
	}
	public ConnectionEntity(final LoadEntity load, final NodeEntity node, final long num){
		this.load = load;
		this.node = node;
		this.num = num;
	}
	//
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

	public final long getNum() {
		return num;
	}

	public final void setNum(final long num) {
		this.num = num;
	}
}
/////////////////////////////////
class ConnectionEntityPK
implements Serializable{
	//
	private long num;
	private LoadEntityPK load;
	//
	public ConnectionEntityPK(){
	}
	public ConnectionEntityPK(final long number, final LoadEntityPK load){
		this.num = number;
		this.load = load;
	}
	//
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public LoadEntityPK getLoad() {
		return load;
	}
	public void setLoad(LoadEntityPK load) {
		this.load = load;
	}
	///////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof ConnectionEntity)) return false;
		final ConnectionEntity other = (ConnectionEntity) o;
		return (this.num == other.getNum()) && (this.load.getNum() == other.getLoad().getNum()
		&& (this.load.getRun() == other.getLoad().getRun().getId()));

	}
	@Override
	public final int hashCode() {
		return (int) (getNum()+ load.getNum()+load.getRun());
	}
}
