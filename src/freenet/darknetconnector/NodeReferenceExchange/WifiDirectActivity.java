/**
 * Fragment that manages wifi direct connection
 * On device versions >= 14 (ICS)
 * Both devices start a group and show their MAC, SSID etc as a QR code
 * If it receives the details about other device by scanning QR or as nfc message, it kills its own group and connects to the peer
 * Also supports discovery of new devices and connections by conventional process 
 */
package freenet.darknetconnector.NodeReferenceExchange;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;
import freenet.darknetconnector.DarknetAppConnector.QRDisplayActivity;
import freenet.darknetconnector.DarknetAppConnector.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
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
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class WifiDirectActivity extends Fragment {
	
	private static final int WIFI_DIRECT_QR_REQUEST_CODE =301;
	public static final int WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE =303;
	public static final int MESSAGE_NETWORK_CONNECTED = 302;
	private static final int MESSAGE_NDEF_UI = 304;
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
	private static LegacyWifiHelper wifiHelper= null;
	private static boolean successful = false;
	private String passkey = "";
	private String MAC_ID = "";
	private String mySSID = "";
	private String myIP = "";
	private boolean groupRunningWithMeAsGroupOwner = false;
	private WifiP2pDevice friendDevice;
	public static ConnectionInfoHandler handler;
	private boolean groupStartingWithMeAsClient = false;
	/**
	 * Listener to listen for newly discovered peers 
	 */
	private PeerListListener peerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
        	if (devicesArrayAdapter == null) {
        		Log.d(WifiDirectActivity.TAG, "Devices Array is null - reached here without discovery");
        		devicesArrayAdapter = new DevicesArrayAdapter(uiActivity, R.layout.device_name);
        	}
        	devicesArrayAdapter.clear();
        	Log.d(WifiDirectActivity.TAG,"adding peers");
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
    // If there is a wifi direct group, we must remove it to connect as a client
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int position, long id) {
            Log.d(WifiDirectActivity.TAG,"clicked to connect to a peer");
            friendDevice = devicesArrayAdapter.getDevice(position);
            manager.removeGroup(channel, new ActionListener() {

                @Override
                public void onSuccess() {
                	Log.d(WifiDirectActivity.TAG,"removing group");
                	if (!groupRunningWithMeAsGroupOwner) connectToFriend(friendDevice);
                	
                }

                @Override
                public void onFailure(int reason) {
                    Log.d(WifiDirectActivity.TAG,"Couldn't remove group - " + reason);
                    // Reason 2 - There was no group to be removed in the first place!
                    if (reason==2) {
                    	groupRunningWithMeAsGroupOwner = false;
                    	connectToFriend(friendDevice);
                    }
                }
            });
        }
    };
    /**
     * Connect to a friend P2p Device
     * We become the client automatically when we call this method
     * @param device
     */
    private void connectToFriend(WifiP2pDevice device) {
    	if (device == null) return;
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.groupOwnerIntent = 0;  // Zero intention to become a group owner
        config.wps.setup = WpsInfo.PBC;
        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
            	Log.d(WifiDirectActivity.TAG,"starting connection");
            	friendDevice = null;
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Log.d(WifiDirectActivity.TAG,"Connection Failed");
            }
        });
    }
    /**
     * Reach here automatically when a connection is resolved
     * Makes group owner the server and the other device the client
     */
    private ConnectionInfoListener connectionInfoListener = new ConnectionInfoListener() {
    	
		@Override
		public void onConnectionInfoAvailable(WifiP2pInfo info) {
			Log.d(WifiDirectActivity.TAG,"connection Info Available");
			InetAddress groupOwnerAddress = null;
			if (!info.groupFormed) {
				Log.d(WifiDirectActivity.TAG,"group ain't formed");
				return;
			}
			try {
				groupOwnerAddress = InetAddress.getByName(info.groupOwnerAddress.getHostAddress());
			} catch (UnknownHostException e) {
				Log.e(WifiDirectActivity.TAG,"Impossible to arrive here",e);
			}
			if (info.groupFormed && info.isGroupOwner) {
				// We are the group owner of this connection
				groupRunningWithMeAsGroupOwner = true;
    			myIP = info.groupOwnerAddress.getHostAddress();
    			Log.d(WifiDirectActivity.TAG, "myIP "+ myIP);
    			try {
    				groupOwnerAddress = InetAddress.getByName(myIP);
    			} catch (UnknownHostException e) {
    				Log.e(WifiDirectActivity.TAG,"Impossible to arrive here",e);
    			}
				manager.requestGroupInfo(channel, new GroupInfoListener() {
    				@Override
    				public void onGroupInfoAvailable(WifiP2pGroup group) {
    	    			if (group==null) return;
    					passkey = group.getPassphrase();
    	    			mySSID = group.getNetworkName();
    	    			showQR();
    	    			Log.d(WifiDirectActivity.TAG, "passkey --- "+ passkey);
    	    			Log.d(WifiDirectActivity.TAG, "networkname --- "+ mySSID);
    					
    				}
    			});
				if (clientThread!=null) clientThread.cancel();
				clientThread = null;
				if (serverThread!=null) serverThread.cancel();
					serverThread = new ServerThread(groupOwnerAddress);
					serverThread.start();
				Log.d(WifiDirectActivity.TAG,"server started with me as group owner");
			}
			else if (info.groupFormed) {
				// We are not the group owner but we know the group owner ip
				if (serverThread!=null) serverThread.cancel();
				serverThread = null;
				if (clientThread!=null) clientThread.cancel();
				clientThread = new ClientThread(groupOwnerAddress);
				clientThread.start();
				Log.d(WifiDirectActivity.TAG,"client started and not group owner");
			}
		}
    	
    };
    
    /**
     * Show QR of our MAC_ID + SSID + WPA passkey + (ip + port of server)
     */
	private void showQR() {
		int dimension = 250;
		if ((DarknetAppConnector.activity.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_SMALL) {
			    // on a small screen device ...
				dimension = 120;
		}
		String signal = "-->>";
		if (!passkey.equals("") && !MAC_ID.equals("") && !mySSID.equals("") && !myIP.equals("")) 
			signal = passkey +signal +MAC_ID+signal+mySSID + signal + myIP;
		else return;
		int[] pixels = QRDisplayActivity.generateQR(signal,dimension);
		Log.d(WifiDirectActivity.TAG, myIP);
		Bitmap bitmap = Bitmap.createBitmap(dimension,dimension, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, dimension, 0, 0, dimension, dimension);
        ImageView imageview = (ImageView)uiActivity.findViewById(R.id.wifi_direct_qr_img);
        imageview.setImageBitmap(bitmap);
        TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
        if (DarknetAppConnector.isNfcEnabled==true)
			text.setText("Ready - Bring devices back to back if peer has Nfc Enabled");
		else text.setText("Scan QR (faster) or use conventional discovery process");
	}
	
	/**
	 * Array adapter that holds all discovered devices
	 */
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
        	WifiP2pDevice device = null;
        	try {
        		device = availablePeers.getItem(position);
        	}
        	catch(IndexOutOfBoundsException e) {
        		Log.e(WifiDirectActivity.TAG,"Peer is disappearing", e);
        	}
        	return device;
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
	/**
	 * Entry point
	 * Get the parent activity as well as add all the listeners
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        uiActivity = DarknetAppConnector.activity;
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        manager = (WifiP2pManager) uiActivity.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(uiActivity, uiActivity.getMainLooper(), null);
        View view = inflater.inflate(R.layout.exchange_wifi_direct_layout,null);
        handler = new ConnectionInfoHandler();
        return view;
	}
	
	/**
	 * Register receivers and show QR
	 */
	@Override
	public void onStart() {
		super.onStart();
		TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
		text.setText("Initializing");
        if(receiver==null) {
        	receiver = new MyBroadcastReceiver();
        	uiActivity.registerReceiver(receiver, intentFilter);
        	Log.d(TAG,"receiver registered");
        }
		showQR();
		setListeners();
	}
	// Called when peer list is updated
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
			
			// Got the state whether p2p is enabled or disabled
			// If enabled create a group 
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
		        int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
		        if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
		            isWifiP2pEnabled = true;
		            createServer();
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
	            if (networkInfo.isConnectedOrConnecting()) {
	            	Log.d(WifiDirectActivity.TAG,"Requested Connection Info for a connection 3");
	                manager.requestConnectionInfo(channel, connectionInfoListener);
	            }
	            else {
	            	groupRunningWithMeAsGroupOwner = false;
	            	// Reach here if our group is destroyed
	            	if (friendDevice != null) connectToFriend(friendDevice);
	            	
	            }

	        } 
	        // Our device status changed. Redraw the QR with new MAC,SSID etc
	        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
	            WifiP2pDevice device = ((WifiP2pDevice) intent.getParcelableExtra(
	                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
	            String deviceName = device.deviceName;
	            MAC_ID = device.deviceAddress;
	            showQR();
	            String status = getDeviceStatus(device.status);
	            Log.d(TAG,deviceName);
	            Log.d(TAG,MAC_ID);
	            Log.d(TAG,status);
	          
	        } 
	        // Connected to a device through legacy wifi
	        else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
	        	Log.d(TAG,"network state changed");
	        	NetworkInfo info =  intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	        	Log.d(TAG,WifiManager.EXTRA_NETWORK_INFO);
	        	if (info.isConnected() && wifiHelper!=null && wifiHelper.isCorrectAP()) {
	        		if (clientThread!=null) clientThread.cancel();
	        		clientThread = null;
	        		InetAddress IP = wifiHelper.getIP();
	        		if (IP!=null) {
	        			clientThread = new ClientThread(IP);
	        			clientThread.start();
	        		}
	        	}
	        }
		}
	}
	public class ConnectionInfoHandler extends Handler {
		@Override
        public void handleMessage(Message msg) {
			if (msg.arg1 == WifiDirectActivity.MESSAGE_NETWORK_CONNECTED) {
				Log.d(WifiDirectActivity.TAG,"got message");
				if (wifiHelper!=null && wifiHelper.isCorrectAP()) {
					TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
					text.setText("Connected - Transferring noderefs ");
					Log.d(TAG,"check 1");
	        		if (clientThread!=null) clientThread.cancel();
	        		clientThread = null;
	        		Log.d(TAG,"check 2");
	        		InetAddress IP = wifiHelper.getIP();
	        		Log.d(TAG,"check 3");
	        		if (IP!=null) {
	        			clientThread = new ClientThread(IP);
	        			clientThread.start();
	        		}
	        	}
			}
			else if (msg.arg1 == DarknetAppConnector.MESSAGE_SUCCESSFUL_EXCHANGE){
				TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
				boolean res = (Boolean) msg.obj;
				if (res) {
					text.setText("Exchange Successful and Connection Closed");
					closeActivity();
				}
				else { 
					text.setText("Exchange Cancelled");
					restartActivity();
				}
			}
			else if (msg.arg1 == WifiDirectActivity.MESSAGE_NDEF_UI) {
				TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
				text.setText("Connecting - Please Wait ");
			}
			/**
			 * Received the other mobile's details by either QR or as NDEF message
			 * Destroy our group and connect
			 */
			else if (msg.arg1 == WifiDirectActivity.WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE) {
				TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
				text.setText("Connecting - Please Wait ");
				if (manager!=null && channel!=null) {
					final String signal = (String)msg.obj;
					manager.removeGroup(channel, new ActionListener() {

						@Override
						public void onFailure(int arg0) {
							if (arg0==2)  {
								Message msg2 = new Message();
								msg2.obj = signal;
								wifiHelper = new LegacyWifiHelper(uiActivity);
								wifiHelper.start();
								groupStartingWithMeAsClient = true;
								msg2.arg1 = WifiDirectActivity.WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE+1;
								// crappy code
								// Assuming our server would be removed in 500ms
								handler.sendMessageDelayed(msg2, 500);
								
							}
							else {
								TextView text = (TextView) uiActivity.findViewById(R.id.wifi_direct_title_new_devices);
								text.setText("Connection Failed - Couldn't remove group");
							}
						}

						@Override
						public void onSuccess() {
							Message msg2 = new Message();
							msg2.obj = signal;
							wifiHelper = new LegacyWifiHelper(uiActivity);
							wifiHelper.start();
							groupStartingWithMeAsClient = true;
							msg2.arg1 = WifiDirectActivity.WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE+1;
							// crappy code
							//  Assuming our server would be removed  in 500ms
							handler.sendMessageDelayed(msg2, 500);
						}
						
					});
				}
			}
			else if (msg.arg1 == WifiDirectActivity.WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE+1) {
				String signal = (String) msg.obj;
				Log.d(TAG,"scan result -- "+ signal);
				String[] splitSignal = signal.split("-->>");
				if (splitSignal.length != 4) return;
				Log.d(WifiDirectActivity.TAG, splitSignal[3]);
				if (wifiHelper!=null) wifiHelper.add(splitSignal[2],splitSignal[0],splitSignal[3],splitSignal[1]);
				else wifiHelper = new LegacyWifiHelper(uiActivity,splitSignal[2],splitSignal[0],splitSignal[3],splitSignal[1]);
				if (wifiHelper!=null) synchronized(wifiHelper) {
					wifiHelper.notify();
				}
			}
		}

		private void restartActivity() {
			closeActivity();
			createServer();
		}
	}
	private void createServer() {
		manager.createGroup(channel, new ActionListener() {
			@Override
            public void onSuccess() {
                Log.d(WifiDirectActivity.TAG,"1");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(WifiDirectActivity.TAG,"Group can't be created - " +reasonCode);
                
            }
		}); 
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
		if (manager == null) {
			//show error
			Log.d(TAG,"manager not initiated");
		}
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
		serverThread = null;
		if (clientThread!=null) clientThread.cancel();
		clientThread = null;
		if (manager!=null) {
			manager.cancelConnect(channel, null);
			manager.removeGroup(channel, null);
		}
		if (wifiHelper!=null) wifiHelper.finish();
		wifiHelper = null;
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
        showQR();
        setListeners();
    }

    private void setListeners() {
    	Button discover = (Button) uiActivity.findViewById(R.id.wifi_direct_discover_button);
		Button scan = (Button) uiActivity.findViewById(R.id.wifi_direct_qr_scan_button);
		discover.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (manager!=null && channel!=null) {
					manager.removeGroup(channel, new ActionListener() {

						@Override
						public void onFailure(int arg0) {
							if (arg0==2)  {
								startDiscovery();
							}
							else Log.d(TAG,"Couldn't remove group");
							
						}

						@Override
						public void onSuccess() {
							startDiscovery();
						}
						
					});
				}
			}
			
		});
		scan.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (manager!=null && channel!=null) {
					manager.removeGroup(channel, new ActionListener() {

						@Override
						public void onFailure(int arg0) {
							if (arg0==2)  {
								wifiHelper = new LegacyWifiHelper(uiActivity);
								wifiHelper.start();
								Intent intent = new Intent("freenet.darknetconnector.QRCode.SCAN");
								intent.putExtra("freenet.darknetconnector.QRCode.SCAN.SCAN_MODE", "QR_CODE_MODE");
								uiActivity.startActivityForResult(intent, WifiDirectActivity.WIFI_DIRECT_QR_REQUEST_CODE);
							}
							else Log.d(TAG,"Couldn't remove group");
							
						}

						@Override
						public void onSuccess() {
							wifiHelper = new LegacyWifiHelper(uiActivity);
							wifiHelper.start();
							Intent intent = new Intent("freenet.darknetconnector.QRCode.SCAN");
							intent.putExtra("freenet.darknetconnector.QRCode.SCAN.SCAN_MODE", "QR_CODE_MODE");
							uiActivity.startActivityForResult(intent, WifiDirectActivity.WIFI_DIRECT_QR_REQUEST_CODE);
						}
						
					});
				}
			}
			
		});
		
	}
	@Override
	public void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
        if (requestCode == WifiDirectActivity.WIFI_DIRECT_QR_REQUEST_CODE) {
			if (resultCode == FragmentActivity.RESULT_OK) {
				String signal = data.getStringExtra("SCAN_RESULT");
				Log.d(TAG,"scan result -- "+ signal);
				String[] splitSignal = signal.split("-->>");
				if (splitSignal.length != 4) return;
				Log.d(WifiDirectActivity.TAG, splitSignal[3]);
				if (wifiHelper!=null) wifiHelper.add(splitSignal[2],splitSignal[0],splitSignal[3],splitSignal[1]);
				else wifiHelper = new LegacyWifiHelper(uiActivity,splitSignal[2],splitSignal[0],splitSignal[3],splitSignal[1]);
				if (wifiHelper!=null) synchronized(wifiHelper) {
					wifiHelper.notify();
				}
			}
			else if (resultCode == FragmentActivity.RESULT_CANCELED) {
		         // Handle cancel
		    }
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
	// Nfc Helper asks for a message. Put our MAC, SSID etc
	public NdefMessage createNdefMessage(NfcEvent event) {
		NdefMessage msg = null;
		String signal = "-->>";
		if (!passkey.equals("") && !MAC_ID.equals("") && !mySSID.equals("") && !myIP.equals("")) 
			signal = passkey +signal +MAC_ID+signal+mySSID + signal + myIP;
		else return msg;
		NdefRecord ndefRec = null;
		try {
			ndefRec = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,"Freenet".getBytes(),signal.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		msg = new NdefMessage(new NdefRecord[]{ ndefRec });
	    return msg;
	}

	public void onNdefPushComplete(NfcEvent event) {
		Message msg = new Message();
		msg.arg1 = WifiDirectActivity.MESSAGE_NDEF_UI;
		handler.sendMessage(msg);
	}
}
