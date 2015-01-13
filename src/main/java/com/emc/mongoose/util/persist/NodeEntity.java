package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="NodeEntity")
@Table(name = "node")
public final class NodeEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@Column(name = "address", unique = true)
	private String address;
	//
	public NodeEntity(){
	}
	public NodeEntity(final String addr){
		this.address = addr;
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final String getAddress() {
		return address;
	}
	public final void setAddress(final String address) {
		this.address = address;
	}
}
