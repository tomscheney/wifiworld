package com.anynet.wifiworld.wifi;

import java.io.Serializable;

public class WifiInfoScanned implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String mWifiName;
	private String mWifiMAC;

	private String mWifiPwd;
	private String mWifiType;
	private Integer mWifiStrength;
	private String mRemark;
	
	public WifiInfoScanned(String name, String mac, String pwd, String type, Integer strenghth, String remark) {
		mWifiName = name;
		mWifiMAC = mac;
		mWifiPwd = pwd;
		mWifiType = type;
		mWifiStrength = strenghth;
		mRemark = remark;
	}

	public String getmWifiMAC() {
		return mWifiMAC;
	}

	public void setmWifiMAC(String mWifiMAC) {
		this.mWifiMAC = mWifiMAC;
	}

	public String getRemark() {
		return mRemark;
	}

	public void setRemark(String remark) {
		this.mRemark = remark;
	}

	public String getWifiName() {
		return mWifiName;
	}

	public void setWifiName(String wifiName) {
		this.mWifiName = wifiName;
	}

	public String getWifiPwd() {
		return mWifiPwd;
	}

	public void setWifiPwd(String wifiPwd) {
		this.mWifiPwd = wifiPwd;
	}

	public String getWifiType() {
		return mWifiType;
	}

	public void setWifiType(String wifiType) {
		this.mWifiType = wifiType;
	}

	public Integer getWifiStrength() {
		return mWifiStrength;
	}

	public void setWifiStrength(Integer wifiStrength) {
		this.mWifiStrength = wifiStrength;
	}
	
}
