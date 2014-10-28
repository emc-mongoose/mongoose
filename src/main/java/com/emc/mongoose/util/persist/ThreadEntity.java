package com.emc.mongoose.util.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Thread")
@Table(name = "threads", uniqueConstraints = {
		@UniqueConstraint(columnNames = "load"),
		@UniqueConstraint(columnNames = "node"),
		@UniqueConstraint(columnNames = "num")})
public class ThreadEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "load", nullable = false)
	private LoadEntity load;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "node", nullable = false)
	private NodeEntity node;
	@Column(name = "num")
	private BigInteger num;

	//
	public ThreadEntity(){
	}
	public ThreadEntity(final LoadEntity load, final NodeEntity node, final BigInteger num){
		this.load = load;
		load.getThreadSet().add(this);
		this.node = node;
		node.getThreadSet().add(this);
		this.num = num;
	}
	//
	public BigInteger getNum() {
		return num;
	}
	public void setNum(final BigInteger num) {
		this.num = num;
	}
	public BigInteger getId() {
		return id;
	}
	public void setId(final BigInteger id) {
		this.id = id;
	}
	public LoadEntity getLoad() {
		return load;
	}
	public void setLoad(final LoadEntity load) {
		this.load = load;
	}
	public NodeEntity getNode() {
		return node;
	}
	public void setNode(final NodeEntity node) {
		this.node = node;
	}
}
