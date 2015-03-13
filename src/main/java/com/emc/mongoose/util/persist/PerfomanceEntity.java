package com.emc.mongoose.util.persist;

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
public final class PerfomanceEntity
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
		final double meanTP, final double oneMinTP, final double fiveMinTP, final double fifteenMinTP,
		final double meanBW, final double oneMinBW, final double fiveMinBW, final double fifteenMinBW)
	{
		this.load = load;
		this.timestamp = timestamp;
		this.countSucc = countSucc;
		this.countQueue = countQueue;
		this.countFail = countFail;
		this.meanTP = meanTP;
		this.oneMinTP = oneMinTP;
		this.fiveMinTP = fiveMinTP;
		this.fifteenMinTP = fifteenMinTP;
		this.meanBW = meanBW;
		this.oneMinBW = oneMinBW;
		this.fiveMinBW = fiveMinBW;
		this.fifteenMinBW = fifteenMinBW;
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public LoadEntity getLoad() {
		return load;
	}
	public void setLoad(LoadEntity load) {
		this.load = load;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public long getCountSucc() {
		return countSucc;
	}
	public void setCountSucc(long countSucc) {
		this.countSucc = countSucc;
	}
	public long getCountQueue() {
		return countQueue;
	}
	public void setCountQueue(long countQueue) {
		this.countQueue = countQueue;
	}
	public long getCountFail() {
		return countFail;
	}
	public void setCountFail(long countFail) {
		this.countFail = countFail;
	}
	public double getMeanTP() {
		return meanTP;
	}
	public void setMeanTP(double meanTP) {
		this.meanTP = meanTP;
	}
	public double getOneMinTP() {
		return oneMinTP;
	}
	public void setOneMinTP(double oneMinTP) {
		this.oneMinTP = oneMinTP;
	}
	public double getFiveMinTP() {
		return fiveMinTP;
	}
	public void setFiveMinTP(double fiveMinTP) {
		this.fiveMinTP = fiveMinTP;
	}
	public double getFifteenMinTP() {
		return fifteenMinTP;
	}
	public void setFifteenMinTP(double fifteenMinTP) {
		this.fifteenMinTP = fifteenMinTP;
	}
	public double getMeanBW() {
		return meanBW;
	}
	public void setMeanBW(double meanBW) {
		this.meanBW = meanBW;
	}
	public double getOneMinBW() {
		return oneMinBW;
	}
	public void setOneMinBW(double oneMinBW) {
		this.oneMinBW = oneMinBW;
	}
	public double getFiveMinBW() {
		return fiveMinBW;
	}
	public void setFiveMinBW(double fiveMinBW) {
		this.fiveMinBW = fiveMinBW;
	}
	public double getFifteenMinBW() {
		return fifteenMinBW;
	}
	public void setFifteenMinBW(double fifteenMinBW) {
		this.fifteenMinBW = fifteenMinBW;
	}
}
