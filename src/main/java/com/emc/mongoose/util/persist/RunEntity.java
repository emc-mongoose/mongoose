package com.emc.mongoose.util.persist;
//
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
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 17.10.14.
 */
@Entity(name = "Runs")
@Table(name = "runs", uniqueConstraints = {
		@UniqueConstraint(columnNames = "mode"),
		@UniqueConstraint(columnNames = "name")})
public final class RunEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mode", nullable = false)
	private ModeEntity mode;
	@Column(name = "name")
	private String name;
	@OneToMany(targetEntity=LoadEntity.class, fetch = FetchType.LAZY, mappedBy = "run")
	private Set<LoadEntity> loadsSet = new HashSet<LoadEntity>();
	@OneToMany(targetEntity=MessageEntity.class, fetch = FetchType.LAZY, mappedBy = "run")
	private Set<MessageEntity> messageSet = new HashSet<MessageEntity>();
	//
	public RunEntity(){
	}
	public RunEntity(ModeEntity mode, String name){
		this.mode = mode;
		this.name = name;
		mode.getRunsSet().add(this);
	}
	public RunEntity(final String name){
		this.name = name;
	}
	//
	public final BigInteger getId() {
		return id;
	}
	public final void setId(final BigInteger id) {
		this.id = id;
	}
	public final ModeEntity getMode() {
		return mode;
	}
	public final void setMode(final ModeEntity mode) {
		this.mode = mode;
	}
	public final String getName() {
		return name;
	}
	public final void setName(final String name) {
		this.name = name;
	}
	public final Set<LoadEntity> getLoadsSet() {
		return loadsSet;
	}
	public final void setLoadsSet(final Set<LoadEntity> loadsSet) {
		this.loadsSet = loadsSet;
	}
	public final Set<MessageEntity> getMessageSet() {
		return messageSet;
	}
	public final void setMessageSet(final Set<MessageEntity> messageSet) {
		this.messageSet = messageSet;
	}
}
