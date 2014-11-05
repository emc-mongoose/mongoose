package com.emc.mongoose.util.persist;
//
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

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 28.10.14.
 */
@Entity(name="NodeEntity")
@Table(name = "Nodes", uniqueConstraints = {
		@UniqueConstraint(columnNames = "address")})
public class NodeEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@Column(name = "address")
	private String address;
	@OneToMany(targetEntity=ThreadEntity.class, fetch = FetchType.LAZY, mappedBy = "node")
	private Set<ThreadEntity> threadSet = new HashSet<ThreadEntity>();
	//
	public NodeEntity(){
	}
	public NodeEntity(final String addr){
		this.address = addr;
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(final BigInteger id) {
		this.id = id;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(final String address) {
		this.address = address;
	}
	public Set<ThreadEntity> getThreadSet() {
		return threadSet;
	}
	public void setThreadSet(final Set<ThreadEntity> threadSet) {
		this.threadSet = threadSet;
	}
}
