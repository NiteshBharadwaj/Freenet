package freenet.darknetconnector.NodeReferenceExchange;

import java.net.InetAddress;
import java.net.UnknownHostException;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;
import freenet.darknetconnector.DarknetAppConnector.R;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class WifiDirectActivity extends Fragment {
	private static WifiP2pManager manager;
	public static String TAG = "WifiDirectActivity";
	private static Channel channel;
	private Activity uiActivity;
	private boolean isWifiP2pEnabled = false;
	private boolean discoveryFailure = false;
	private long lastDiscovery = 0;
	private MyBroadcastReceiver receiver = null;
	private final IntentFilter intentFilter = new IntentFilter();
	private DevicesArrayAdapter devicesArrayAdapter;
	private static ServerThread serverThread = null;
	private static ClientThread clientThread = null;
	private static boolean successful = false;
	private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
        	if (devicesArrayAdapter == null) {
        		Log.d(WifiDirectActivity.TAG, "Discovery couldn't be initiated");
        		return;
        	}
        	devicesArrayAdapter.clear();
        	
            for(WifiP2pDevice device : peerList.getDeviceList()) {
            	devicesArrayAdapter.add(device);
            }
            if (devicesArrayAdapter.isEmpty()) {
                Log.d(WifiDirectActivity.TAG, "No devices found");
                return;
            }
            updatePeers();
        }

    };
    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            Log.d("dumb","clicked");
            WifiP2pDevice device = devicesArrayAdapter.getDevice(position);
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            config.wps.setup = WpsInfo.PBC;
            manager.connect(channel, config, new ActionListener() {

                @Override
                public void onSuccess() {
                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(WifiDirectActivity.TAG,"Connection Failed");
                }
            });
            
        }
    };
    private ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener() {
    	
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo info) {
			Log.d(WifiDirectActivity.TAG,"connection Info Available");
			InetAddress groupOwnerAddress = null;
			try {
				groupOwnerAddress = InetAddress.getByName(info.groupOwnerAddress.getHostAddress());
			} catch (UnknownHostException e) {
				Log.e(WifiDirectActivity.TAG,"Impossible to arrive here",e);
			}
			if (info.groupFormed && info.isGroupOwner) {
				// We are the group owner of this connection
				if (serverThread!=null) serverThread.cancel();
				if (clientThread!=null) clientThread.cancel();
				serverThread = new ServerThread(groupOwnerAddress);
				serverThread.start();
				Log.d(WifiDirectActivity.TAG,"server started with me as group owner");
			}
			else if (info.groupFormed) {
				// We are not the group owner but we know the group owner ip
				if (serverThread!=null) serverThread.cancel();
				if (clientThread!=null) clientThread.cancel();
				clientThread = new ClientThread(groupOwnerAddress);
				Log.d(WifiDirectActivity.TAG,"client started and not group owner");
			}
		}
    	
    };
	private class DevicesArrayAdapter extends ArrayAdapter<String>{
		private ArrayAdapter<WifiP2pDevice> availablePeers;
		public DevicesArrayAdapter(Context context, int resourceId) {
			super(context,resourceId);
			availablePeers = new ArrayAdapter<WifiP2pDevice>(context,resourceId);
		}       
        public void add(WifiP2pDevice device) {
        	super.add(device.deviceName +" - "+ device.deviceAddress);
        	availablePeers.add(device);
        }
        public WifiP2pDevice getDevice(int position) {
        	return availablePeers.getItem(position);
        }
        @Override
        public void clear() {
        	super.clear();
        	availablePeers.clear();
        }
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        uiActivity = DarknetAppConnector.activity;
        manager = (WifiP2pManager) uiActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(uiActivity, uiActivity.getMainLooper(), null);
        View view = inflater.inflate(R.layout.exchange_wifi_direct_layout,null);
        return view;
	}
	
	protected void updatePeers() {
        ListView newDevicesListView = (ListView) uiActivity.findViewById(R.id.wifi_direct_new_devices);
        if (newDevicesListView==null) Log.d(WifiDirectActivity.TAG,"new devices list view is null");
        if (devicesArrayAdapter == null) Log.d(WifiDirectActivity.TAG,"devices array adapter not yet obtained");
        newDevicesListView.setAdapter(devicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);	
	}

	private class MyBroadcastReceiver extends BroadcastReceiver {
		
		public MyBroadcastReceiver() {
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
		        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
		        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
		            isWifiP2pEnabled = true;
		            startDiscovery();
		        } else {
		            isWifiP2pEnabled = false;
		        }
		    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

	            // The peer list has changed
		    	if (manager != null) {
		            manager.requestPeers(channel,peerListListener);
		        }
		        Log.d(WifiDirectActivity.TAG, "P2P peers changed");

	        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
	        	Log.d(WifiDirectActivity.TAG,"Requested Connection Info for a connection 1");
	        	if (manager == null) {
	                return;
	            }
	            NetworkInfo networkInfo = (NetworkInfo) intent
	                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
	            Log.d(WifiDirectActivity.TAG,"Requested Connection Info for a connection 2");
	            if (networkInfo.isConnected()) {
	            	Log.d(WifiDirectActivity.TAG,"Requested Connection Info for a connection 3");
	                manager.requestConnectionInfo(channel, connectionInfoListener);
	            }

	        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
	            //DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
	              //      .findFragmentById(R.id.frag_list);
	            WifiP2pDevice device = ((WifiP2pDevice) intent.getParcelableExtra(
	                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
	            String deviceName = device.deviceName;
	            String status = getDeviceStatus(device.status);
	            Log.d(TAG,deviceName);
	            Log.d(TAG,status);
	        }
			
		}
	}
	
	private static String getDeviceStatus(int deviceStatus) {
        Log.d(WifiDirectActivity.TAG, "Peer status :" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }
	private void startDiscovery() {
		manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

	        @Override
	        public void onSuccess() {
	            Log.d(WifiDirectActivity.TAG,"Discovery initiated successfully");
	            discoveryFailure = false;
	            lastDiscovery = System.currentTimeMillis();
	            devicesArrayAdapter = new DevicesArrayAdapter(uiActivity, R.layout.device_name);
	        }
	        @Override
	        public void onFailure(int reasonCode) {
	            Log.d(WifiDirectActivity.TAG,"Discovery couldn't be initiated");
	            discoveryFailure = true;
	        }
		});
		
	}
	public static boolean closeActivity() {
		if (serverThread!=null) serverThread.cancel();
		if (clientThread!=null) clientThread.cancel();
		if (manager!=null) {
			manager.cancelConnect(channel, null);
		}
		DarknetAppConnector.fragmentManager.popBackStack();
		Log.d(WifiDirectActivity.TAG,"closed the wifi direct fragment");
		return successful;
	} 
	
    @Override
    public void onResume() {
        super.onResume();
        if(receiver==null) {
        	receiver = new MyBroadcastReceiver();
        	uiActivity.registerReceiver(receiver, intentFilter);
        }
        if (discoveryFailure && System.currentTimeMillis()-lastDiscovery>2000) {
        	startDiscovery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (receiver!=null)
        	try {
        		uiActivity.unregisterReceiver(receiver);
        	} catch (Exception e){
        		}
        	}
}
