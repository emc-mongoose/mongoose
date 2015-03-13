package com.emc.mongoose.util.persist;
//
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
//
/**
 * Created by olga on 21.10.14.
 */
@Entity
@IdClass(LoadEntityPK.class)
@Table(name = "load")
public final class LoadEntity
implements Serializable {
	//
	@Id
	@Column(name = "num")
	private long num;
	@Id
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "run", referencedColumnName = "id", nullable = false)
	private RunEntity run;
	//
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "type", referencedColumnName = "id", nullable = false)
	private LoadTypeEntity type;
	//
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "api", referencedColumnName = "id", nullable = false)
	private ApiEntity api;
	//
	public LoadEntity(){
	}
	public LoadEntity(final RunEntity run, final LoadTypeEntity type, final long num, final ApiEntity api){
		this.run = run;
		this.num = num;
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
	public final long getNum() {
		return num;
	}
	public final void setNum(final long num) {
		this.num = num;
	}
	public final ApiEntity getApi() {
		return api;
	}
	public final void setApi(final ApiEntity api) {
		this.api = api;
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Load entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class LoadEntityPK
implements Serializable{
	//
	private long num;
	private long run;
	//
	public LoadEntityPK(){
	}
	public LoadEntityPK(final long number, final long runEntity){
		this.num = number;
		this.run = runEntity;
	}
	//
	public final long getNum() {
		return num;
	}
	public final void setNum(long num) {
		this.num = num;
	}
	public final long getRun() {
		return run;
	}
	public final void setRun(long run) {
		this.run = run;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean equals(final Object o) {
		if(o == null) return false;
		if(!(o instanceof LoadEntity)) return false;
		final LoadEntity other = (LoadEntity) o;
		return (this.num == other.getNum()) && (this.run == other.getRun().getId());

	}
	@Override
	public final int hashCode() {
		return (int) ( getNum() + getRun());
	}
}