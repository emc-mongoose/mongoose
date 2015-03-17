package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Status")
@Table(name = "status", uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
public final class StatusEntity
implements Serializable{
	@Id
	@Column(name = "code")
	private int code;
	@Column(name = "name")
	private String name;
	//
	public StatusEntity(){
	}
	public StatusEntity(final int code, final String name){
		this.code = code;
		this.name = name;
	}
	//
	public final int getCode() {
		return code;
	}
	public final void setCode(final int code) {
		this.code = code;
	}
	public final String getName() {
		return name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
}

