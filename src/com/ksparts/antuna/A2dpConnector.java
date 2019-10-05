package com.ksparts.antuna;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

@SuppressLint("NewApi")
public class A2dpConnector {
   /**
   * Callback invoked when the connector successfully connects an A2DP device.
   */
	public interface Callback {
		void onConnected(BluetoothDevice device);
		void onFail(BluetoothDevice device, int State);
	}

	private Callback nullCallback = new Callback() {
		@Override
		public void onConnected(BluetoothDevice device) {}
		@Override
		public void onFail(BluetoothDevice device, int State) {}
	};
	public final boolean b = true;
	private final Context context;
	public final BluetoothAdapter mBluetoothAdapter;
	
	
	public A2dpConnector(Context context, BluetoothAdapter mBluetoothAdapter) {
		this.context = context;
		this.mBluetoothAdapter = mBluetoothAdapter;
	}

	public void connect(final BluetoothDevice device) {
		connect(device, nullCallback);
	}
  
	public void connect(final BluetoothDevice device, final Callback callback) {
		BluetoothProfile.ServiceListener mProfileListener 
		= new BluetoothProfile.ServiceListener() {
			private BluetoothA2dp mBluetoothA2dp;

			public void onServiceConnected(int profile, BluetoothProfile proxy) {
				if(profile == BluetoothProfile.A2DP) {
					Log.w("A2DP","Connect 1");
					mBluetoothA2dp = (BluetoothA2dp) proxy;
					if (BluetoothProfile.STATE_CONNECTED == mBluetoothA2dp.getConnectionState(device)) {
						// Nothing to do.
						Log.w("A2DP","Connect 2");
						callback.onConnected(device);
						Log.i("A2dp","Bluetooth_A2DP Connected"+mBluetoothA2dp.getConnectionState(device)
								+"/"+BluetoothProfile.STATE_CONNECTED);
						return;
					}
					// connect Method is hidden. So we must use reflection.
					Class<?> c = mBluetoothA2dp.getClass();
					try {
						Log.w("A2DP","Connect 3");
						Method m = c.getMethod("connect", BluetoothDevice.class);
						Object[] args = new Object[1];
						args[0] = device;
						m.invoke(mBluetoothA2dp, args);
					} catch (NoSuchMethodException e) {
						Log.w("A2DP","Connect 4");
						// This means the method does not exist on this android SDK version.
						// TODO(jaimeyap): Figure out a decent way to do error handling on
						// Android.
						e.printStackTrace();
						callback.onFail(device,BluetoothProfile.STATE_DISCONNECTED);
					} catch (Exception e) {
						Log.w("A2DP","Connect 5");
						// TODO(jaimeyap): Figure out a decent way to do error handling on
						// Android.
						e.printStackTrace();
						callback.onFail(device,BluetoothProfile.STATE_DISCONNECTED);
					} finally {
						Log.w("A2DP","Connect 6");
						// Close proxy connection after use.
						mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP,
								mBluetoothA2dp); 
					}
//					callback.onConnected(device);
				}
			}

			public void onServiceDisconnected(int profile) {
				Log.w("A2DP","Connect 1");
				if (profile == BluetoothProfile.A2DP) mBluetoothA2dp = null;	
				callback.onFail(device,BluetoothProfile.STATE_DISCONNECTED);
			}
		};

		// Establish connection to the proxy.
		mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.A2DP);
	}
	public void disconnect(final BluetoothDevice device) {
		BluetoothProfile.ServiceListener mProfileListener 
		= new BluetoothProfile.ServiceListener() {
			private BluetoothA2dp mBluetoothA2dp;

			public void onServiceConnected(int profile, BluetoothProfile proxy) {
				if (profile == BluetoothProfile.A2DP) {
					mBluetoothA2dp = (BluetoothA2dp) proxy;
					if (BluetoothProfile.STATE_DISCONNECTED == mBluetoothA2dp.getConnectionState(device)) {
						// Nothing to do.
						Log.i("A2dp","Bluetooth_A2DP Disconnected");
						return;
					}
					// connect Method is hidden. So we must use reflection.
					Class<?> c = mBluetoothA2dp.getClass();
					try {
						Method m = c.getMethod("disconnect", BluetoothDevice.class);
						Object[] args = new Object[1];
						args[0] = device;
						m.invoke(mBluetoothA2dp, args);
					} catch (NoSuchMethodException e) {
						// This means the method does not exist on this android SDK version.
						// TODO(jaimeyap): Figure out a decent way to do error handling on
						// Android.
						e.printStackTrace();
					} catch (Exception e) {
						// TODO(jaimeyap): Figure out a decent way to do error handling on
						// Android.
						e.printStackTrace();
					} finally {
						// Close proxy connection after use.
						mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP,
								mBluetoothA2dp); 
					}
				}
			}

			public void onServiceDisconnected(int profile) {
				if (profile == BluetoothProfile.A2DP) mBluetoothA2dp = null;
			}
		};

		// Establish connection to the proxy.
		mBluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.A2DP);
	}
}