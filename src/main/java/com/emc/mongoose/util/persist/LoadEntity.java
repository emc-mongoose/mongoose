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
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumn(name = "api", referencedColumnName = "id", nullable = false)
	private ApiEntity api;
	@Column(name = "countSucc")
	private long countSucc;
	@Column(name = "countFail")
	private long countFail;
	@Column(name = "latencyAvg")
	private int latencyAvg;
	@Column(name = "latencyMin")
	private int latencyMin;
	@Column(name = "latencyMed")
	private int latencyMed;
	@Column(name = "latencyMax")
	private int latencyMax;
	@Column(name = "meanTP")
	private double meanTP;
	@Column(name = "oneMinTP")
	private double oneMinTP;
	@Column(name = "fiveMinTP")
	private double fiveMinTP;
	@Column(name = "fifteenMinTP")
	private double fifteenMinTP;
	@Column(name = "meanBW")
	private double meanBW;
	@Column(name = "oneMinBW")
	private double oneMinBW;
	@Column(name = "fiveMinBW")
	private double fiveMinBW;
	@Column(name = "fifteenMinBW")
	private double fifteenMinBW;
	//
	public LoadEntity(){
	}
	public LoadEntity(final RunEntity run, final LoadTypeEntity type, final long num, final ApiEntity api){
		this.run = run;
		this.num = num;
		this.type = type;
		this.api = api;
	}
	public LoadEntity(
		final RunEntity run, final LoadTypeEntity type, final long num, final ApiEntity api,
		final long countSucc, final long countFail,
		final int latencyAvg, final int latencyMin, final int latencyMed, final int latencyMax,
		final double meanTP, final double oneMinTP, final double fiveMinTP, final double fifteenMinTP,
		final double meanBW, final double oneMinBW, final double fiveMinBW, final double fifteenMinBW){
		this.run = run;
		this.num = num;
		this.type = type;
		this.api = api;
		this.countSucc = countSucc;
		this.countFail = countFail;
		this.latencyAvg = latencyAvg;
		this.latencyMin = latencyMin;
		this.latencyMed = latencyMed;
		this.latencyMax = latencyMax;
		this.meanTP = meanTP;
		this.oneMinTP = oneMinTP;
		this.fiveMinTP = fiveMinTP;
		this.fifteenMinTP = fifteenMinTP;
		this.meanBW = meanBW;
		this.fiveMinBW = fiveMinBW;
		this.fifteenMinBW = fifteenMinBW;
	}
	//
	public final void setPerfomance(
		final long countSucc, final long countFail,
		final int latencyAvg, final int latencyMin, final int latencyMed, final int latencyMax,
		final double meanTP, final double oneMinTP, final double fiveMinTP, final double fifteenMinTP,
		final double meanBW, final double oneMinBW, final double fiveMinBW, final double fifteenMinBW
	){
		this.countSucc = countSucc;
		this.countFail = countFail;
		this.latencyAvg = latencyAvg;
		this.latencyMin = latencyMin;
		this.latencyMed = latencyMed;
		this.latencyMax = latencyMax;
		this.meanTP = meanTP;
		this.oneMinTP = oneMinTP;
		this.fiveMinTP = fiveMinTP;
		this.fifteenMinTP = fifteenMinTP;
		this.meanBW = meanBW;
		this.fiveMinBW = fiveMinBW;
		this.fifteenMinBW = fifteenMinBW;
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
	public final long getCountSucc() {
		return countSucc;
	}
	public final void setCountSucc(final long countSucc) {
		this.countSucc = countSucc;
	}
	public final long getCountFail() {
		return countFail;
	}
	public final void setCountFail(final long countFail) {
		this.countFail = countFail;
	}
	public final int getLatencyAvg() {
		return latencyAvg;
	}
	public final void setLatencyAvg(final int latencyAvg) {
		this.latencyAvg = latencyAvg;
	}
	public final int getLatencyMin() {
		return latencyMin;
	}
	public final void setLatencyMin(final int latencyMin) {
		this.latencyMin = latencyMin;
	}
	public final int getLatencyMed() {
		return latencyMed;
	}
	public final void setLatencyMed(final int latencyMed) {
		this.latencyMed = latencyMed;
	}
	public final int getLatencyMax() {
		return latencyMax;
	}
	public final void setLatencyMax(final int latencyMax) {
		this.latencyMax = latencyMax;
	}
	public final double getMeanTP() {
		return meanTP;
	}
	public final void setMeanTP(final double meanTP) {
		this.meanTP = meanTP;
	}
	public final double getOneMinTP() {
		return oneMinTP;
	}
	public final void setOneMinTP(final double oneMinTP) {
		this.oneMinTP = oneMinTP;
	}
	public final double getFiveMinTP() {
		return fiveMinTP;
	}
	public final void setFiveMinTP(final double fiveMinTP) {
		this.fiveMinTP = fiveMinTP;
	}
	public final double getFifteenMinTP() {
		return fifteenMinTP;
	}
	public final void setFifteenMinTP(final double fifteenMinTP) {
		this.fifteenMinTP = fifteenMinTP;
	}
	public final double getMeanBW() {
		return meanBW;
	}
	public final void setMeanBW(final double meanBW) {
		this.meanBW = meanBW;
	}
	public final double getOneMinBW() {
		return oneMinBW;
	}
	public final void setOneMinBW(final double oneMinBW) {
		this.oneMinBW = oneMinBW;
	}
	public final double getFiveMinBW() {
		return fiveMinBW;
	}
	public final void setFiveMinBW(final double fiveMinBW) {
		this.fiveMinBW = fiveMinBW;
	}
	public final double getFifteenMinBW() {
		return fifteenMinBW;
	}
	public final void setFifteenMinBW(final double fifteenMinBW) {
		this.fifteenMinBW = fifteenMinBW;
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//Load entity composite primary key
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
final class LoadEntityPK
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
	public final void setNum(final long num) {
		this.num = num;
	}
	public final long getRun() {
		return run;
	}
	public final void setRun(final long run) {
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