package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
import java.util.HashSet;
import java.util.Set;
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 17.10.14.
 */
@Entity(name = "Runs")
@Table(name = "run")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class RunEntity
		implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mode", nullable = false)
	private ModeEntity mode;
	@Column(name = "name", unique = true)
	private String name;
	//
	public RunEntity(){
	}
	public RunEntity(ModeEntity mode, String name){
		this.mode = mode;
		this.name = name;
	}
	public RunEntity(final String name){
		this.name = name;
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final ModeEntity getMode() {
		return mode;
	}
	public final void setMode(final ModeEntity mode) {
		this.mode = mode;
	}
	public final String getName() {
		return name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
}
