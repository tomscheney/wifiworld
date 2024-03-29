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
 * @date 2015-06-07
 * @author buffer
 *
 */
package com.anynet.wifiworld;

import java.lang.ref.SoftReference;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Process;
import cn.bmob.v3.Bmob;
import cn.smssdk.SMSSDK;

import com.anynet.wifiworld.util.GlobalBroadcast;
import com.anynet.wifiworld.util.LocationHelper;
import com.anynet.wifiworld.util.LoginHelper;
import com.anynet.wifiworld.util.NetworkStateListener;
import com.anynet.wifiworld.wifi.WifiAdmin;
import com.umeng.analytics.MobclickAgent;
import com.umeng.fb.FeedbackAgent;
import com.umeng.update.UmengUpdateAgent;

public class WifiWorldApplication extends Application {
	private String TAG = WifiWorldApplication.class.getSimpleName();
	
    private static WifiWorldApplication mInstance;
    private Stack<SoftReference<Activity>> mActivityStack = new Stack<SoftReference<Activity>>();
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        mInstance = this;
        GlobalBroadcast.registerBroadcastListener(mNetworkListener);
        // 初始化第三方组件
        // 打开数据库
        Bmob.initialize(this, GlobalConfig.BMOB_KEY);
        // 打开验证码
        SMSSDK.initSDK(this, GlobalConfig.SMSSDK_KEY, GlobalConfig.SMSSDK_SECRECT);
        // 打开友盟自动更新
        UmengUpdateAgent.setUpdateCheckConfig(false);
        UmengUpdateAgent.update(this);
        // 打开友盟反馈
		FeedbackAgent agent = new FeedbackAgent(this);
		agent.sync();
		// 打开友盟分析
		MobclickAgent.updateOnlineConfig( this );
		
		//Helper组件
		LocationHelper.getInstance(this);
    }
    
    public static WifiWorldApplication getInstance() {
        return mInstance;
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        GlobalBroadcast.unregisterBroadcastListener(mNetworkListener);
    }
    
    // 以下用户管理整个应用的activity
    public Activity getTopActivity() {
        return mActivityStack.peek().get();
    }
    
    public void exitAplication() {
        finishAllActivity();
        ActivityManager activityMgr = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        activityMgr.killBackgroundProcesses(this.getPackageName());
        Process.killProcess(Process.myPid());
    }
    
    public void activityCreated(Activity activity) {
        if (activity == null) {
            return;
        }
        SoftReference<Activity> softActivity = new SoftReference<Activity>(activity);
        mActivityStack.push(softActivity);
    }
    
    public void activityDestroyed(Activity activity) {
        if (activity == null || mActivityStack.isEmpty() == true) {
            return;
        }
        //可能被垃圾回收了
        if (mActivityStack.peek().get() != activity) {
            return;
        }
        mActivityStack.pop();
    }
    
    public void finishAllActivity()
    {
        while (mActivityStack.isEmpty() == false)
        {
            Activity activity = mActivityStack.pop().get();
            if (activity != null)
            {
                activity.finish();
            }
        }
        mActivityStack.clear();
    }

	public static boolean isLogin() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private NetworkStateListener mNetworkListener = new NetworkStateListener() {
		@Override
		public void onNetworkStateChange(Intent intent) {

			NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			// 网络断了的时候
			if (!networkInfo.isConnected()) {
				LoginHelper.getInstance(getApplicationContext()).logoff();
			} else {
				new Timer().schedule(new TimerTask() {

					@Override
		            public void run() {
						//自动登录
						LoginHelper.getInstance(getApplicationContext()).AutoLogin();
						//自动上传上网记录
						LoginHelper.getInstance(getApplicationContext()).updateWifiDynamic();
		            }
					
				}, 50);
			}
		}
	};
}
