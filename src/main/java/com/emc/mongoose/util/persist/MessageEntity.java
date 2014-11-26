package com.emc.mongoose.util.persist;
//
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 23.10.14.
 */
@Entity(name = "MessageEntity")
@Table(name = "messages", uniqueConstraints = {
	@UniqueConstraint(columnNames = "run"),
	@UniqueConstraint(columnNames = "level"),
	@UniqueConstraint(columnNames = "class"),
	@UniqueConstraint(columnNames = "tstamp"),
	@UniqueConstraint(columnNames = "message")})
public final class MessageEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
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
	public MessageEntity(final RunEntity run, final MessageClassEntity classMessage, final LevelEntity level,
			final String messsage, final Date tstamp){
		this.run = run;
		this.classMessage = classMessage;
		this.level = level;
		this.message = messsage;
		this.tstamp = tstamp;
		run.getMessageSet().add(this);
		classMessage.getMessageSet().add(this);
		level.getMessageSet().add(this);
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
}
