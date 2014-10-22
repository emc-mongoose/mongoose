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
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigInteger;
//
import static javax.persistence.GenerationType.IDENTITY;
//
/**
 * Created by olga on 21.10.14.
 */
@Entity(name="Loads")
@Table(name = "Loads", uniqueConstraints = {
		@UniqueConstraint(columnNames = "run"),
		@UniqueConstraint(columnNames = "type"),
		@UniqueConstraint(columnNames = "number"),
		@UniqueConstraint(columnNames = "api")})
public class LoadEntity
implements Serializable {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private BigInteger id;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	private LoadTypeEntity type;
	@Column(name = "number")
	private BigInteger num;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "api", nullable = false)
	private ApiEntity api;
	//
	public LoadEntity(){
	}
	public LoadEntity(RunEntity run, LoadTypeEntity type, BigInteger num, ApiEntity api){
		this.run = run;
		run.getLoadsSet().add(this);
		this.type = type;
		type.getLoadsSet().add(this);
		this.num = num;
		this.api = api;
		api.getLoadsSet().add(this);
	}
	//
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public RunEntity getRun() {
		return run;
	}
	public void setRun(RunEntity run) {
		this.run = run;
	}
	public LoadTypeEntity getType() {
		return type;
	}
	public void setType(LoadTypeEntity type) {
		this.type = type;
	}
	public BigInteger getNum() {
		return num;
	}
	public void setNum(BigInteger num) {
		this.num = num;
	}
	public ApiEntity getApi() {
		return api;
	}
	public void setApi(ApiEntity api) {
		this.api = api;
	}
}
