package com.emc.mongoose.util.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 17.10.14.
 */
@Entity(name = "Runs")
@Table(name = "runs", uniqueConstraints = {
		@UniqueConstraint(columnNames = "mode"),
		@UniqueConstraint(columnNames = "name")})
public final class Runs
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mode", nullable = false)
	private Modes mode;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=Loads.class, fetch = FetchType.LAZY, mappedBy = "run")
	private Set<Loads> loadsSet = new HashSet<Loads>();
	//
	public Runs(){
	}
	public Runs(Modes mode,String name){
		this.mode = mode;
		this.name = name;
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public Modes getMode() {
		return mode;
	}
	public void setMode(Modes mode) {
		this.mode = mode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<Loads> getLoadsSet() {
		return loadsSet;
	}
	public void setLoadsSet(Set<Loads> loadsSet) {
		this.loadsSet = loadsSet;
	}
}
