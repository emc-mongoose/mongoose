package com.emc.mongoose.util.persist;
//
import org.hibernate.annotations.ForeignKey;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import static javax.persistence.GenerationType.AUTO;

/**
 * Created by olga on 23.10.14.
 */
@Entity(name = "MessageEntity")
@IdClass(MessageEntityPK.class)
@Table(name = "message")
public final class MessageEntity
		implements Serializable{
	//
	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = AUTO)
	private long id;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	//
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "level", nullable = false)
	private LevelEntity level;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "class", nullable = false)
	private MessageClassEntity classMessage;
	@Temporal( TemporalType.TIMESTAMP )
	@Column(name = "tstamp")
	private Date tstamp;
	@Column(name = "message")
	private String message;
	//
	public MessageEntity(){
	}
	public MessageEntity(final RunEntity runEntity, final MessageClassEntity classMessage, final LevelEntity level,
						 final String messsage, final Date tstamp){
		this.run = runEntity;
		this.classMessage = classMessage;
		this.level = level;
		this.message = messsage;
		this.tstamp = tstamp;
	}
	//
	public final LevelEntity getLevel() {
		return level;
	}
	public final void setLevel(final LevelEntity level) {
		this.level = level;
	}
	public final MessageClassEntity getClassMessage() {
		return classMessage;
	}
	public final void setClassMessage(final MessageClassEntity className) {
		this.classMessage = className;
	}
	public final Date getTstamp() {
		return tstamp;
	}
	public final void setTstamp(final Date tstamp) {
		this.tstamp = tstamp;
	}
	public final String getMessage() {
		return message;
	}
	public final void setMessage(final String message) {
		this.message = message;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public RunEntity getRun() {
		return run;
	}
	public void setRun(RunEntity run) {
		this.run = run;
	}
}

/////////////////////////////////////////////
@Embeddable
class MessageEntityPK implements Serializable {
	//
	private long id;
	private RunEntity run;
	//
	public MessageEntityPK(){}
	//
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public RunEntity getRun() {
		return run;
	}
	public void setRun(RunEntity run) {
		this.run = run;
	}
}