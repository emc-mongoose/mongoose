package com.emc.mongoose.persist.db.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

import static javax.persistence.GenerationType.IDENTITY;
/**
 * Created by olga on 13.03.15.
 */
@Entity
@Table(name = "Perfomance")
public class PerfomanceEntity
implements Serializable{
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id")
	private long id;
	//
	@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@JoinColumns({
		@JoinColumn(name = "load", referencedColumnName = "num", nullable = false),
		@JoinColumn(name = "run", referencedColumnName = "run", nullable = false)
	})
	private LoadEntity load;
	@Column(name = "timestamp")
	private Date timestamp;
	@Column(name = "countSucc")
	private long countSucc;
	@Column(name = "countQueue")
	private long countQueue;
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
	public PerfomanceEntity(){

	}
	public PerfomanceEntity(
		final LoadEntity load, final Date timestamp,
		final long countSucc, final long countQueue, final long countFail,
		final int latencyAvg, final int latencyMin, final int latencyMed, final int latencyMax,
		final double meanTP, final double oneMinTP, final double fiveMinTP, final double fifteenMinTP,
		final double meanBW, final double oneMinBW, final double fiveMinBW, final double fifteenMinBW)
	{
		this.load = load;
		this.timestamp = timestamp;
		this.countSucc = countSucc;
		this.countQueue = countQueue;
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
		this.oneMinBW = oneMinBW;
		this.fiveMinBW = fiveMinBW;
		this.fifteenMinBW = fifteenMinBW;
	}

	public final long getId() {
		return id;
	}
	public final void setId(final long id) {
		this.id = id;
	}
	public final LoadEntity getLoad() {
		return load;
	}
	public final void setLoad(final LoadEntity load) {
		this.load = load;
	}
	public final Date getTimestamp() {
		return timestamp;
	}
	public final void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}
	public final long getCountSucc() {
		return countSucc;
	}
	public final void setCountSucc(final long countSucc) {
		this.countSucc = countSucc;
	}
	public final long getCountQueue() {
		return countQueue;
	}
	public final void setCountQueue(final long countQueue) {
		this.countQueue = countQueue;
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
