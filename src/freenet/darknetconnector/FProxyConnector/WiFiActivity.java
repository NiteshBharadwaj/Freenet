package freenet.darknetconnector.FProxyConnector;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;

public class WiFiActivity extends Fragment {
	private WifiP2pManager manager;
	private Channel channel;
	private WifiP2pGroup group;
	private boolean p2penabled = false;
	private View view = null;
	private boolean connected = false;
	private final IntentFilter intentFilter = new IntentFilter();
	private IntentReceiver receiver;
	private Activity activity;
	private Boolean suc = false;
	public class IntentReceiver extends BroadcastReceiver {
		private WifiP2pManager manager;
	    private Channel channel;
	    
	    public IntentReceiver(WifiP2pManager manager, Channel channel, Activity activity) {
	    	this.manager = manager;
	    	this.channel= channel;
	    }
	    public void connect(WifiP2pDevice device) {

	        WifiP2pConfig config = new WifiP2pConfig();
	        config.deviceAddress = device.deviceAddress;
	        config.wps.setup = WpsInfo.PBC;
	        manager.connect(channel, config, new ActionListener() {

	            @Override
	            public void onSuccess() {
	                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
	            	Log.d("done","connected");
	            	connected =true;
	            }

	            @Override
	            public void onFailure(int reason) {
	                
	            }
	        });

	    }
		private PeerListListener p2pPeerListListener = new PeerListListener() {

			@Override
			public void onPeersAvailable(WifiP2pDeviceList peerList) {
				// TODO Auto-generated method stub
				 // Out with the old, in with the new.
	            for (WifiP2pDevice device: peerList.getDeviceList()) {
	            	Log.d("hero",device.deviceName);
	            	if (device!=null){
	            		connect(device);
	            		break;
	            	}
	            	
	            }

	           
			}
		};
		private GroupInfoListener groupInfoListener = new GroupInfoListener() {

			@Override
			public void onGroupInfoAvailable(WifiP2pGroup group) {
				
			}

		};
		private ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener() {

			@Override
			public void onConnectionInfoAvailable(WifiP2pInfo info) {
				Log.d("hell",info.groupOwnerAddress.toString());
				
			}
			
		};
		@Override
		public void onReceive(Context context, Intent intent) {
			String action= intent.getAction();
		    if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

		        // Request available peers from the wifi p2p manager. This is an
		        // asynchronous call and the calling activity is notified with a
		        // callback on PeerListListener.onPeersAvailable()
		        if (manager != null && connected==false) {
		            manager.requestPeers(channel, p2pPeerListListener);
		        }
		        if (connected) {
		        	manager.requestConnectionInfo(channel, connectionInfoListener);
		        }
		        Log.d("ok", "P2P peers changed");
		    }
			
		}
	}
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = inflater.inflate(R.layout.activity_fproxy_connector, null);
		this.activity = getActivity();
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

	    // Indicates a change in the list of available peers.
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

	    // Indicates the state of Wi-Fi P2P connectivity has changed.
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

	    // Indicates this device's details have changed.
	    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		manager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
		channel = manager.initialize(activity, activity.getMainLooper(), null);
		Button exchange_button = (Button) view.findViewById(R.id.exchange_noderef_button);
		exchange_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				starter();
			}
		});
		return view;
	}
    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new IntentReceiver(manager,channel,activity);
        activity.registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        activity.unregisterReceiver(receiver);
    }
    
    public void starter() {
    	manager.createGroup(channel, new ActionListener() {
			@Override
            public void onSuccess() {
                suc = true;
                Log.d("p1","1");
            }

            @Override
            public void onFailure(int reasonCode) {
                suc = false;
                Log.d("p1","no");
                Log.d("p1","" +reasonCode);
                
            }
		}); 
    	if (!suc)return;
    	manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

	        @Override
	        public void onSuccess() {
	            // Code for when the discovery initiation is successful goes here.
	            // No services have actually been discovered yet, so this method
	            // can often be left blank.  Code for peer discovery goes in the
	            // onReceive method, detailed below.
	        	Log.d("yes","4");
	        }

	        @Override
	        public void onFailure(int reasonCode) {
	            // Code for when the discovery initiation fails goes here.
	            // Alert the user that something went wrong.
	        	Log.d("yes","5");
	        }
    	});
    }
}
