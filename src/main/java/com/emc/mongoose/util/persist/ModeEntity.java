package com.emc.mongoose.util.persist;
//
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
@Table(name = "modes", uniqueConstraints = {
		@UniqueConstraint(columnNames = "id"),
		@UniqueConstraint(columnNames = "name")})
public final class ModeEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@Column(name = "name")
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
	public final BigInteger getId() {
		return id;
	}
	public final void setId(final BigInteger id) {
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
