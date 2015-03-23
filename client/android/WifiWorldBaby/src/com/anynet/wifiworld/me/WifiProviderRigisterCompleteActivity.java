package com.anynet.wifiworld.me;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.anynet.wifiworld.MainActivity;
import com.anynet.wifiworld.R;
import com.anynet.wifiworld.R.layout;
import com.anynet.wifiworld.R.string;
import com.anynet.wifiworld.app.BaseActivity;
import com.anynet.wifiworld.data.DataCallback;
import com.anynet.wifiworld.data.WifiProfile;

public class WifiProviderRigisterCompleteActivity extends BaseActivity {
	//IPC
	private Intent mIntent = null;
	private WifiProfile mWifiProfile = null;
	
	private void bingdingTitleUI() {
		mTitlebar.ivHeaderLeft.setVisibility(View.VISIBLE);
		mTitlebar.llFinish.setVisibility(View.VISIBLE);
		mTitlebar.llHeaderMy.setVisibility(View.INVISIBLE);
		/*mTitlebar.tvHeaderRight.setVisibility(View.VISIBLE);
		mTitlebar.tvHeaderRight.setText(R.string.push_btn);
		mTitlebar.tvHeaderRight.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Intent i = new Intent(WifiProviderRigisterCompleteActivity.this,
				//		MainActivity.class);
				mIntent.setClass(WifiProviderRigisterCompleteActivity.this,
						MainActivity.class);
				startActivity(mIntent);
			}
		});*/
		mTitlebar.tvTitle.setText(getString(R.string.merchant_certify));
		mTitlebar.ivHeaderLeft.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mIntent = getIntent();
		setContentView(R.layout.wifi_provider_certify_complete);
		super.onCreate(savedInstanceState);
		bingdingTitleUI();
		
		this.findViewById(R.id.btn_wifi_provider_register).setOnClickListener(
			new OnClickListener() {

				@Override
                public void onClick(View arg0) {
					mWifiProfile = (WifiProfile) mIntent.getSerializableExtra(WifiProfile.class.getName());
					mWifiProfile.StoreRemote(getApplicationContext(), 
						new DataCallback<WifiProfile>() {

							@Override
                            public void onSuccess(WifiProfile object) {
								showToast("WiFi信息登记成功。");
								mIntent.setClass(WifiProviderRigisterCompleteActivity.this,
										MainActivity.class);
								startActivity(mIntent);
                            }

							@Override
                            public void onFailed(String msg) {
								showToast("WiFi信息登记失败： " + msg);
                            }
						
					});
				}
				
			});
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

}