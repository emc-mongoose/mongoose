package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 16.10.14.
 */
@Entity(name="Modes")
@Table(name = "modes")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class ModeEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true)
	private long id;
	@Column(name = "name", unique = true)
	private String name;
	@OneToMany(targetEntity=RunEntity.class, fetch = FetchType.LAZY, mappedBy = "mode")
	private Set<RunEntity> runsSet = new HashSet<RunEntity>();
	//
	public ModeEntity(){
	}
	public ModeEntity(final String name){
		this.name = name;
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final String getName() {
		return name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
	public final Set<RunEntity> getRunsSet() {
		return runsSet;
	}
	public final void setRunsSet(final Set<RunEntity> runsSet) {
		this.runsSet = runsSet;
	}
}
