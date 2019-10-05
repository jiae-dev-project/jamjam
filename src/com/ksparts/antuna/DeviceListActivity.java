package com.ksparts.antuna;

import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
	
	
	
    // Debugging
    private static final String TAG = "AUDIO/DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    // Name of the connected device
//    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
//    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
//    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
//    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
//    private BluetoothChatService mChatService = null;

    // Intent request codes
//    private static final int REQUEST_CONNECT_DEVICE = 1;
//    private static final int REQUEST_ENABLE_BT = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public final int HANDLER_ITEM_CLICK = 10;
    public final int HANDLER_ON_CLICK = 11;
    
    public String device_address = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

       
        
        // Set result CANCELED incase the user backs out
        

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setMaxHeight(30);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	
            	final Intent i = new Intent();
				i.setAction("DeviceListActivity");
				sendBroadcast(i);	
            	startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
        });
        

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
            	//if(device.getName().toCharArray()[0] == 'K' || device.getName().toCharArray()[0] == 'B')
                	mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            	
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            if(noDevices.toCharArray()[0] == 'K'){
            	mPairedDevicesArrayAdapter.add(noDevices);
            }
        }
    }
    
    @SuppressLint("HandlerLeak")
	public final Handler dHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_ITEM_CLICK:
				Log.e("AUDIO/"+"DeviceListActivity","Address:"+device_address);
	            mBtAdapter.cancelDiscovery();
	            Intent intent = new Intent();
	            intent.putExtra(EXTRA_DEVICE_ADDRESS, device_address);
				setResult(Activity.RESULT_OK, intent);
//	            overridePendingTransition(R.anim.alpha_out, R.anim.alpha_in);
	            finish();
				break;
			case HANDLER_ON_CLICK:
				final Intent i = new Intent();
				i.setAction("DeviceListActivity");
				sendBroadcast(i);	
            	startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
				break;
				
			}
    	}
    };
    
   
    public void onRestart(){
    	super.onRestart();
    	finish();
    }
    
    public void FINISHI(){
    	onDestroy();
    	finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
        	
        	String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            device_address = address;
        	
            mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            info = ((TextView) v).getText().toString();
            address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            
            switch (av.getId()) {
			case R.id.new_devices:
				setResult(Activity.RESULT_FIRST_USER, intent);
				break;

			case R.id.paired_devices:
				setResult(Activity.RESULT_OK, intent);
				break;
			}
            
            
            // Set result and finish this Activity
            overridePendingTransition(R.anim.alpha_out, R.anim.alpha_in);
            finish();
        }
    };
    
    

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("  Select please");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			finish();
			break;
		
		}
		return super.onKeyDown(keyCode, event);
	}
    
    

}