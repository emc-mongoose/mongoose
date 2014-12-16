package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

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
//
import static javax.persistence.GenerationType.IDENTITY;
//
/**
 * Created by olga on 23.10.14.
 */
@Entity(name = "LevelEntity")
@Table(name = "levels", uniqueConstraints = {
	@UniqueConstraint(columnNames = "name")})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public final class LevelEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=MessageEntity.class, fetch = FetchType.LAZY, mappedBy = "level")
	private Set<MessageEntity> messageSet = new HashSet<MessageEntity>();
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
	public final Set<MessageEntity> getMessageSet() {
		return messageSet;
	}
	public final void setMessageSet(final Set<MessageEntity> messageSet) {
		this.messageSet = messageSet;
	}
}
