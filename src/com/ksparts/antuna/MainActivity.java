package com.ksparts.antuna;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	final private String TAG = "BluetoothMain";

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_STATE_CHANGE_A2DP = 8;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int MESSAGE_RECIVE_SET = 6;
	public static final int MESSAGE_RECIVE_MOD = 7;
	
	public static final int VIEW_CONNECT = 10;
	public static final int VIEW_INIT = 11;
	
	public static int VIEW_STATUS  = 0;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	final private int REQUEST_CODE_INTRO = 1;
	final private int REQUEST_CODE_DEVICELIST = 2;

	private ImageButton mButton_connect;
	private RelativeLayout mView_connect;
	private EditText mEdit_text;
	private Button mButton_send, mButton1, mButton2, mButton3;
	private View mView_init;
	private ListView mList;
	
	private ArrayAdapter<String> mArrayAdapter;
	private ArrayList<String> mArrayList = new ArrayList<String>();

	private SharedPreferences prefs;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothDevice mBluetoothDevice;
	private BluetoothA2dp mBluetoothA2dp;
	BluetoothChatService mChatService = null;

	String mDeviceAddress = "";
	private ProgressDialog PDlg = null;
	private A2dpConnector.Callback mA2dpCallback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO onCreate
		super.onCreate(savedInstanceState);

		Log.w(TAG, "++++ onCreate : Intro ++++");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();

		// 블르투스 연결되어있을시에 정보를 가져오는 리스너
		// mBluetoothAdapter.getProfileProxy(this, mProfileListener,
		// BluetoothProfile.A2DP);

		IntentFilter filter = new IntentFilter();// new
													// IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY + 1);
		registerReceiver(mReceiver, filter); // Don't forget to unregister
												// during onDestroy
		Layout_setting();
		setupChat();
		Select_View(VIEW_INIT);

	}

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			// When discovery finds a device
			Log.e(TAG, "BR Action : " + action);

		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_CODE_DEVICELIST:
			if (resultCode == RESULT_OK) {

				if (PDlg != null)
					PDlg.setTitle("BlueTooth Spp Connect");
				PDlg.setMessage("CONNECTING.....");
				PDlg.show();

				mDeviceAddress = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);

				// Get the BLuetoothDevice object
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(mDeviceAddress);

				mChatService.connect(device);

				break;

			}
			break;
		}

	};

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Log.w(TAG,"onClick : "+v.getId());

		switch (v.getId()) {

		case R.id.button_connect:
			if (!mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.enable();
				Toast.makeText(this, "Bluetooth is On, Please retuch",
						Toast.LENGTH_LONG).show();
				break;
			}
			Intent intro_intent = new Intent(MainActivity.this,
					DeviceListActivity.class);
			// intro_intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivityForResult(intro_intent, REQUEST_CODE_DEVICELIST);

			break;
			
		case R.id.button_send:
			
			String write_message = mEdit_text.getText().toString();
			
			try {
				byte[] bytes = write_message.getBytes("UTF-8");
				if(bytes!=null)
				mChatService.write(bytes);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
			
		case R.id.edit_text:
//			mEdit_text.setSelected(true);
			mEdit_text.selectAll();
			
			break;
			
		case R.id.button1:
			mArrayList.clear();
			mArrayAdapter.notifyDataSetChanged();
			break;
			
		case R.id.button2:
			mButton_connect.performClick();
			break;
			
		case R.id.button3:
			mArrayList.add("Receive : A");
			mArrayAdapter.notifyDataSetChanged();
			break;

		}

	}

	public final Handler mHandler = new Handler() {
		// TODO Handler
		@SuppressLint("ShowToast")
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE: // 블루투스의 상태가 변했을 떄
				Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED: // SPP연결 되었을 때;
					
					Select_View(VIEW_INIT);
					
					if (PDlg != null)
						PDlg.dismiss();
					Log.e(TAG, "SPP CONNECTED");

					break;
				case BluetoothChatService.STATE_CONNECTING:
					Log.i(TAG, "BlueTooth Connecting...");
					if (PDlg != null)
						PDlg.setTitle("BlueTooth Spp Connect");
					PDlg.setMessage("CONNECTING.....");
					PDlg.show();
					break;
				case BluetoothChatService.STATE_LISTEN:
					Log.e(TAG, "SPP STATE_LISTEN");
					break;
				case BluetoothChatService.STATE_NONE:
					Log.e(TAG, "SPP STATE_NONE");
					break;
				case BluetoothChatService.STATE_FAIL:
					if (PDlg != null)
						PDlg.dismiss();
					Toast.makeText(getApplicationContext(),
							"Bluetooth Connect Fail", 1).show();
					Log.e(TAG, "SPP STATE_FAIL");
					break;
				}
				break;
			case MESSAGE_WRITE:
				break;
			case MESSAGE_READ:
				
				try {
					byte[] readBuf = (byte[]) msg.obj;
					String value = new String(readBuf);
					
					if(readBuf != null){
						Log.i(TAG,"MESSAGE_READ : " +value);
						mArrayList.add("Receive : "+value);
						mArrayAdapter.notifyDataSetChanged();
					}
				} catch (Exception e) {
					// TODO: handle exception
					Log.e(TAG,"MESSAGE_READ Error");
				}
				
				break;
			case MESSAGE_DEVICE_NAME:// save the connected device's name
				String mConnectedDeviceName = msg.getData().getString(
						DEVICE_NAME);
				break;
			case MESSAGE_TOAST:
				Log.e(TAG, "Message _ Toast");
				String temp_str = "Device connection was lost";
				if (temp_str.equals(msg.getData().getString(TOAST)) == true) {
					Toast.makeText(getApplicationContext(), msg.getData()
							.getString(TOAST), Toast.LENGTH_SHORT);
					Log.e(TAG, msg.getData().getString(TOAST));
					Log.e(TAG, "SELECT_MODE != MODE_BT");
				}
				break;
			}
		}
	};
	
	private void Select_View(int i){
		if(i == VIEW_CONNECT){
			
			mView_connect.setVisibility(View.VISIBLE);
			mView_init.setVisibility(View.GONE);
			
		}else if(i== VIEW_INIT){
			
			mView_connect.setVisibility(View.GONE);
			mView_init.setVisibility(View.VISIBLE);
			
		}
		
		
	}
	
	

	private void setupChat() {
		Log.d(TAG, "setupChat()");
		if (mChatService == null)
			mChatService = new BluetoothChatService(this, mHandler, null);
	}

	public void onResume() {
		super.onResume();
		Log.w(TAG, "++++ onResume : Intro ++++");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.w(TAG, "++++ onDestroy : Intro ++++");
		saveLast();
		unregisterReceiver(mReceiver);
		// mBluetoothAdapter.disable();
	}

	private void getLast() {
		prefs = getSharedPreferences("pStatus_src", MODE_PRIVATE);
		int START = prefs.getInt("START", 0);

	}

	private void saveLast() {
		prefs = getSharedPreferences("pStatus_src", MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		int START = 1;
		ed.putInt("START", START);
		ed.commit();

	}

	private void Layout_setting() {

		PDlg = new ProgressDialog(this);

		mButton_connect = (ImageButton) findViewById(R.id.button_connect);
		
		mButton_send = (Button) findViewById(R.id.button_send);
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mButton3 = (Button)	findViewById(R.id.button3);
		
		mView_init = (View) findViewById(R.id.view_init);
		mView_connect = (RelativeLayout) findViewById(R.id.view_connect);
		
		mEdit_text = (EditText) findViewById(R.id.edit_text);
		
		mList = (ListView) findViewById(R.id.list);
		
		mButton_connect.setOnClickListener(this);
		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);
		mButton3.setOnClickListener(this);
		mButton_send.setOnClickListener(this);
		
		
		mEdit_text.setOnClickListener(this);
		
		mEdit_text.setSelectAllOnFocus(true);
		
		
		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mArrayList);
		mList.setAdapter(mArrayAdapter);
	}

}
