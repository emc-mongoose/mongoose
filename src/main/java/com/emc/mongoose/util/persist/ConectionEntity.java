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
//
import static javax.persistence.GenerationType.IDENTITY;
//
/**
 * Created by olga on 28.10.14.
 */
@Entity(name="Conection")
@Table(name = "conection")
public final class ConectionEntity
		implements Serializable{
	@Id
	@Column(name = "num")
	private long number;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "load", nullable = false)
	private LoadEntity load;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "node", nullable = false)
	private NodeEntity node;
	//
	public ConectionEntity(){
	}
	public ConectionEntity(final LoadEntity load, final NodeEntity node, final long num){
		this.load = load;
		this.node = node;
		this.number = num;
	}
	//
	public final LoadEntity getLoad() {
		return load;
	}
	public final void setLoad(final LoadEntity load) {
		this.load = load;
	}
	public final NodeEntity getNode() {
		return node;
	}
	public final void setNode(final NodeEntity node) {
		this.node = node;
	}
	public long getNumber() {
		return number;
	}
	public void setNumber(long number) {
		this.number = number;
	}
	public RunEntity getRun() {
		return run;
	}
	public void setRun(RunEntity run) {
		this.run = run;
	}
}
