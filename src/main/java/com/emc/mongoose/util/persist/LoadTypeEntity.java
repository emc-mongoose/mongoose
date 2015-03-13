package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
//
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 21.10.14.
 */
@Entity
@Table(name = "LoadType", uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
public final class LoadTypeEntity
	implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@Column(name = "name")
	private String name;
	//
	public LoadTypeEntity(){
	}
	public LoadTypeEntity(final String name){
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
}
