package com.emc.mongoose.persist.entity;
//

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Date;
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 17.10.14.
 */
@Entity
@Table(name = "run", uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})})
public class RunEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "mode", referencedColumnName = "id")
	private ModeEntity mode;
	@Column(name = "name")
	private String name;
	//
	public RunEntity(){
	}
	public RunEntity(final String name){
		this.name = name;
	}
	public RunEntity(final ModeEntity mode, final  String name){
		this.mode = mode;
		this.name = name;
	}
	//
	public final long getId() {
		return this.id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final ModeEntity getMode() {
		return this.mode;
	}
	public final void setMode(final ModeEntity mode) {
		this.mode = mode;
	}
	public final String getName() {
		return this.name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
}