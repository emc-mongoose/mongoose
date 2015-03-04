package com.emc.mongoose.util.persist;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 23.10.14.
 */
@Entity
@Table(name = "message")
public class MessageEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "level", nullable = false)
	private LevelEntity level;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "class", nullable = false)
	private MessageClassEntity messageClass;
	@Temporal( TemporalType.TIMESTAMP )
	@Column(name = "tstamp")
	private Date tstamp;
	@Column(name = "message")
	private String message;
	//
	public MessageEntity(){
	}
	public MessageEntity(
		final RunEntity run, final MessageClassEntity messageClass,
		final LevelEntity level, final String messsage, final Date tstamp)
	{
		this.run = run;
		this.messageClass = messageClass;
		this.level = level;
		this.message = messsage;
		this.tstamp = tstamp;
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
	public final MessageClassEntity getMessageClass() {
		return messageClass;
	}
	public final void setMessageClass(final MessageClassEntity className) {
		this.messageClass = className;
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
