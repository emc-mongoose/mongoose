package com.emc.mongoose.util.persist;

import com.emc.mongoose.run.Main;
import org.hibernate.Query;

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
 * Created by olga on 17.10.14.
 */
@Entity(name="API")
@Table(name = "API", uniqueConstraints = {
		@UniqueConstraint(columnNames = "name")})
public class ApiEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=LoadEntity.class, fetch = FetchType.LAZY, mappedBy = "api")
	private Set<LoadEntity> loadsSet = new HashSet<LoadEntity>();
	//
	public ApiEntity(){
	}
	public ApiEntity(final String name){
		this.name = name;
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(final BigInteger id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public Set<LoadEntity> getLoadsSet() {
		return loadsSet;
	}
	public void setLoadsSet(final Set<LoadEntity> loadsSet) {
		this.loadsSet = loadsSet;
	}
}
