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
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.util.Date;
import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 17.10.14.
 */
@Entity
@Table(name = "run", uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "tstamp"})})
public final class RunEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "mode", referencedColumnName = "id", nullable = false)
	private ModeEntity mode;
	@Column(name = "name")
	private String name;
	@Column(name = "tstamp")
	private Date timestamp;
	//
	public RunEntity(){
	}
	public RunEntity(final ModeEntity mode, final  String name, final Date tstamp){
		this.mode = mode;
		this.name = name;
		this.timestamp = tstamp;
	}
	//
	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
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
	public final Date getTimestamp() {
		return timestamp;
	}
	public final void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}
}