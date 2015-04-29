package com.emc.mongoose.persist.entity;
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
//
/**
 * Created by olga on 23.10.14.
 */
@Entity
@Table(name = "Level", uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
public class LevelEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@Column(name = "name")
	private String name;
	//
	public LevelEntity(){
	}
	public LevelEntity(final String name){
		this.name = name;
	}
	//
	public final String getName() {
		return name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
}