package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="StatusEntity")
@Table(name = "Statuses", uniqueConstraints = {
		@UniqueConstraint(columnNames = "name")})
public class StatusEntity
implements Serializable{
	@Id
	//@GeneratedValue(strategy = IDENTITY)
	@Column(name = "code")
	private int code;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=TraceEntity.class, fetch = FetchType.LAZY, mappedBy = "status")
	private Set<TraceEntity> traceSet = new HashSet<TraceEntity>();
	//
	public StatusEntity(){
	}
	public StatusEntity(final int code, final String name){
		this.code = code;
		this.name = name;
	}
	//
	public int getCode() {
		return code;
	}
	public void setCode(final int code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}
	public Set<TraceEntity> getTraceSet() {
		return traceSet;
	}
	public void setTraceSet(final Set<TraceEntity> traceSet) {
		this.traceSet = traceSet;
	}
}
