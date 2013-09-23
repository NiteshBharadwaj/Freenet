package freenet.darknetconnector.DarknetAppConnector;

/**
 * The main activity that starts when application is started
 * The application presently can pull/push nodereferences from a freenet node if the updated plugin MDNSDiscovery is enabled and is on the same network as the mobile
 * The application allows exchange of nodereferences with peers by blueooth, Wi-Fi Direct and QR  
 * Once it is configured with a freenet node, that particular node becomes its homeNode.
 * @author Illutionist
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import freenet.darknetconnector.DarknetAppConnector.R;
import freenet.darknetconnector.NodeReferenceExchange.BluetoothActivity;
import freenet.darknetconnector.NodeReferenceExchange.NfcHandler;
import freenet.darknetconnector.NodeReferenceExchange.QRActivity;
import freenet.darknetconnector.NodeReferenceExchange.WifiDirectActivity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class DarknetAppConnector extends FragmentActivity {

	// All static variables.. Should remain same for the whole application
	public static final String directoryName = Environment.getExternalStorageDirectory() + File.separator + "Android"+ File.separator + "data" + File.separator +"org.freenet.darknetappconnector";
	public final static String propertiesFileName =  directoryName+File.separator+"config.properties";
	public final static String nodeRefFileName = directoryName+File.separator+"myref.txt"; 
	public final static String peerNodeRefsFileName =  directoryName+File.separator+"friendrefs.txt";
	public static boolean configured = false;
	private MDNSClient mdnsClient; 
	private static Properties prop = new Properties(); 
	private static Context context;
	public static FragmentActivity activity;
	public static FragmentManager fragmentManager;
	public static int newDarknetPeersCount = 0;
	public static int newDarknetPeersCountPrev = 0;
	public static long lastSynched = 0;
	public static UIHandler handler;
	public final static int MESSAGE_FRAGMENT_WIFI_DIRECT = 4;
	public final static int MESSAGE_START_FRAGMENT = 2;
	public final static int MESSAGE_FINISH_FRAGMENT = 3;
	public static final int MESSSAGE_UNINITIALIZED = 0;
	public static final int MESSAGE_CONFIGURED_FIRST_TIME = 1;
	public static final int MESSAGE_AUTHORIZATION_RESULT = 9;
	protected static final int MESSAGE_FRAGMENT_BLUETOOTH = 5;
	public static final int MESSAGE_PEERS_UPDATED = 10;
	public static final int MESSAGE_TIMER_TASK = 11;
	public static final int MESSAGE_FRAGMENT_QR = 6;
	public static final int MESSAGE_SUCCESSFUL_EXCHANGE = 999;
	public static final int MESSAGE_NETWORK_CONNECTED = 20;
	private static final String TAG = "DarknetAppConnector";
	private NfcHandler nfcHandler;
	public static int currentApiVersion = android.os.Build.VERSION.SDK_INT;
	public static boolean isNfcEnabled = false;
	
	/**
	 * Entry point to the application
	 * Create an MDNSClient that listens in the background for broadcasts of - the presence darknet app server on a freenet node
	 * Check if the node is configured
	 * If already configured, switch to the options fragment (displays options for noderef exchange)
	 * Any switching between fragments is made to go through this class
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		context = getBaseContext();
		activity = this;
		fragmentManager = getSupportFragmentManager();
		initialize();
		mdnsClient = new MDNSClient(context);
		handler = new UIHandler();
		mdnsClient.startListening();
		if (configured) {
			setListeners();
		}
	}
	
	protected void onStart() {
		super.onStart();
		// Don't start nfc handler on old mobiles
		if(currentApiVersion>14) {
			nfcHandler = new NfcHandler(this);
			if (nfcHandler!=null) isNfcEnabled = true;
		}
	}
	
	protected void onPause() {
		super.onPause();
		Log.d(DarknetAppConnector.TAG,"paused");
		if (nfcHandler!=null) nfcHandler.onPause();
		//mdnsClient.stopListening();
	}
	protected void onResume() {
		super.onResume();
		if (nfcHandler!=null) nfcHandler.onResume();
		Log.d(DarknetAppConnector.TAG,"resumed");
		//mdnsClient.startListening();
	}
	protected void onStop() {
		super.onStop();
		Log.d(DarknetAppConnector.TAG,"stopped");
		//mdnsClient.stopListening();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_menu, menu);
		return true;
	}
	/**
	 * Create the directory sructure (if not exists already)
	 * Pull properties like homeNode name, temporary peers etc. (if present)
	 */
	private void initialize() {
		File directory = new File(directoryName);
		if (!directory.exists()) directory.mkdirs();
		File pfile = new File(propertiesFileName);	
		File nfile = new File(nodeRefFileName);
		try {
			if (!pfile.exists()) pfile.createNewFile();
			if (!nfile.exists()) nfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			String name = prop.getProperty("homeNodeName");
			String pin = prop.getProperty("homeNodePin");
			if (name!=null && name.length()!=0) {
				HomeNode.setName(name);
				TextView text = (TextView) findViewById(R.id.main_screen_text);
				text.setText(this.getString(R.string.main_screen_configured)+" " +name);
				configured = true;
			}
			if (pin!=null && pin.length()!=0) {
				HomeNode.setPin(pin);
			}
			String count = prop.getProperty("newDarknetPeersCount");
			if (count!=null && !count.equals(""))  {
				newDarknetPeersCount = Integer.parseInt(count);
				configured = true;
			}
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	/**
	 * This method is called when our home node is configured for the first time or when the home node is changed by the user
	 * @param name homeNode name 
	 */
	public static void updatePropertiesFile(String name, String pin) {
		File pfile = new File(propertiesFileName);	
		try {
			if (!pfile.exists()) pfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			if (name!=null && name.length()!=0) {
				prop.setProperty("homeNodeName", name);			
				prop.setProperty("newDarknetPeersCount", String.valueOf(newDarknetPeersCount));
				DarknetAppConnector.configured = true;
				if (pin!=null && pin.length()!=0) {
					prop.setProperty("homeNodePin", pin);	
				}
				prop.store(new FileOutputStream(pfile), null);
				TextView text = (TextView) activity.findViewById(R.id.main_screen_text);
				text.setText(activity.getString(R.string.main_screen_configured)+" " +name);
				//  Configured - Switch to the options fragment 
				setListeners();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * Updates the properties file with new darknet peers count
	 * This method needs to be called whenever a successful nodereference exchange takes place
	 */
	public static void updatedPeersCount() {
		File pfile = new File(propertiesFileName);	
			try {
				prop.load(new FileInputStream(pfile));
				prop.setProperty("newDarknetPeersCount", String.valueOf(newDarknetPeersCount));
				prop.store(new FileOutputStream(pfile), null);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * Some operations that are to be performed only from main thread
	 * Slave threads send commands
	 */
	public class UIHandler extends Handler {

		@Override
        public void handleMessage(Message msg) {
            int command = (int)msg.arg1;
            switch(command) {
            	case DarknetAppConnector.MESSSAGE_UNINITIALIZED: {
            		// Remove all other fragments since nothing is initialized as yet
            		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            		Log.d(DarknetAppConnector.TAG,"Not started yet");
            		break;
            	}
            	case DarknetAppConnector.MESSAGE_CONFIGURED_FIRST_TIME: {
            		String signal = (String)msg.obj;
            		String[] split = signal.split("-->>");
            		if (split.length != 2) return; //TODO: Throw exception
            		String name = split[0];
            		String pin = split[1];
		 			DarknetAppConnector.updatePropertiesFile(name,pin);
		 			HomeNode.setName(name);
		 			HomeNode.setPin(pin);
		 			DarknetAppConnector.configured = true;
		 			break;
            	}
            	// Options fragment has got a command to start wifi-direct exchange
            	// Switch to wifi direct fragment
            	case DarknetAppConnector.MESSAGE_FRAGMENT_WIFI_DIRECT: {
            		int command2 = (int) msg.arg2;
            		switch(command2) {
            			case DarknetAppConnector.MESSAGE_START_FRAGMENT: {	
            				Log.d(DarknetAppConnector.TAG,"Starting Wifi Direct fragment");
            				FragmentTransaction transaction = fragmentManager.beginTransaction();
            				Fragment fragment = new WifiDirectActivity(); 
            			    transaction.replace(R.id.fragment_view,fragment,WifiDirectActivity.TAG);
            			    transaction.addToBackStack("Transaction from optionsfragment to wifidirect fragment");
            			    transaction.commit();
            			}
            		}
            		break;
            	}
            	// Switch to bluetooth fragment
            	case DarknetAppConnector.MESSAGE_FRAGMENT_BLUETOOTH: {
            		int command2 = (int) msg.arg2;
            		switch(command2) {
            			case DarknetAppConnector.MESSAGE_START_FRAGMENT: {	
            				Log.d(DarknetAppConnector.TAG,"Starting Bluetooth fragment");
            				FragmentTransaction transaction = fragmentManager.beginTransaction();
            				Fragment fragment = new BluetoothActivity(); 
            			    transaction.replace(R.id.fragment_view,fragment,BluetoothActivity.TAG);
            			    transaction.addToBackStack("Transaction from optionsfragment to bluetoothfragment");
            			    transaction.commit();
            			    break;
            			}
            		}
            		break;
            	}
            	// Switch to QR fragment
            	case DarknetAppConnector.MESSAGE_FRAGMENT_QR: {
            		int command2 = (int) msg.arg2;
            		switch(command2) {
        				case DarknetAppConnector.MESSAGE_START_FRAGMENT: {
        					Log.d(DarknetAppConnector.TAG,"Starting QR fragment");
            				FragmentTransaction transaction = fragmentManager.beginTransaction();
            				Fragment fragment = new QRActivity(); 
            			    transaction.replace(R.id.fragment_view,fragment,QRActivity.TAG);
            			    transaction.addToBackStack("Transaction from optionsfragment to qrfragment");
            			    transaction.commit();
        					break;
        				}
            		}
            		break;
            	}
            	// Start an activity to display the node reference completely and allowing user to accept/ reject it
            	// The result(accept/reject) is received in the onActivtyResult function
            	// TODO: Display only important parts of noderef like nodeName etc
            	case DarknetAppConnector.MESSAGE_PEERS_UPDATED: {
            		String friendRef = (String) msg.obj;
            		Intent i = new Intent(DarknetAppConnector.this,PeerReferenceActivity.class);
            		i.putExtra("text", friendRef);
        			i.putExtra("code", 100);
        			startActivityForResult(i,100);
        			break;
            		
            	}
            	// Reach here when user accepts/ rejects a change in homenode
            	case DarknetAppConnector.MESSAGE_AUTHORIZATION_RESULT: {
            		boolean result = (Boolean) msg.obj;
            		MDNSReceiver.handleResult(result);
            		break;
            	}
            	// We got new reference but have not synchronized them yet 
            	// Persistently, try to connect to our home node
            	case DarknetAppConnector.MESSAGE_TIMER_TASK: {
            		if (DarknetAppConnector.newDarknetPeersCount>0) {
            			if (HomeNode.getIP() != null && HomeNode.getPin()!= null && HomeNode.getName()!= null && HomeNode.getPort() != 0) {
            				Runnable r = new ConnectionWithServer(HomeNode.getIP(),HomeNode.getPort(),HomeNode.getPin(),HomeNode.getName(),true);
    	       		 		new Thread(r).start();
            			}
            			else {
            				Message msg2 = new Message();
                   		 	msg2.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
                   		 	DarknetAppConnector.handler.sendMessageDelayed(msg2, 60*1000);
            			}
            		}
            	}
            	case DarknetAppConnector.MESSAGE_NETWORK_CONNECTED: {
            		if (!mdnsClient.isListening()) {
            			mdnsClient.startListening();
            		}
            	}
            }
	    }
	}
	
	/**
	 * Reach here when an nfc message is received
	 */
	@Override
	public void onNewIntent(Intent intent) {
		if (nfcHandler!=null) nfcHandler.onNewIntent(intent);
	}
	
	
	@Override
    protected void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
		Fragment fragment; 
		// Result of QR Scan
	    if (requestCode == 210) {
	        if (resultCode == FragmentActivity.RESULT_OK) {
	        	String friendRef = data.getStringExtra("SCAN_RESULT");
	        	String format = data.getStringExtra("SCAN_RESULT_FORMAT");
	        	if (!format.equals("QR_CODE"))  {
	        		// TODO: Show error message
	        		return;
	        	}
	        	Message msg = new Message();
	        	msg.arg1 = DarknetAppConnector.MESSAGE_PEERS_UPDATED;
	        	msg.obj = friendRef;
	        	Log.d(DarknetAppConnector.TAG,friendRef);
	        	// Handle successful scan
	        	DarknetAppConnector.handler.sendMessage(msg);
	      } 
	      
	      // Some kind of cancel has occurred
	      // Find out the source and destroy corresponding activity
	      else if (resultCode == FragmentActivity.RESULT_CANCELED) {
	    	  fragment = (Fragment) fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
	    	  if (fragment!=null) BluetoothActivity.closeActivity();
	    	  fragment = (Fragment) fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
	    	  if (fragment!=null) WifiDirectActivity.closeActivity();
	         // Handle cancel
	      }
		}
		// New peer reference has either been accepted or rejected
	    // If accepted store it
	    // Send the success/failure to the corresponding fragment
		else if(resultCode == 100){
			Message msg = null;
        	Boolean result = data.getBooleanExtra("check", false);
        	if (result) {
        		 String reference = data.getStringExtra("reference");
        		 File nfile = new File(peerNodeRefsFileName);	
        		 Properties prop = new Properties();
        		 if (!nfile.exists())
					try {
						nfile.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					try {
		     			prop.load(new FileInputStream(nfile));
		     			prop.setProperty("newPeers" + ++newDarknetPeersCount, reference);
		     			Log.d(DarknetAppConnector.TAG,"Latest Count" +newDarknetPeersCount);
		     			prop.store(new FileOutputStream(nfile), null);
					} catch (IOException e) {
						e.printStackTrace();
					}

        		 updatedPeersCount();
        		 msg = new Message();
        		 msg.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
        		 DarknetAppConnector.handler.sendMessage(msg);
        		 msg = new Message();
        		 msg.arg1 = DarknetAppConnector.MESSAGE_SUCCESSFUL_EXCHANGE;
        		 msg.obj = true;
        	}
        	else {
        		msg = new Message();
        		msg.arg1 = DarknetAppConnector.MESSAGE_SUCCESSFUL_EXCHANGE;
        		msg.obj = false;
        	}
        	fragment = (Fragment) fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
	    	if (fragment!=null) {
       		 	BluetoothActivity.handler.sendMessage(msg);
	    	}
	    	fragment = (Fragment) fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
	    	if (fragment!=null) {
	    		WifiDirectActivity.handler.sendMessage(msg);
	    	}
        }
	    // If startActivityForResult() has been started by other fragments, redirect there
		else {
			fragment = (Fragment) fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
		    if(fragment != null){
		    	fragment.onActivityResult(requestCode, resultCode, data);
		    }
		    else if ((fragment = (Fragment) fragmentManager.findFragmentByTag(WifiDirectActivity.TAG))!=null) {
		    	fragment.onActivityResult(requestCode, resultCode, data);
		    }
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	// Call this from any method to set the default screen with default listeners 
	// Caution: Call only after the mobile is configured with a home node
	public static void setListeners() {
		if (!configured) return;
		OptionsFragment options = new OptionsFragment();
		if (fragmentManager.getBackStackEntryCount()>0) {
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.add(R.id.fragment_view, options,OptionsFragment.TAG);
		transaction.addToBackStack("EmptyFragment to OptionsFragment");
		transaction.commit();
	}
	
	@Override
	public void onBackPressed() {
		if (fragmentManager.findFragmentByTag(WifiDirectActivity.TAG)!=null) {
			boolean activityResult = WifiDirectActivity.closeActivity();
			if (activityResult) {
				Log.d(DarknetAppConnector.TAG,"Reference Exchange Successful");
			} 
			else {
				Log.d(DarknetAppConnector.TAG,"Reference Exchange Unsuccessful");
			}
			super.onBackPressed();
		}
		else if (fragmentManager.findFragmentByTag(BluetoothActivity.TAG)!=null) {
			boolean activityResult = BluetoothActivity.closeActivity();
			if (activityResult) {
				Log.d(DarknetAppConnector.TAG,"Reference Exchange Successful");
			} 
			else {
				Log.d(DarknetAppConnector.TAG,"Reference Exchange Unsuccessful");
			}
			super.onBackPressed();
		}
		else if (fragmentManager.findFragmentByTag(OptionsFragment.TAG)!=null) {
			fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			super.onBackPressed();
		}
		else {
			super.onBackPressed();
		}
	}
}
