/*
 * Copyright 2015 Anynet Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Anynet Corporation or its suppliers
 * or licensors. Title to the Material remains with Anynet Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Anynet or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and
 * treaty provisions.
 * No part of the Material may be used, copied, reproduced, modified, published
 * , uploaded, posted, transmitted, distributed, or disclosed in any way
 * without Anynet's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Anynet in writing.
 *
 * @brief ANLog is the custom log for wifiworld project.
 * @date 2015-06-04
 * @author Jason.Chen
 *
 */

package com.anynet.wifiworld.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.util.Log;

public class WifiSecuritiesV8 extends WifiSecurities {
	
	static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;
    
    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }
    
    private static final String TAG = "WifiSecuritiesV8";
    
    private static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

	@Override
	public String getWifiConfigurationSecurity(WifiConfiguration wifiConfig) {
		return String.valueOf(getSecurity(wifiConfig));
	}

	@Override
	public String getScanResultSecurity(ScanResult scanResult) {
		return String.valueOf(getSecurity(scanResult));
	}

	@Override
	public void setupSecurity(WifiConfiguration config, String security, String password) {
		config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        
        final int sec = security == null ? SECURITY_NONE : Integer.valueOf(security);
        final int passwordLen = password == null ? 0 : password.length();
        switch (sec) {
        case SECURITY_NONE:
            config.allowedKeyManagement.set(KeyMgmt.NONE);
            break;

        case SECURITY_WEP:
            config.allowedKeyManagement.set(KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
            if (passwordLen != 0) {
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if ((passwordLen == 10 || passwordLen == 26 || passwordLen == 58) &&
                        password.matches("[0-9A-Fa-f]*")) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = '"' + password + '"';
                }
            }
            break;

        case SECURITY_PSK:
            config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
            if (passwordLen != 0) {
                if (password.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = password;
                } else {
                    config.preSharedKey = '"' + password + '"';
                }
            }
            break;

        	case SECURITY_EAP:
            config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
            config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
//            config.eap.setValue((String) mEapMethodSpinner.getSelectedItem());
//
//            config.phase2.setValue((mPhase2Spinner.getSelectedItemPosition() == 0) ? "" :
//                    "auth=" + mPhase2Spinner.getSelectedItem());
//            config.ca_cert.setValue((mEapCaCertSpinner.getSelectedItemPosition() == 0) ? "" :
//                    KEYSTORE_SPACE + Credentials.CA_CERTIFICATE +
//                    (String) mEapCaCertSpinner.getSelectedItem());
//            config.client_cert.setValue((mEapUserCertSpinner.getSelectedItemPosition() == 0) ?
//                    "" : KEYSTORE_SPACE + Credentials.USER_CERTIFICATE +
//                    (String) mEapUserCertSpinner.getSelectedItem());
//            config.private_key.setValue((mEapUserCertSpinner.getSelectedItemPosition() == 0) ?
//                    "" : KEYSTORE_SPACE + Credentials.USER_PRIVATE_KEY +
//                    (String) mEapUserCertSpinner.getSelectedItem());
//            config.identity.setValue((mEapIdentityView.length() == 0) ? "" :
//                    mEapIdentityView.getText().toString());
//            config.anonymous_identity.setValue((mEapAnonymousView.length() == 0) ? "" :
//                    mEapAnonymousView.getText().toString());
//            if (mPasswordView.length() != 0) {
//                config.password.setValue(mPasswordView.getText().toString());
//            }
            break;

        	default:
                Log.e(TAG, "Invalid security type: " + sec);
        }
        
//        config.proxySettings = mProxySettings;
//        config.ipAssignment = mIpAssignment;
//        config.linkProperties = new LinkProperties(mLinkProperties);
        //WifiProxySetting.setWifiProxySettings(config, "127.0.0.1", 8118);
		
	}
	
	private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }

	@Override
	public String getDisplaySecirityString(final ScanResult scanResult) {
		final int security = getSecurity(scanResult);
		if(security == SECURITY_PSK) {
			switch(getPskType(scanResult)) {
			case WPA:
				return "WPA";
			case WPA_WPA2:
			case WPA2:
				return "WPA2";
			default:
				return "?";
			}
		} else {
			switch(security) {
			case SECURITY_NONE:
				return "OPEN";
			case SECURITY_WEP:
				return "WEP";
			case SECURITY_EAP:
				return "EAP";
			}
		}
		
		return "?";
	}

	@Override
	public boolean isOpenNetwork(String security) {
		return String.valueOf(SECURITY_NONE).equals(security);
	}

}
