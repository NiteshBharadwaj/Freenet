package freenet.darknetconnector.DarknetAppConnector;

/**
 * The main activity that starts when application is started
 * The application presently can pull/push nodereferences from a freenet node if the updated plugin MDNSDiscovery is enabled 
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
import freenet.darknetconnector.NodeReferenceExchange.WifiDirectActivity;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DarknetAppConnector extends FragmentActivity {

	// All static variables.. Should remain same for the whole application
	public final static String propertiesFileName = Environment.getExternalStorageDirectory() + File.separator +"config.properties";
	public final static String nodeRefFileName = Environment.getExternalStorageDirectory() + File.separator +"myref.txt"; 
	public final static String peerNodeRefsFileName = Environment.getExternalStorageDirectory() + File.separator +"friendrefs.txt";
	public static boolean configured = false;
	private MDNSClient mdnsClient; 
	private static Properties prop = new Properties(); 
	private static Context context;
	public static FragmentActivity activity;
	public static FragmentManager fragmentManager;
	public static int newDarknetPeersCount = 0;
	public static int newDarknetPeersCountPrev = 0;
	public static long lastSynched = 0;
	public static Handler handler;
	
	public final static int MESSAGE_FRAGMENT_WIFI_DIRECT = 4;
	public final static int MESSAGE_START_FRAGMENT = 2;
	public final static int MESSAGE_FINISH_FRAGMENT = 3;
	public static final int MESSSAGE_UNINITIALIZED = 0;
	public static final int MESSAGE_CONFIGURED_FIRST_TIME = 1;
	public static final int MESSAGE_AUTHORIZATION_RESULT = 9;
	protected static final int MESSAGE_FRAGMENT_BLUETOOTH = 5;
	public static final int MESSAGE_PEERS_UPDATED = 10;
	public static final int MESSAGE_TIMER_TASK = 11;
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
	
	protected void onPause() {
		super.onPause();
		//mdnsClient.stopListening();
	}
	protected void onResume() {
		super.onResume();
		//setContentView(R.layout.activity_fproxy_connector);
		//mdnsClient.startListening();
	}
	protected void onStop() {
		super.onStop();
		//mdnsClient.stopListening();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_menu, menu);
		return true;
	}
	
	private void initialize() {
		File pfile = new File(propertiesFileName);	
		File nfile = new File(nodeRefFileName);
		try {
			if (!pfile.exists()) pfile.createNewFile();
			if (!nfile.exists()) nfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			String name = prop.getProperty("homeNodeName");
			if (name!=null && name.length()!=0) {
				HomeNode.setName(name);
				TextView text = (TextView) findViewById(R.id.main_screen_text);
				text.setText(this.getString(R.string.main_screen_configured)+" " +name);
				configured = true;
			}
			String count = prop.getProperty("newDarknetPeersCount");
			if (count!=null && !count.equals("")) newDarknetPeersCount = Integer.parseInt(count);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	public static void updatePropertiesFile(String name) {
		File pfile = new File(propertiesFileName);	
		try {
			if (!pfile.exists()) pfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			if (name!=null && name.length()!=0) {
				prop.setProperty("homeNodeName", name);			
				prop.setProperty("newDarknetPeersCount", String.valueOf(newDarknetPeersCount));
				DarknetAppConnector.configured = true;
				prop.store(new FileOutputStream(pfile), null);
				TextView text = (TextView) activity.findViewById(R.id.main_screen_text);
				text.setText(activity.getString(R.string.main_screen_configured)+" " +name);
				setListeners();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void updatedPeersCount() {
		File pfile = new File(propertiesFileName);	
			try {
				prop.load(new FileInputStream(pfile));
				prop.setProperty("newDarknetPeersCount", String.valueOf(newDarknetPeersCount));
				prop.store(new FileOutputStream(pfile), null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	// Some operations that are to be performed only from main thread
		// Slave threads send commands
		public class UIHandler extends Handler {
			public UIHandler() {
				
			}
			@Override
	        public void handleMessage(Message msg) {
	            int command = (int)msg.arg1;
	            switch(command) {
	            	case DarknetAppConnector.MESSSAGE_UNINITIALIZED: {
	            		// Remove all other fragments since nothing is initialized as yet
	            		fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
	            		Log.d("dumb","Not started yet");
	            		break;
	            	}
	            	case DarknetAppConnector.MESSAGE_CONFIGURED_FIRST_TIME: {
	            		String name = (String)msg.obj;
			 			DarknetAppConnector.updatePropertiesFile(name);
			 			HomeNode.setName(name);
			 			DarknetAppConnector.configured = true;
			 			break;
	            	}
	            	case DarknetAppConnector.MESSAGE_FRAGMENT_WIFI_DIRECT: {
	            		int command2 = (int) msg.arg2;
	            		switch(command2) {
	            			case DarknetAppConnector.MESSAGE_START_FRAGMENT: {	
	            				Log.d("dumb","Starting Wifi Direct fragment");
	            				FragmentTransaction transaction = fragmentManager.beginTransaction();
	            				Fragment fragment = new WifiDirectActivity(); 
	            			    transaction.replace(R.id.fragment_view,fragment,WifiDirectActivity.TAG);
	            			    transaction.addToBackStack("Transaction from optionsfragment to wifidirect fragment");
	            			    transaction.commit();
	            			}
	            		}
	            		break;
	            	}
	            	case DarknetAppConnector.MESSAGE_FRAGMENT_BLUETOOTH: {
	            		int command2 = (int) msg.arg2;
	            		switch(command2) {
	            			case DarknetAppConnector.MESSAGE_START_FRAGMENT: {	
	            				Log.d("dumb","Starting Bluetooth fragment");
	            				FragmentTransaction transaction = fragmentManager.beginTransaction();
	            				Fragment fragment = new BluetoothActivity(); 
	            			    transaction.replace(R.id.fragment_view,fragment,BluetoothActivity.TAG);
	            			    transaction.addToBackStack("Transaction from optionsfragment to bluetoothfragment");
	            			    transaction.commit();
	            			}
	            		}
	            		break;
	            	}
	            	case DarknetAppConnector.MESSAGE_PEERS_UPDATED: {
	            		String friendRef = (String) msg.obj;
	            		Intent i = new Intent(DarknetAppConnector.this,PeerReferenceActivity.class);
	            		i.putExtra("text", friendRef);
	        			i.putExtra("code", 100);
	        			startActivityForResult(i,100);
	        			break;
	            		
	            	}
	            	case DarknetAppConnector.MESSAGE_AUTHORIZATION_RESULT: {
	            		boolean result = (Boolean) msg.obj;
	            		MDNSReceiver.handleResult(result);
	            		break;
	            	}
	            	case DarknetAppConnector.MESSAGE_TIMER_TASK: {
	            		if (DarknetAppConnector.newDarknetPeersCount>0) {
	            			if (MDNSReceiver.ip != null && MDNSReceiver.pin != null && MDNSReceiver.name != null && MDNSReceiver.port != 0) {
	            				Runnable r = new ConnectionWithServer(MDNSReceiver.ip,MDNSReceiver.port,MDNSReceiver.pin,MDNSReceiver.name,true);
	    	       		 		new Thread(r).start();
	            			}
	            			else {
	            				Message msg2 = new Message();
	                   		 	msg2.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
	                   		 	DarknetAppConnector.handler.sendMessageDelayed(msg2, 60*1000);
	            			}
	            		}
	            	}
	            }
	            
	        }
		}
	
	@Override
    protected void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == 100){
        	
        	Boolean result = data.getBooleanExtra("check", false);
        	if (result) {
        		 String reference = data.getStringExtra("reference");
        		 File nfile = new File(peerNodeRefsFileName);	
        		 Properties prop = new Properties();
        		 if (!nfile.exists())
					try {
						nfile.createNewFile();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					try {
		     			prop.load(new FileInputStream(nfile));
		     			prop.setProperty("newPeers" + ++newDarknetPeersCount, reference);
		     			Log.d("dumb","Latest Count" +newDarknetPeersCount);
		     			prop.store(new FileOutputStream(nfile), null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

        		 updatedPeersCount();
        		 Message msg = new Message();
        		 msg.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
        		 DarknetAppConnector.handler.sendMessage(msg);
        	}
        }
	}
	// Call this from any method to set the default screen with default listeners 
	// Caution: Call only after the mobile is configured with a home node
	public static void setListeners() {
		if (!configured) return;
		int i = 0;
		OptionsFragment options = new OptionsFragment();
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
				Log.d("dumb","Reference Exchange Successful");
			} 
			else {
				Log.d("dumb","Reference Exchange Unsuccessful");
			}
		}
		else if (fragmentManager.findFragmentByTag(BluetoothActivity.TAG)!=null) {
			boolean activityResult = BluetoothActivity.closeActivity();
			if (activityResult) {
				Log.d("dumb","Reference Exchange Successful");
			} 
			else {
				Log.d("dumb","Reference Exchange Unsuccessful");
			}
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
