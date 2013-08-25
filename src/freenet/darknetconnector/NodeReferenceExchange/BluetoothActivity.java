package freenet.darknetconnector.NodeReferenceExchange;

import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;
import freenet.darknetconnector.DarknetAppConnector.R;

public class BluetoothActivity extends Fragment {
	private static BluetoothAdapter bluetoothAdapter;
	static final int BLUETOOTH_DISCOVERY_ID = 100;
	static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static String TAG = "BluetoothActivity";
	private Activity uiActivity;
	private static boolean isBluetoothPreviouslyDisabled;
	private static ServerThread serverThread;
	private static ClientThread clientThread;
	private static ConnectionThread connectionThread;
	private ListView listView;
	private DiscoveryResultReceiver discoveryResultReceiver;
	private ArrayAdapter<String> devicesArrayAdapter;
	private static boolean isSuccessful = false;
	private boolean activityStarted = false;
	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            // Cancel discovery because it's costly and we're about to connect
            Log.d("dumb","clicked");
        	bluetoothAdapter.cancelDiscovery();
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            Log.d("dumb","becoming client");
            clientThread = new ClientThread(device,bluetoothAdapter);
            clientThread.start();
        }
    };
    
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		View view = inflater.inflate(R.layout.echange_bluetooth_layout,null);
		// Check bluetooth
		if (bluetoothAdapter == null) {
		    // TODO: Put message that bluetooth is not supported in this device
			view = inflater.inflate(R.layout.empty_layout, null);
			closeActivity();
		}
		if (!bluetoothAdapter.isEnabled()) isBluetoothPreviouslyDisabled = true;
		listView = (ListView) view.findViewById(R.id.bluetooth_new_devices);
		return view;
		/*Button discoveryButton = (Button) uiActivity.findViewById(R.id.bluetooth_discovery_button);
		discoveryButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (bluetoothAdapter.isDiscovering()) {
					bluetoothAdapter.cancelDiscovery();
				}
				bluetoothAdapter.startDiscovery();
			}
		});
		discoveryButton.setFocusable(false);
		discoveryButton.setFocusableInTouchMode(false);*/
	}
	 @Override
	 public void onStart() {
		 super.onStart();
		// Check if bluetooth is discoverable or not
			if(bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 150);
			    startActivityForResult(discoverableIntent, BluetoothActivity.BLUETOOTH_DISCOVERY_ID);
			    Log.d("dumb","trying to make discoverable");
			}
			else {
				activityStarted = true;
				startServer();
			}
	 }
	@Override
	public void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
        Log.d("dumb","activity result");
		super.onActivityResult(requestCode, resultCode, data);
		 Log.d("dumb","activity result 1"); 
        if (requestCode == BluetoothActivity.BLUETOOTH_DISCOVERY_ID) {
        	if (resultCode == uiActivity.RESULT_CANCELED) {
        		 closeActivity();
        		 Log.d("dumb","activity result 2");
        	}
        	else if (resultCode == 150) {
        		activityStarted = true;
        		startServer();
        		 Log.d("dumb","activity result 3");
        	}
        }
	}
	
	public static boolean closeActivity() {
		if (serverThread!=null) serverThread.cancel();
		if (clientThread!=null) clientThread.cancel();
		if (connectionThread!=null) connectionThread.cancel();
		if (bluetoothAdapter!=null) {
			if (bluetoothAdapter.isDiscovering())
				bluetoothAdapter.cancelDiscovery();
			if (isBluetoothPreviouslyDisabled)
				bluetoothAdapter.disable();
		}
		DarknetAppConnector.fragmentManager.popBackStack();
		return isSuccessful;
	}

	@Override
    public synchronized void onResume() {
        super.onResume();
        // The app was paused to ask the user whether to make the device discoverable
        // Now user has allowed it and our app is resumed
        if (activityStarted && serverThread==null)
        startServer();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(discoveryResultReceiver!=null)
			try {
				uiActivity.unregisterReceiver(discoveryResultReceiver);
			} catch (Exception e) {
				
			}
	}
	// Starts the server to accept bluetooth connections and also discovers nearby devices
	private void startServer() {

		uiActivity = DarknetAppConnector.activity;
		devicesArrayAdapter = new ArrayAdapter<String>(uiActivity, R.layout.device_name);
		if (serverThread!=null) serverThread.cancel();
        serverThread = new ServerThread(bluetoothAdapter);
        serverThread.start();
        Log.d("dumb","server started");
        bluetoothAdapter.startDiscovery();
        
		if (discoveryResultReceiver!=null) {
			uiActivity.unregisterReceiver(discoveryResultReceiver);
			discoveryResultReceiver = null;
		}
		discoveryResultReceiver = new DiscoveryResultReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        uiActivity.registerReceiver(discoveryResultReceiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        uiActivity.registerReceiver(discoveryResultReceiver, filter);
        
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        uiActivity.registerReceiver(discoveryResultReceiver, filter);
        
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        uiActivity.registerReceiver(discoveryResultReceiver, filter);
        
        filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        uiActivity.registerReceiver(discoveryResultReceiver, filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        uiActivity.registerReceiver(discoveryResultReceiver,filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        uiActivity.registerReceiver(discoveryResultReceiver,filter);
        
        filter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        uiActivity.registerReceiver(discoveryResultReceiver,filter);
	}	
	
	protected void updatePeers() { 
        listView.setAdapter(devicesArrayAdapter);
        listView.setOnItemClickListener(mDeviceClickListener);
	}
	private class DiscoveryResultReceiver extends BroadcastReceiver {
		
		public DiscoveryResultReceiver() {
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
	        // When discovery finds a device
	        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
	            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	            devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
	            Log.d("dumb","found a device");
	        }
	        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
	        	Log.d("dumb","present state -  connected to a device");
	        }
	        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
	        	Log.d("dumb","present state -  disconnected to a device");
	        	if (connectionThread!=null)
	        	connectionThread.cancel();
	        }
	        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
	        	Log.d("dumb","present state -  disconnect requested");
	        }
	        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	updatePeers();
	        }
	        else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
	        	Log.d("dumb","discovery started");
	        }
	        else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
	        	if (BluetoothAdapter.EXTRA_SCAN_MODE.equals(BluetoothAdapter.SCAN_MODE_CONNECTABLE) && BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE.equals(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)) {
	        		Log.d("dumb","Undiscoverable, connectable");
	        	}
	        	else {
	        		Log.d("dumb","Discoverablity status changed");
	        	}
	        }
	        else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
	        	if (BluetoothAdapter.EXTRA_CONNECTION_STATE.equals(BluetoothAdapter.STATE_CONNECTING)) {
	        		Log.d("dumb","present state -  connecting");
	        	}
	        	else if (BluetoothAdapter.EXTRA_CONNECTION_STATE.equals(BluetoothAdapter.STATE_CONNECTED)){
	        		Log.d("dumb","present state -  connected");
	        	}
	        	else if (BluetoothAdapter.EXTRA_CONNECTION_STATE.equals(BluetoothAdapter.STATE_DISCONNECTED)){
	        		Log.d("dumb","present state -  disconnected");
	        	}
	        	else if (BluetoothAdapter.EXTRA_CONNECTION_STATE.equals(BluetoothAdapter.STATE_DISCONNECTING)){
	        		Log.d("dumb","present state -  disconnecting");
	        	}
	        	else {
	        		Log.d("dumb","present state -  unknown");
	        	}
	        }
		}
	}
	public static void manageConnectedSocket(BluetoothSocket socket,boolean isClientThread) {
		if (isClientThread) {
			if (serverThread!=null) {
				serverThread.cancel();
			}
			if (connectionThread!=null){
				connectionThread.cancel();
			}
			
			Log.d("dumb","starting connection thread");
			connectionThread = new ConnectionThread(socket);
			connectionThread.start();
		}
		else {
			// If there is another RF communication running
			if (connectionThread!=null) {
				connectionThread.cancel();
			}
			
			// The server has accepted a connection. We need not become client to none now
			if (clientThread!=null) {
				clientThread.cancel();
			}
			
			Log.d("dumb","Starting connecjtion thread");
			connectionThread = new ConnectionThread(socket);
			connectionThread.start();
		}
	}

}
