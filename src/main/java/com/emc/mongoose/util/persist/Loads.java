package com.emc.mongoose.util.persist;

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
import java.math.BigInteger;

import static javax.persistence.GenerationType.IDENTITY;

/**
 * Created by olga on 21.10.14.
 */
@Entity(name="Loads")
@Table(name = "Loads", uniqueConstraints = {
		@UniqueConstraint(columnNames = "run"),
		@UniqueConstraint(columnNames = "type"),
		//@UniqueConstraint(columnNames = "num"),
		@UniqueConstraint(columnNames = "api")})
public class Loads
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private Runs run;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	private LoadType type;
	//@Column(name = "num")
	//private BigInteger num;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "api", nullable = false)
	private Api api;
	//
	public Loads(){
	}
	public Loads(Runs run, LoadType type, Api api){
		this.run = run;
		this.type = type;
		this.api = api;
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public Runs getRun() {
		return run;
	}
	public void setRun(Runs run) {
		this.run = run;
	}
	public LoadType getType() {
		return type;
	}
	public void setType(LoadType type) {
		this.type = type;
	}
	/*
	public BigInteger getNam() {
		return nam;
	}
	public void setNam(BigInteger nam) {
		this.nam = nam;
	}**/
	public Api getApi() {
		return api;
	}
	public void setApi(Api api) {
		this.api = api;
	}
}
