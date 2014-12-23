package com.emc.mongoose.util.persist;
//
import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;
//

//
/**
 * Created by olga on 21.10.14.
 */
@Entity(name="Loads")
@IdClass(LoadEntityPK.class)
@Table(name = "load")
public class LoadEntity
implements Serializable {
	@Id
	@Column(name = "number")
	private long number;
	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "run", nullable = false)
	private RunEntity run;
	//
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "type", nullable = false)
	private LoadTypeEntity type;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "api", nullable = false)
	private ApiEntity api;
	//
	public LoadEntity(){
	}
	public LoadEntity(final RunEntity run, final LoadTypeEntity type, final long num, final ApiEntity api){
		this.run = run;
		this.number = num;
		this.type = type;
		this.api = api;
	}
	//
	public final RunEntity getRun() {
		return run;
	}
	public final void setRun(final RunEntity run) {
		this.run = run;
	}
	public final LoadTypeEntity getType() {
		return type;
	}
	public final void setType(final LoadTypeEntity type) {
		this.type = type;
	}
	public final long getNumber() {
		return number;
	}
	public final void setNumber(final long number) {
		this.number = number;
	}
	public final ApiEntity getApi() {
		return api;
	}
	public final void setApi(final ApiEntity api) {
		this.api = api;
	}
}
@Embeddable
class LoadEntityPK
implements Serializable{
	//
	private long number;
	private RunEntity run;
	//
	LoadEntityPK(){
	}
	//
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
