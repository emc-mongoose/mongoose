package com.emc.mongoose.util.persist;

import com.emc.mongoose.run.Main;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
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
public final class RunEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mode", nullable = false)
	private ModeEntity mode;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=LoadEntity.class, fetch = FetchType.LAZY, mappedBy = "run")
	private Set<LoadEntity> loadsSet = new HashSet<LoadEntity>();
	//
	public RunEntity(){
	}
	public RunEntity(ModeEntity mode, String name){
		this.mode = mode;
		this.name = name;
		mode.getRunsSet().add(this);
	}
	public RunEntity(String name){
		this.name = name;
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public ModeEntity getMode() {
		return mode;
	}
	public void setMode(ModeEntity mode) {
		this.mode = mode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Set<LoadEntity> getLoadsSet() {
		return loadsSet;
	}
	public void setLoadsSet(Set<LoadEntity> loadsSet) {
		this.loadsSet = loadsSet;
	}
}
