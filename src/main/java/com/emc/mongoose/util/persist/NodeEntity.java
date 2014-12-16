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

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 28.10.14.
 */
@Entity(name="NodeEntity")
@Table(name = "Nodes", uniqueConstraints = {
	@UniqueConstraint(columnNames = "address")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class NodeEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
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
	public final Set<ThreadEntity> getThreadSet() {
		return threadSet;
	}
	public final void setThreadSet(final Set<ThreadEntity> threadSet) {
		this.threadSet = threadSet;
	}
}
