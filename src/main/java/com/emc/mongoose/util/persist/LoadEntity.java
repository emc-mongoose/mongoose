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
//
import static javax.persistence.GenerationType.IDENTITY;
//
/**
 * Created by olga on 21.10.14.
 */
@Entity(name="Loads")
@Table(name = "Loads", uniqueConstraints = {
	@UniqueConstraint(columnNames = "run"),
	@UniqueConstraint(columnNames = "type"),
	@UniqueConstraint(columnNames = "number"),
	@UniqueConstraint(columnNames = "api")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoadEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	private LoadTypeEntity type;
	@Column(name = "number")
	private long num;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "api", nullable = false)
	private ApiEntity api;
	@OneToMany(targetEntity=ThreadEntity.class, fetch = FetchType.LAZY, mappedBy = "load")
	private Set<ThreadEntity> threadSet = new HashSet<ThreadEntity>();
	//
	public LoadEntity(){
	}
	public LoadEntity(final RunEntity run, final LoadTypeEntity type, final long num, final ApiEntity api){
		this.run = run;
		run.getLoadsSet().add(this);
		this.type = type;
		type.getLoadsSet().add(this);
		this.num = num;
		this.api = api;
		api.getLoadsSet().add(this);
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final RunEntity getRun() {
		return run;
	}
	public final void setRun(final RunEntity run) {
		this.run = run;
	}
	public final LoadTypeEntity getType() {
		return type;
	}
	public final void setType(final LoadTypeEntity type) {
		this.type = type;
	}
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public final ApiEntity getApi() {
		return api;
	}
	public final void setApi(final ApiEntity api) {
		this.api = api;
	}
	public final Set<ThreadEntity> getThreadSet() {
		return threadSet;
	}
	public final void setThreadSet(Set<ThreadEntity> threadSet) {
		this.threadSet = threadSet;
	}
}
