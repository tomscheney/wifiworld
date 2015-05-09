package com.anynet.wifiworld.wifi;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.anynet.wifiworld.MainActivity.MainFragment;
import com.anynet.wifiworld.R;
import com.anynet.wifiworld.UserLoginActivity;
import com.anynet.wifiworld.R.color;
import com.anynet.wifiworld.app.BaseActivity;
import com.anynet.wifiworld.data.DataCallback;
import com.anynet.wifiworld.data.DataListenerHelper;
import com.anynet.wifiworld.data.MultiDataCallback;
import com.anynet.wifiworld.data.UserProfile;
import com.anynet.wifiworld.data.WifiBlack;
import com.anynet.wifiworld.data.WifiComments;
import com.anynet.wifiworld.data.WifiProfile;
import com.anynet.wifiworld.data.WifiQuestions;
import com.anynet.wifiworld.data.WifiWhite;
import com.anynet.wifiworld.knock.KnockStepFirstActivity;
import com.anynet.wifiworld.me.WifiProviderRigisterFirstActivity;
import com.anynet.wifiworld.me.WifiProviderRigisterLicenseActivity;
import com.anynet.wifiworld.util.LoginHelper;
import com.anynet.wifiworld.wifi.WifiBRService.OnWifiStatusListener;
import com.anynet.wifiworld.wifi.ui.WifiCommentActivity;
import com.anynet.wifiworld.wifi.ui.WifiCommentsAdapter;
import com.anynet.wifiworld.wifi.ui.WifiSquarePopup;
import com.avos.avoscloud.LogUtil.log;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

public class WifiFragment extends MainFragment {
	private final static String TAG = WifiFragment.class.getSimpleName();

	public static final int WIFI_CONNECT_CONFIRM = 1;
	public static final int WIFI_COMMENT = 2;

	public final static int UPDATE_WIFI_LIST = 99;
	public final static int GET_WIFI_DETAILS = 98;

	private WifiAdmin mWifiAdmin;
	private PullToRefreshListView mWifiListView;
	private WifiListAdapter mWifiListAdapter;
	private List<WifiInfoScanned> mWifiAuth = new ArrayList<WifiInfoScanned>();
	private List<WifiInfoScanned> mWifiFree = new ArrayList<WifiInfoScanned>();
	private List<WifiInfoScanned> mWifiEncrypt = new ArrayList<WifiInfoScanned>();
	private WifiListHelper mWifiListHelper;
	private LoginHelper mLoginHelper = null;

	private TextView mWifiNameView;
	private ImageView mWifiLogoView;
	private Button mOpenWifiBtn;
	private ToggleButton mWifiSwitch;
	private TextView mWifiMaster;
	private TextView mWifiDesc;

	private LinearLayout mWifiTitleLayout;
	
	private LinearLayout mWifiSquareLayout;
	private WifiSquarePopup mWifiSquarePopup;
	private int mLastSquareId = -1;

	private WifiConnectDialog mWifiConnectDialog;
	private WifiConnectDialog mWifiConnectPwdDialog;

	private WifiInfo mLastWifiInfo = null;
	private WifiInfoScanned mLastWifiInfoScanned = null;
	private WifiInfoScanned mWifiItemClick;
	
	private List<WifiWhite> mWifiWhite = null;
	private List<WifiBlack> mWifiBlack = null;

	private boolean isPwdConnect = false;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "handle wifi list helper message");
			int value = msg.what;
			if (value == UPDATE_WIFI_LIST) {
				mWifiAuth = mWifiListHelper.getWifiAuths();
				mWifiFree = mWifiListHelper.getWifiFrees();
				mWifiEncrypt = mWifiListHelper.getWifiEncrypts();
				if (mWifiListAdapter != null) {
					mWifiListAdapter.refreshWifiList(mWifiAuth, mWifiFree, mWifiEncrypt);
				}
				refreshWifiTitleInfo(true);
			}
			mWifiListView.onRefreshComplete();
		}

	};

	private WifiBRService.WifiMonitorService mWifiMonitorService;
	ServiceConnection conn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mWifiMonitorService = ((WifiBRService.WifiMonitorService.WifiStatusBinder) service).getService();
			mWifiMonitorService.setOnWifiStatusListener(new OnWifiStatusListener() {

				@Override
				public void onWifiStatChanged(boolean isEnabled) {
					if (isEnabled) {
						mPageRoot.findViewById(R.id.wifi_disable_layout).setVisibility(View.INVISIBLE);
						mPageRoot.findViewById(R.id.wifi_enable_layout).setVisibility(View.VISIBLE);
						WifiBRService.setWifiScannable(true);
					} else {
						mPageRoot.findViewById(R.id.wifi_disable_layout).setVisibility(View.VISIBLE);
						mPageRoot.findViewById(R.id.wifi_enable_layout).setVisibility(View.INVISIBLE);
					}
				}

				@Override
				public void onNetWorkChanged(boolean isConnected, String str) {
					if (isConnected) {
						// 一旦网络状态发生变化后停止监听服务
						WifiBRService.setWifiSupplicant(false);
					}
					// refresh WiFi list and WiFi title info
					mWifiListHelper.fillWifiList();

					// refreshWifiTitleInfo();
					isPwdConnect = false;

					if (isConnected) {

						WifiInfo curwifi = WifiAdmin.getInstance(getApplicationContext()).getWifiConnected();
						if (curwifi == null)
							return;
						boolean isExist = false;
						for (WifiInfoScanned item : mWifiAuth) {
							if (item.getWifiName().equals(curwifi.getSSID())) {
								isExist = true;
							}
						}// 发现wifi未认证不做数据监听
						if (!isExist)
							return;

						// 一旦打开连接wifi，如果是认证的wifi需要做监听wifi提供者实时共享子信息
						String CurMac = curwifi.getBSSID();
						WifiProfile data_listener = new WifiProfile();
						data_listener.startListenRowUpdate(getActivity(), "WifiProfile", WifiProfile.unique_key, CurMac,
								DataListenerHelper.Type.UPDATE, new DataCallback<WifiProfile>() {

									@Override
									public void onFailed(String msg) {
										Log.d(TAG, msg);
									}

									@Override
									public void onSuccess(WifiProfile object) {
										// 通知下线
										if (!object.isShared()) {
											getActivity().runOnUiThread(new Runnable() {

												@Override
												public void run() {
													showToast("对不起，此网络主人停止了网络分享，请保存数据退出网络。");
													WifiAdmin.getInstance(getApplicationContext()).disConnectionWifi(
															mWifiAdmin.getWifiConnected().getNetworkId());
												}
											});
										}
									}
								});
					}
				}

				@Override
				public void onScannableChanged() {
					mWifiListHelper.fillWifiList();
				}

				@Override
				public void onSupplicantChanged(String statusStr) {
					mWifiNameView.setText(statusStr);
					// mWifiNameView.setTextColor(Color.BLACK);
					WifiInfoScanned wifiInfoCurrent = WifiListHelper.getInstance(getActivity()).mWifiInfoCur;
					if (wifiInfoCurrent != null && wifiInfoCurrent.getWifiLogo() != null) {
						mWifiLogoView.setImageBitmap(wifiInfoCurrent.getWifiLogo());
					} else {
						mWifiLogoView.setImageResource(R.drawable.icon_invalid);
					}
				}

				@Override
				public void onSupplicantDisconnected(String statusStr) {
					if (isPwdConnect) {
						Log.i(TAG, "forget network: " + mWifiItemClick.getNetworkId());
						mWifiAdmin.forgetNetwork(mWifiItemClick.getNetworkId());
						isPwdConnect = false;
						String titleStr = "密码输入错误，请重新输入密码";
						showWifiConnectPwdConfirmDialog(titleStr, mWifiItemClick, mWifiConnectPwdDialog);
					} else {
						mWifiListHelper.fillWifiList();
					}
				}
			});
		}
	};

	private OnClickListener mWifiSquareClickListener = new OnClickListener() {
		
		@Override
		public void onClick(View view) {
			switch (view.getId()) {
			case R.id.wifi_speed:
				mWifiSquarePopup.displayLayout(WifiSquarePopup.SquareType.SPEED_TESTER);
				break;
			case R.id.wifi_share:
				mWifiSquarePopup.displayLayout(WifiSquarePopup.SquareType.SHARE_PAGE);
				break;
			case R.id.wifi_louder:
				mWifiSquarePopup.displayLayout(WifiSquarePopup.SquareType.COMMENTS_PAGE);
				break;
			default:
				break;
			}
			if (!mWifiSquarePopup.isShowing()) {
				mWifiSquarePopup.show(view);
			} else if (mLastSquareId == view.getId()) {
				mWifiSquarePopup.dismiss();
			}

			mLastSquareId = view.getId();
		}
	};
	
	private void bingdingTitleUI() {
		mTitlebar.ivHeaderLeft.setVisibility(View.INVISIBLE);
		mTitlebar.llFinish.setVisibility(View.VISIBLE);
		// mTitlebar.llHeaderMy.setVisibility(View.INVISIBLE);
		mTitlebar.tvHeaderRight.setVisibility(View.INVISIBLE);
		// mTitlebar.llFinish.setOnClickListener(this);
		mTitlebar.tvTitle.setText(getString(R.string.connect));
	}

	@Override
	protected void onVisible() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onInvisible() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mWifiListHelper = WifiListHelper.getInstance(getActivity(), mHandler);
		mWifiAdmin = mWifiListHelper.getWifiAdmin();

		WifiBRService.bindWifiService(getActivity(), conn);
		mWifiConnectDialog = new WifiConnectDialog(getActivity(), WifiConnectDialog.DialogType.DEFAULT);
		mWifiConnectPwdDialog = new WifiConnectDialog(getActivity(), WifiConnectDialog.DialogType.PASSWORD);

		mLoginHelper = LoginHelper.getInstance(getApplicationContext());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		mPageRoot = inflater.inflate(R.layout.fragment_wifi, null);
		if (mWifiAdmin.isWifiEnabled()) {
			mPageRoot.findViewById(R.id.wifi_disable_layout).setVisibility(View.INVISIBLE);
			mPageRoot.findViewById(R.id.wifi_enable_layout).setVisibility(View.VISIBLE);
		} else {
			mPageRoot.findViewById(R.id.wifi_disable_layout).setVisibility(View.VISIBLE);
			mPageRoot.findViewById(R.id.wifi_enable_layout).setVisibility(View.INVISIBLE);
		}

		mWifiTitleLayout = (LinearLayout) mPageRoot.findViewById(R.id.wifi_connected_info);
		mWifiTitleLayout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent("com.anynet.wifiworld.wifi.ui.WIFI_ADVANCE");
				getActivity().startActivity(intent);
				getActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.hold);
			}
		});
		// handle WIFI square view
		mWifiSquareLayout = (LinearLayout) mPageRoot.findViewById(R.id.wifi_square);
		setWifiSquareListener(mWifiSquareLayout);
		// initial WIFI square pop-up view
		mWifiSquarePopup = new WifiSquarePopup(getActivity(), this);
		// display WIFI SSID which is connected or not
		mWifiNameView = (TextView) mPageRoot.findViewById(R.id.tv_wifi_name);
		mWifiLogoView = (ImageView) mPageRoot.findViewById(R.id.wifi_logo);
		mWifiMaster = (TextView) mPageRoot.findViewById(R.id.tv_wifi_master_v);
		mWifiDesc = (TextView) mPageRoot.findViewById(R.id.tv_wifi_desc_v);

		// WIFI list view display and operation
		mWifiListView = (PullToRefreshListView) mPageRoot.findViewById(R.id.wifi_list_view);
		mWifiListAdapter = new WifiListAdapter(this.getActivity(), mWifiAuth, mWifiFree, mWifiEncrypt);
		mWifiListView.setAdapter(mWifiListAdapter);
		mWifiListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int index_auth = (mWifiAuth.size() + 2);
				if (position < index_auth) {
					mWifiItemClick = mWifiAuth.get(position - 2);
					// 先过滤用户，再判断是否敲门，没有敲门提醒其去敲门
					if (!LoginHelper.getInstance(getActivity()).isLogined()) {
						new AlertDialog.Builder(getActivity()).setTitle("提示").setMessage("需要登录才能免费使用WiFi，是否登录？")
						.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								UserLoginActivity.start((BaseActivity) getActivity());
							}
						}).setNegativeButton("取消", null).show();
					} else {
						final String userId = mLoginHelper.getUserid();
						WifiWhite wifiWhite = new WifiWhite();
						wifiWhite.QueryWhitersByUser(getActivity(), mWifiItemClick.getSponser(), new MultiDataCallback<WifiWhite>() {

							@Override
							public boolean onSuccess(List<WifiWhite> whiteList) {
								mWifiWhite = whiteList;
								if (whiteList.size() > 0 && !mWifiWhite.contains(userId)) {
									showToast("Can not to connect this wifi as sponser set the white list and you are not under this list");
								} else if (mLoginHelper.canAccessDirectly(mWifiItemClick.getWifiMAC()) || mLoginHelper.mKnockList.contains(mWifiItemClick.getWifiMAC())) {
									showWifiConnectConfirmDialog(mWifiItemClick, true);
								} else {
									// 弹出询问对话框
									new AlertDialog.Builder(getActivity()).setTitle("Wi-Fi敲门").setMessage("当前Wi-Fi的门没有敲开，是否去敲门？")
											.setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {

												@Override
												public void onClick(DialogInterface dialog, int which) {
													// 拉取敲门问题
													WifiQuestions wifiQuestions = new WifiQuestions();
													wifiQuestions.QueryByMacAddress(getApplicationContext(), mWifiItemClick.getWifiMAC(),
															new DataCallback<WifiQuestions>() {

																@Override
																public void onSuccess(WifiQuestions object) {
																	KnockStepFirstActivity.start(WifiFragment.this.getActivity(), "WifiDetailsActivity", object);
																}

																@Override
																public void onFailed(String msg) {
																	showToast("获取敲门信息失败，请稍后重试:" + msg);
																}
															});
												}
											}).setNegativeButton("取消", null).show();
								}
								return true;
							}

							@Override
							public boolean onFailed(String msg) {
								showToast("Failed to pull wifi white list:" + msg);
								Log.e(TAG, "Failed to pull wifi white list:" + msg);
								return false;
							}
						});
					}
				} else if (position < (index_auth + mWifiFree.size() + 1)) {
					mWifiItemClick = mWifiFree.get(position - 1 - index_auth);
					showWifiConnectConfirmDialog(mWifiItemClick, false);
				} else {
					mWifiItemClick = mWifiEncrypt.get(position - 2 - mWifiFree.size() - index_auth);
					String titleStr = "连接到：" + mWifiItemClick.getWifiName();
					showWifiConnectPwdConfirmDialog(titleStr, mWifiItemClick, mWifiConnectPwdDialog);

				}
			}
		});
		mWifiListView.setOnRefreshListener(new OnRefreshListener<ListView>() {

			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				mWifiListHelper.fillWifiList();
			}
		});

		mOpenWifiBtn = (Button) mPageRoot.findViewById(R.id.open_wifi_btn);
		mOpenWifiBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				mWifiAdmin.openWifi();
			}
		});

		mWifiSwitch = (ToggleButton) mPageRoot.findViewById(R.id.wifi_control_toggle);
		mWifiSwitch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				WifiInfo wifiInfo = mWifiAdmin.getWifiConnected();
				if (wifiInfo != null) {
					mWifiAdmin.disConnectionWifi(wifiInfo.getNetworkId());
				}
				refreshWifiTitleInfo(false);
			}
		});

		mWifiListHelper.fillWifiList();
		// refreshWifiTitleInfo();

		// bind common title UI
		super.onCreateView(inflater, container, savedInstanceState);
		bingdingTitleUI();
		return mPageRoot;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == WIFI_CONNECT_CONFIRM && resultCode == android.app.Activity.RESULT_OK) {
			mWifiListHelper.fillWifiList();
		} else if (requestCode == WIFI_CONNECT_CONFIRM && resultCode != android.app.Activity.RESULT_CANCELED) {
			if (mWifiItemClick != null) {
				Toast.makeText(getActivity(), "Failed to connect to " + mWifiItemClick.getWifiName(), Toast.LENGTH_LONG).show();
			}
		}

		if (requestCode == WIFI_COMMENT && resultCode == android.app.Activity.RESULT_OK) {
			WifiComments commentCtn = (WifiComments) data.getSerializableExtra(WifiCommentActivity.WIFI_COMMENT_ADD);
			mWifiSquarePopup.addComment(commentCtn);
		}
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		// if (mSupplicantBRRegisterd) {
		// getActivity().unregisterReceiver(mSupplicantReceiver);
		// }

		// getActivity().unregisterReceiver(mLoginReceiver);

		super.onPause();
	}

	@Override
	public void onResume() {
		// mSupplicantReceiver = mWifiSupplicant.mReceiver;
		//
		// final IntentFilter filter = new
		// IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		// getActivity().registerReceiver(mSupplicantReceiver, filter);
		// mSupplicantBRRegisterd = true;

		IntentFilter loginFilter = new IntentFilter();
		loginFilter.addAction(LoginHelper.LOGIN_SUCCESS);
		// getActivity().registerReceiver(mLoginReceiver, loginFilter);

		super.onResume();
	}

	@Override
	public boolean onBackPressed() {
		if (mWifiSquarePopup.isShowing()) {
			mWifiSquarePopup.dismiss();
			return true;
		} else {
			return super.onBackPressed();
		}
	}

	

	private void showWifiConnectConfirmDialog(final WifiInfoScanned wifiInfoScanned, boolean beAuth) {
		mWifiConnectDialog.setTitle("连接到：" + wifiInfoScanned.getWifiName());
		mWifiConnectDialog.setSignal(String.valueOf(wifiInfoScanned.getWifiStrength()));
		if (beAuth) {
			mWifiConnectDialog.setSecurity("该WiFi已经认证，请放心愉快的使用！");
		} else {
			mWifiConnectDialog.setSecurity("该WiFi未经认证，请谨慎使用！");
		}

		mWifiConnectDialog.setLeftBtnStr("取消");
		mWifiConnectDialog.setRightBtnStr("确定");

		mWifiConnectDialog.setRightBtnListener(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				WifiBRService.setWifiSupplicant(true);

				boolean connResult = false;
				WifiConfiguration cfgSelected = mWifiAdmin.getWifiConfiguration(wifiInfoScanned);
				if (cfgSelected != null) {
					connResult = mWifiAdmin.connectToConfiguredNetwork(getActivity(), cfgSelected, true);
					// Log.d(TAG, "reconnect saved wifi with " +
					// wifiInfoScanned.getWifiName() + ", " +
					// wifiInfoScanned.getWifiPwd());
				} else {
					connResult = mWifiAdmin.connectToNewNetwork(getActivity(), wifiInfoScanned, true, false);
					// Log.d(TAG, "reconnect wifi with " +
					// wifiInfoScanned.getWifiName() + ", " +
					// wifiInfoScanned.getWifiPwd());
				}
				dialog.dismiss();
				if (!connResult) {
					Toast.makeText(getActivity(), "不能连接到网络：" + wifiInfoScanned.getWifiName() + ", 准备重启WiFi请稍后再试。", Toast.LENGTH_LONG).show();
					mWifiAdmin.closeWifi();
					mWifiAdmin.openWifi();
				}
			}
		});

		mWifiConnectDialog.setLeftBtnListener(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				dialog.dismiss();

			}
		});

		mWifiConnectDialog.show();
	}

	private void showWifiConnectPwdConfirmDialog(String title, final WifiInfoScanned wifiInfoScanned, final WifiConnectDialog wifiConnectDialog) {
		wifiConnectDialog.setTitle(title);
		wifiConnectDialog.setSignal(String.valueOf(wifiInfoScanned.getWifiStrength()));

		wifiConnectDialog.setLeftBtnStr("取消");
		wifiConnectDialog.setRightBtnStr("确定");
		wifiConnectDialog.clearPwdEditText();

		wifiConnectDialog.setRightBtnListener(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				boolean connResult = false;
				String inputedPwd = wifiConnectDialog.getPwdContent();
				if (inputedPwd.equals("")) {
					Toast.makeText(getActivity(), "请输入密码。", Toast.LENGTH_LONG).show();
					return;
				}

				isPwdConnect = true;
				WifiBRService.setWifiSupplicant(true);
				wifiInfoScanned.setWifiPwd(inputedPwd);
				connResult = mWifiAdmin.connectToNewNetwork(getActivity(), wifiInfoScanned, true, true);
				dialog.dismiss();
				if (!connResult) {
					Toast.makeText(getActivity(), "不能连接到网络：" + wifiInfoScanned.getWifiName() + ", 正在重启WiFi请稍后再试。", Toast.LENGTH_LONG).show();
					mWifiAdmin.closeWifi();
					mWifiAdmin.openWifi();
				}
			}
		});

		wifiConnectDialog.setLeftBtnListener(new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {

				dialog.dismiss();

			}
		});

		wifiConnectDialog.show();
	}

	private void refreshWifiTitleInfo(boolean open) {
		WifiInfo wifiCurInfo = mWifiAdmin.getWifiConnected();

		mWifiMaster.setText("");
		mWifiDesc.setText("");
		// update WiFi title info
		if (open && wifiCurInfo != null) {
			WifiInfoScanned wifiInfoCurrent = WifiListHelper.getInstance(getActivity()).mWifiInfoCur;
			if (wifiInfoCurrent != null && wifiInfoCurrent.getWifiLogo() != null) {
				mWifiLogoView.setImageBitmap(wifiInfoCurrent.getWifiLogo());
			} else {
				mWifiLogoView.setImageResource(R.drawable.icon_invalid);
			}
			if (wifiInfoCurrent != null && wifiInfoCurrent.getAlias() != null && wifiInfoCurrent.getAlias().length() > 0) {
				mWifiNameView.setText("已连接: " + wifiInfoCurrent.getAlias());
				mWifiMaster.setText(wifiInfoCurrent.getSponser());
				mWifiDesc.setText(wifiInfoCurrent.getBanner());
			} else {
				mWifiNameView.setText("已连接: " + WifiAdmin.convertToNonQuotedString(wifiCurInfo.getSSID()));
			}
			// mWifiNameView.setTextColor(Color.BLACK);

			mWifiSwitch.setVisibility(View.VISIBLE);
			mWifiSwitch.setChecked(true);
			mWifiSquareLayout.setVisibility(View.VISIBLE);
			mWifiTitleLayout.setClickable(true);
		} else {
			mWifiNameView.setText("未连接任何WiFi");
			// mWifiNameView.setTextColor(Color.GRAY);
			mWifiLogoView.setImageResource(R.drawable.icon_invalid);

			mWifiSwitch.setVisibility(View.GONE);
			mWifiSwitch.setChecked(false);
			mWifiSquareLayout.setVisibility(View.GONE);
			mWifiTitleLayout.setClickable(false);
		}

		// forget last WiFi connected configuration info
		if (mLastWifiInfo != null && mLastWifiInfoScanned != null && mLastWifiInfoScanned.isAuthWifi() && !mLastWifiInfoScanned.isLocalSave()) {
			Log.i(TAG, "erase WiFi config which has been shared and not local save");
			mWifiAdmin.forgetNetwork(mLastWifiInfo);
		}
		mLastWifiInfo = wifiCurInfo;
		mLastWifiInfoScanned = mWifiListHelper.mWifiInfoCur;
	}

	

	private void setWifiSquareListener(LinearLayout wifiSquareLayout) {
		TextView wifiSpeed = (TextView) wifiSquareLayout.findViewById(R.id.wifi_speed);
		wifiSpeed.setOnClickListener(mWifiSquareClickListener);

		TextView wifiShare = (TextView) wifiSquareLayout.findViewById(R.id.wifi_share);
		wifiShare.setOnClickListener(mWifiSquareClickListener);

		TextView wifiLouder = (TextView) wifiSquareLayout.findViewById(R.id.wifi_louder);
		wifiLouder.setOnClickListener(mWifiSquareClickListener);
	}
}
