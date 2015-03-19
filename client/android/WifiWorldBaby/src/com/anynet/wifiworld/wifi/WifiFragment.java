package com.anynet.wifiworld.wifi;

import java.util.List;

import com.anynet.wifiworld.MainActivity.MainFragment;
import com.anynet.wifiworld.R;
import com.anynet.wifiworld.api.WifiListHelper;

import android.app.ActionBar.LayoutParams;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class WifiFragment extends MainFragment {
	private final static String TAG = WifiFragment.class.getSimpleName();

	private WifiAdmin mWifiAdmin;
	private ListView mWifiListView;
	private WifiListAdapter mWifiListAdapter;
	private List<WifiInfoScanned> mWifiFree;
	private List<WifiInfoScanned> mWifiEncrypt;
	private WifiListHelper mWifiListHelper;
	
	private TextView mWifiNameView;
	private LinearLayout mWifiSquareLayout;
	private PopupWindow mPopupWindow;

	private void bingdingTitleUI() {
		mTitlebar.ivHeaderLeft.setVisibility(View.INVISIBLE);
		mTitlebar.llFinish.setVisibility(View.VISIBLE);
		mTitlebar.llHeaderMy.setVisibility(View.INVISIBLE);
		mTitlebar.tvHeaderRight.setVisibility(View.INVISIBLE);
		// mTitlebar.llFinish.setOnClickListener(this);
		mTitlebar.tvTitle.setText(getString(R.string.connect));
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWifiListHelper = WifiListHelper.getInstance(getActivity());
		mWifiAdmin = mWifiListHelper.getWifiAdmin();
		mWifiListHelper.fillWifiList();
		mWifiFree = mWifiListHelper.getWifiFrees();
		mWifiEncrypt = mWifiListHelper.getWifiEncrypts();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		mPageRoot = inflater.inflate(R.layout.fragment_wifi, null);
		//handle WIFI square view
		mWifiSquareLayout = (LinearLayout) mPageRoot.findViewById(R.id.wifi_square);
		//initial WIFI square pop-up view
		initWifiSquarePopupView();
		//set click listener for WIFI square view
		setWifiSquareListener(mWifiSquareLayout);
		//display WIFI SSID which is connected or not
		mWifiNameView = (TextView) mPageRoot.findViewById(R.id.wifi_name);
		displayWifiConnected(mWifiNameView);
		
		//WIFI list view display and operation
		mWifiListView = (ListView) mPageRoot.findViewById(R.id.wifi_list_view);
		mWifiListAdapter = new WifiListAdapter(this.getActivity(), mWifiFree, mWifiEncrypt);
		mWifiListView.setAdapter(mWifiListAdapter);
		mWifiListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position < (mWifiFree.size() + 1)) {
					WifiInfoScanned wifiSelected = mWifiFree.get(position - 1);
//					mWifiAdmin.Connect(wifiSelected.getWifiName(),
//							wifiSelected.getWifiPwd(),
//							wifiSelected.getWifiType());
					WifiConfiguration cfgSelected = mWifiAdmin.getWifiConfiguration(wifiSelected);
					if (cfgSelected != null) {
						mWifiAdmin.connectToConfiguredNetwork(getActivity(),
								mWifiAdmin.getWifiConfiguration(wifiSelected),
								false);

						mWifiFree.remove(position - 1);
						mWifiListAdapter.refreshWifiList(mWifiFree, mWifiEncrypt);
						TextView wifi_connected = (TextView) mPageRoot.findViewById(R.id.wifi_name);
						String wifiConnected = mWifiAdmin.getWifiNameConnection();
						while (wifiConnected == "") {
							wifiConnected = mWifiAdmin.getWifiNameConnection();
						}
						wifi_connected.setText("已连接" + wifiConnected.substring(1,wifiConnected.length() - 1));
						wifi_connected.setTextColor(Color.BLACK);
						wifi_connected.refreshDrawableState();
					}
				}
			}
		});
		
		//bind common title UI
		super.onCreateView(inflater, container, savedInstanceState);
		bingdingTitleUI();
		return mPageRoot;
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub		
		super.onDestroyView();
	}

	@Override
	public void onPause() {
		getActivity().unregisterReceiver(mReceiver);
		super.onPause();
	}

	@Override
	public void onResume() {
		final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		getActivity().registerReceiver(mReceiver, filter);
		super.onResume();
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
				mWifiListHelper.fillWifiList();
				mWifiFree = mWifiListHelper.getWifiFrees();
				mWifiEncrypt = mWifiListHelper.getWifiEncrypts();
				displayWifiConnected(mWifiNameView);
				mWifiListAdapter.refreshWifiList(mWifiFree, mWifiEncrypt);
			}
		}
	};

	private void displayWifiConnected(TextView wifiNameView) {
		String wifiConnected = mWifiAdmin.getWifiNameConnection();
		if (!wifiConnected.equals("")) {
			wifiNameView.setText("已连接"	+ WifiAdmin.convertToNonQuotedString(wifiConnected));
			wifiNameView.setTextColor(Color.BLACK);
			mWifiListHelper.rmWifiConnected(WifiAdmin.convertToNonQuotedString(wifiConnected));
			mWifiSquareLayout.setVisibility(View.VISIBLE);
		} else {
			wifiNameView.setText("未连接任何Wifi");
			mWifiSquareLayout.setVisibility(View.GONE);
		}
	}
	
	private void initWifiSquarePopupView() {
		if (mPopupWindow == null) {
			LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View popupView = layoutInflater.inflate(R.layout.wifi_popup_view, null);
			//create one pop-up window object
			mPopupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			Button testBtn = (Button) popupView.findViewById(R.id.start_button);
			testBtn.setOnClickListener(new WifiSpeedTester(popupView));
		}
	}
	
	private void showPopupWindow(View view) {
		Log.i(TAG, "show PopupWindow");
		mPopupWindow.setFocusable(true);
		mPopupWindow.setOutsideTouchable(false);
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		mPopupWindow.setAnimationStyle(R.style.PopupAnimation);
		mPopupWindow.showAsDropDown(view);
	}
	
	private void setWifiSquareListener(LinearLayout wifiTasteLayout) {
		TextView wifiSpeed = (TextView) wifiTasteLayout.findViewById(R.id.wifi_speed_test);
		wifiSpeed.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View view) {
				showPopupWindow(view);
			}
		});
	}
}
