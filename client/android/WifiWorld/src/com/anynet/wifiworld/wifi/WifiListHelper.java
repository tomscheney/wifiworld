package com.anynet.wifiworld.wifi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.anynet.wifiworld.MainActivity;
import com.anynet.wifiworld.data.MultiDataCallback;
import com.anynet.wifiworld.data.WifiProfile;
import com.anynet.wifiworld.data.WifiWhite;
import com.anynet.wifiworld.util.LoginHelper;

import cn.bmob.v3.datatype.BmobGeoPoint;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class WifiListHelper {
	private final static String TAG = WifiListHelper.class.getSimpleName();
	
	public static WifiListHelper mInstance;
	
	private Context mContext;
	private Handler mHandler;
	private WifiAdmin mWifiAdmin;
	private List<WifiInfoScanned> mWifiAuth; //认证wifi
	private List<WifiInfoScanned> mWifiFree;
	private List<WifiInfoScanned> mWifiEncrypt;
	private List<String> mWifiListUnique;
	public List<WifiProfile> mWifiProfiles;
	public Map<String, List<WifiWhite>> mWifiWhites = new HashMap<String, List<WifiWhite>>();
	public WifiInfoScanned mWifiInfoCur; 
	
	public boolean refreshed = true;
	private final String WIFI_LIST_FILE_NAME = "wifi_list_file.txt";

	public static WifiListHelper getInstance(Context context, Handler handler) {
		if (null == mInstance) {
			mInstance = new WifiListHelper(context, handler);
		}
		return mInstance;
	}
	
	private WifiListHelper(Context context, Handler handler) {
		mWifiAdmin = WifiAdmin.getInstance(context);
		mWifiFree = new ArrayList<WifiInfoScanned>();
		mWifiEncrypt = new ArrayList<WifiInfoScanned>();
		mWifiAuth = new ArrayList<WifiInfoScanned>();
		mWifiListUnique = new ArrayList<String>();
		mContext = context;
		mHandler = handler;
		refreshed = true;
	}
	
	public static WifiListHelper getInstance(Context context) {
		if (null == mInstance) {
			mInstance = new WifiListHelper(context);
		}
		return mInstance;
	}
	
	private WifiListHelper(Context context) {
		mWifiAdmin = WifiAdmin.getInstance(context);
		mWifiAuth = new ArrayList<WifiInfoScanned>();
		mWifiFree = new ArrayList<WifiInfoScanned>();
		mWifiEncrypt = new ArrayList<WifiInfoScanned>();
		mWifiListUnique = new ArrayList<String>();
		mContext = context;
	}
	
	Thread freshThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			refreshed = false;
			Log.e(TAG, "Thread id:"+this.hashCode());
			organizeWifiList(mWifiAdmin.scanWifi());
		}
	});
	
	public boolean fillWifiList() {
		if (mWifiAdmin.isWifiEnabled()) {
			if(refreshed){
				new Thread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						refreshed = false;
						Log.e(TAG, "Thread id:"+this.hashCode());
						organizeWifiList(mWifiAdmin.scanWifi());
					}
				}).start();
			}
			return true;
		}
		return false;
	}

	public void clearListAndCloseWifi() {
		mWifiAuth.clear();
		mWifiFree.clear();
		mWifiEncrypt.clear();
		if (mWifiAdmin.openWifi()) {
			mWifiAdmin.closeWifi();
		}
	}

	public void organizeWifiList(final List<ScanResult> wifiList) {

		List<String> macAddresses = new ArrayList<String>();
		for (ScanResult scanResult : wifiList) {
			macAddresses.add(scanResult.BSSID);
		}
		
		WifiProfile wifiProfile = new WifiProfile();
		wifiProfile.BatchQueryByMacAddress(mContext, macAddresses, true, new MultiDataCallback<WifiProfile>() {
			
			@Override
			public boolean onSuccess(List<WifiProfile> objects) {
				Log.i(TAG, "Batch query by mac address success");
				mWifiProfiles = objects;
				refreshListUI(wifiList);
				return false;
			}
			
			@Override
			public boolean onFailed(String msg) {
				Log.i(TAG, msg);
				return false;
			}
		});
		
		refreshListUI(wifiList);
	}
	
	private void refreshListUI(List<ScanResult> wifiList) {
		mWifiAuth.clear();
		mWifiFree.clear();
		mWifiEncrypt.clear();
		mWifiInfoCur = null;
		WifiInfo wifiInfo = mWifiAdmin.getWifiConnected();
		for (ScanResult hotspot : wifiList) {
			//Check whether WiFi is stored local
			final WifiConfiguration wifiCfg = mWifiAdmin.getWifiConfiguration(hotspot, null);
			verifyList(wifiCfg, hotspot, wifiInfo, mWifiProfiles);
		}
		Message msg = new Message();
		msg.what = WifiFragment.UPDATE_WIFI_LIST;
		mHandler.sendMessageAtFrontOfQueue(msg);
		refreshed = true;
	}
	
	private void verifyList(WifiConfiguration wifiCfg, ScanResult hotspot, WifiInfo wifiInfo, List<WifiProfile> objects) {
		String wifiName;
		String wifiPwd;
		String wifiType;
		String wifiMAC;
		String wifiRemark = "";
		Integer wifiStrength;
		BmobGeoPoint wifiGeometry;
		WifiInfoScanned wifiInfoScanned;
		
		if (wifiInfo != null && wifiInfo.getSSID()!=null && (wifiInfo.getSSID().equals(WifiAdmin.convertToQuotedString(hotspot.SSID))
				|| wifiInfo.getSSID().equals(hotspot.SSID))) {
			Log.i(TAG, hotspot.SSID + " is the current connected wifi");
			wifiName = hotspot.SSID;
			wifiType = WifiAdmin.ConfigSec.getScanResultSecurity(hotspot);
			wifiMAC = hotspot.BSSID;
			wifiStrength = WifiAdmin.getWifiStrength(hotspot.level);
			wifiGeometry = WifiAdmin.getWifiGeometry(mContext, hotspot.level);
			wifiPwd = null;
			wifiRemark = "当前正在使用的WiFi";
			mWifiInfoCur = new WifiInfoScanned(wifiName, wifiMAC, wifiPwd, wifiType,
					wifiStrength, wifiGeometry, wifiRemark);
			mWifiInfoCur.setWifiEncryptString(WifiAdmin.ConfigSec.getDisplaySecirityString(hotspot));
			
			//query WiFi whether has been shared
			WifiProfile wifiProfile = isContained(WifiAdmin.convertToNonQuotedString(hotspot.BSSID), objects);
			if (wifiProfile != null) {
				mWifiInfoCur.setAuthWifi(true);
				mWifiInfoCur.setWifiLogo(wifiProfile.Logo);
				mWifiInfoCur.setBanner(wifiProfile.Banner);
				mWifiInfoCur.setAlias(wifiProfile.Alias);
				mWifiInfoCur.setSponser(wifiProfile.Sponser);
				mWifiInfoCur.setWifiPwd(wifiProfile.Password);
				pullWifiWhites(wifiProfile.Sponser, mWifiInfoCur);
			}
			boolean isLocalSave = wifiCfg == null ? false : true;
			mWifiInfoCur.setLocalSave(isLocalSave);
			
			return;
		}
		
		if (objects != null) {
			WifiProfile wifiProfile = isContained(WifiAdmin.convertToNonQuotedString(hotspot.BSSID), objects);
			if (wifiProfile != null) {
				Log.i(TAG, "Query wifi:" + hotspot.BSSID + ":" + hotspot.SSID + " has been shared");
				wifiName = hotspot.SSID;
				wifiMAC = hotspot.BSSID;
				wifiPwd = wifiProfile.Password;
				wifiType = WifiAdmin.ConfigSec.getScanResultSecurity(hotspot);
				wifiStrength = WifiAdmin.getWifiStrength(hotspot.level);
				
				if (LoginHelper.getInstance(mContext).mKnockList.contains(wifiMAC)) {
					wifiRemark = "已经敲门成功可直接使用";
				} else {
					wifiRemark = "敲门成功就能使用";
				}
				
				wifiInfoScanned = new WifiInfoScanned(wifiName, wifiMAC, wifiPwd, 
						wifiType, wifiStrength, null, wifiRemark);
				wifiInfoScanned.setWifiEncryptString(WifiAdmin.ConfigSec.getDisplaySecirityString(hotspot));
				
				if (wifiProfile.Alias != null && wifiProfile.Alias.length() > 0)
					wifiInfoScanned.setAlias(wifiProfile.Alias);
				wifiInfoScanned.setAuthWifi(true);
				wifiInfoScanned.setWifiLogo(wifiProfile.Logo);
				wifiInfoScanned.setBanner(wifiProfile.Banner);
				wifiInfoScanned.setSponser(wifiProfile.Sponser);
				pullWifiWhites(wifiProfile.Sponser, wifiInfoScanned);
				mWifiAuth.add(wifiInfoScanned);
				return;
			}
		}
		
		if (wifiCfg != null) {
			wifiName = hotspot.SSID;
			wifiPwd = wifiCfg.preSharedKey;
			wifiType = WifiAdmin.ConfigSec.getWifiConfigurationSecurity(wifiCfg);
			wifiMAC = hotspot.BSSID;
			wifiStrength = WifiAdmin.getWifiStrength(hotspot.level);
			wifiGeometry = WifiAdmin.getWifiGeometry(mContext, hotspot.level);

			if (WifiAdmin.ConfigSec.isOpenNetwork(wifiType)) {
				wifiRemark = "无密码";
			} else {
				wifiRemark = "本地已保存";
			}
			
			wifiInfoScanned = new WifiInfoScanned(wifiName, wifiMAC, wifiPwd, wifiType,
					wifiStrength, wifiGeometry, wifiRemark);
			wifiInfoScanned.setLocalSave(true);
			wifiInfoScanned.setWifiEncryptString(WifiAdmin.ConfigSec.getDisplaySecirityString(hotspot));
			mWifiFree.add(wifiInfoScanned);
		} else {
			//Check whether is a open WiFi
			wifiName = hotspot.SSID;
			wifiType = WifiAdmin.ConfigSec.getScanResultSecurity(hotspot);
			wifiMAC = hotspot.BSSID;
			wifiStrength = WifiAdmin.getWifiStrength(hotspot.level);
			wifiGeometry = WifiAdmin.getWifiGeometry(mContext, hotspot.level);
			if (WifiAdmin.ConfigSec.isOpenNetwork(wifiType)) {
				wifiRemark = "无密码";
				wifiInfoScanned = new WifiInfoScanned(wifiName,wifiMAC, null, wifiType, wifiStrength,
						wifiGeometry, wifiRemark);
				wifiInfoScanned.setWifiEncryptString(WifiAdmin.ConfigSec.getDisplaySecirityString(hotspot));
				mWifiFree.add(wifiInfoScanned);
			} else {
				wifiInfoScanned = new WifiInfoScanned(wifiName,wifiMAC, null, wifiType, wifiStrength,
						wifiGeometry, null);
				wifiInfoScanned.setWifiEncryptString(WifiAdmin.ConfigSec.getDisplaySecirityString(hotspot));
				mWifiEncrypt.add(wifiInfoScanned);
			}
		}
	}
	
	private void pullWifiWhites(final String wifiSponser, final WifiInfoScanned wifiInfoScanned) {
		WifiWhite wifiWhite = new WifiWhite();
		wifiWhite.QueryWhitersByUser(mContext, wifiSponser, new MultiDataCallback<WifiWhite>() {

			@Override
			public boolean onSuccess(List<WifiWhite> objects) {
				Log.i(TAG, "Success to pull wifi white list:" + wifiSponser);
				mWifiWhites.put(wifiSponser, objects);
				wifiInfoScanned.setWifiWhites(objects);
				return true;
			}

			@Override
			public boolean onFailed(String msg) {
				Log.e(TAG, "Failed to pull wifi white list:" + msg);
				if (mWifiWhites.size() > 0) {
					wifiInfoScanned.setWifiWhites(mWifiWhites.get(wifiSponser));
				}
				
				return false;
			}
		});
	}
	
	private WifiProfile isContained(String macAddress, List<WifiProfile> mList) {
		if (mList != null) {
			for (int idx=0; idx<mList.size(); ++idx) {
				if (mList.get(idx).MacAddr.equals(macAddress)) {
					return mList.get(idx);
				}
			}
		}
		
		return null;
	}
	
	//remove WIFI which is connected from free-WIFI-list or encrypt-WIFI-list
	//need compare with WIFI SSID and Encrypt type
	public WifiInfoScanned rmWifiConnected(String wifiName) {
		for (Iterator<WifiInfoScanned> it = mWifiFree.iterator(); it.hasNext();) {
			WifiInfoScanned tmpInfo = it.next();
			if (wifiName.equals(tmpInfo.getWifiName())) {
				it.remove();
				return tmpInfo;
			}
		}

		for (Iterator<WifiInfoScanned> it = mWifiEncrypt.iterator(); it.hasNext();) {
			WifiInfoScanned tmpInfo = it.next();
			if (wifiName.equals(tmpInfo.getWifiName())) {
				it.remove();
				return tmpInfo;
			}
		}

		return null;
	}
	
	public Bitmap getWifiLogo(String wifiMac) {
		if (mWifiProfiles != null) {
			for (WifiProfile wifiProfile : mWifiProfiles) {
				if (wifiProfile.MacAddr.equals(wifiMac)) {
					return wifiProfile.getLogo();
				}
			}
		} else {
			Log.i(TAG, "Wifi Profile table return null");
		}
		return null;
	}
	
	public Bitmap getWifiLogo(WifiInfoScanned wifiInfoScanned) {
		if (mWifiProfiles != null) {
			for (WifiProfile wifiProfile : mWifiProfiles) {
				if (wifiProfile.MacAddr.equals(wifiInfoScanned.getWifiMAC())) {
					return wifiProfile.getLogo();
				}
			}
		} else {
			Log.i(TAG, "Wifi Profile table return null");
		}
		return null;
	}
	
	public String getWifiBnner(WifiInfoScanned wifiInfoScanned) {
		if (mWifiProfiles != null) {
			for (WifiProfile wifiProfile : mWifiProfiles) {
				if (wifiProfile.MacAddr.equals(wifiInfoScanned.getWifiMAC())) {
					return wifiProfile.Banner;
				}
			}
		} else {
			Log.i(TAG, "Wifi Profile table return null");
		}
		return null;
	}
	
	private boolean readFile() {
		mWifiListUnique.clear();
		FileInputStream fis = null;
		BufferedReader br = null;
		try {
			fis = mContext.openFileInput(WIFI_LIST_FILE_NAME);
			br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
			String line = "";
			while ((line=br.readLine()) != null) {
				mWifiListUnique.add(line);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (fis != null) {
					fis.close();
				}
			} catch (Exception e2) {
				// TODO: handle exception
				e2.printStackTrace();
			}
			
		}
		return true;
	}
	
	private boolean writeFile() {
		if (mWifiListUnique.isEmpty()) {
			Log.i(TAG, "no wifi date store in wifilistunique");
			return false;
		}
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		try {
			fos = mContext.openFileOutput(WIFI_LIST_FILE_NAME, mContext.MODE_PRIVATE);
			osw = new OutputStreamWriter(fos, "UTF-8");
			bw = new BufferedWriter(osw);
			for (String wifi_str : mWifiListUnique) {
				bw.write(wifi_str+"\n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
				if (osw != null) {
					osw.close();
				}
				if (bw != null) {
					bw.close();
				}
			} catch (Exception e2) {
				// TODO: handle exception
				e2.printStackTrace();
			}
		}
		return true;
	}
	
	public WifiAdmin getWifiAdmin() {
		return mWifiAdmin;
	}

	public List<WifiInfoScanned> getWifiAuths() {
		return mWifiAuth;
	} 
	
	public List<WifiInfoScanned> getWifiFrees() {
		return mWifiFree;
	}

	public List<WifiInfoScanned> getWifiEncrypts() {
		return mWifiEncrypt;
	}
	
	public boolean uploadWifiListSanned() {
		
		return true;
	}
}
