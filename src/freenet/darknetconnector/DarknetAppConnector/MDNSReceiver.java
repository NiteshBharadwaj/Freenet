package freenet.darknetconnector.DarknetAppConnector;

/**
 * A MDNS receiver class that receives MDNS broadcasts from the Asynchronous background task MDNSClient
 * It raises intent asking if home node is to be changed or if to be associated with a home node for the first time as soon as it discovers a broadcast
 * If already associated with a homeNode, it automatically synchronizes the nodereferences 
 * @author Illutionist
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import freenet.darknetconnector.FProxyConnector.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;

public class MDNSReceiver extends Activity{
	
    	private String name;
    	private String ip;
    	private int port;
    	private static String pin;
    	
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		// This activity doesn't display anything on its own. It sends the text to be displayed along with options to authorizationactivity
    		Bundle extras = getIntent().getExtras();
    		if(extras !=null) {
    			this.name = extras.getString("name");
        		this.ip = extras.getString("ip");
        		this.port = extras.getInt("port", 8888);
        		this.pin = extras.getString("pin");
        		Log.d("dumb",pin);
    		}
    		
    		if (DarknetAppConnector.configured && HomeNode.getName().equals(name)) {
    			//We reach here if newly discovered MDNS is same as our homenode
    			// TODO: Add pin to homenode and also check if the pins match
    			if ((DarknetAppConnector.newDarknetPeersCount!=DarknetAppConnector.newDarknetPeersCountPrev && DarknetAppConnector.newDarknetPeersCount>0) || System.currentTimeMillis()-DarknetAppConnector.lastSynched > 60*60*60*1000) {
    				Runnable r = new ConnectionThread(ip,port,pin,name,false);
    				new Thread(r).start();
    			}
				finish();
			}
			else {
				boolean change = false;
				if (DarknetAppConnector.configured)
					change = true;
				raiseIntent(change);
			}
    	}
    	@Override
	    protected void onActivityResult(int requestCode,
	                                     int resultCode, Intent data) {
	        super.onActivityResult(requestCode, resultCode, data);
	        if(resultCode == 100){
	        	
	        	Boolean result = data.getBooleanExtra("check", false);
	        	 if (result) {
	        		 		File file = new File(DarknetAppConnector.nodeRefFileName);
	        		 		if (!file.exists()) {
	        		 			try {
									file.createNewFile();
								} catch (IOException e) {
									e.printStackTrace();
								}
	        		 		}
	        		 		Log.d("dumb",ip);
	        		 		Log.d("port",""+port);
	        		 		Runnable r = new ConnectionThread(ip,port,pin,name,true);
	        		 		new Thread(r).start();
	        		 	}
	        	 else {
	        		 MainFragment.setState(MainFragment.FRAGMENT_STATE_UNINITIALIZED);
	        	 }
	             finish();
	        	 }
	            
	        }

		public void raiseIntent(boolean change) {
			Activity activity = DarknetAppConnector.activity;
			String text  = this.getString(R.string.main_screen_connect1)+" " + name+ " " + activity.getString(R.string.main_screen_connect2);
			if (change) text = this.getString(R.string.main_screen_change)+" " + name;
			Intent i = new Intent(this,AuthorizationActivity.class);
			i.putExtra("text", text);
			i.putExtra("code", 100);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivityForResult(i,100);
		}
		
		public class ConnectionThread implements Runnable {
			
			String ip;
			int port;
			String pin;
			String name;
			boolean firstTime =false;
			public ConnectionThread(String ip, int port, String pin, String name, Boolean firstTime) {
				this.ip = ip;
				this.port = port;
				this.pin = pin;
				this.name = name;
				this.firstTime = firstTime;
			}

			@Override
			public void run() {
				boolean done = false;
				File fl = new File(DarknetAppConnector.nodeRefFileName);
				DarknetAppClient context = new DarknetAppClient(ip,port,true,DarknetAppConnector.activity.getApplicationContext(),pin);
				boolean connected = context.startConnection();
				String ref;
				try {
					if (connected) {
						ref = context.pullHomeReference();
						if (DarknetAppConnector.newDarknetPeersCount>0)
							context.pushReferences();
						FileUtils.writeStringToFile(fl, ref);
						context.closeConnection();
						done = true;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (done && firstTime) {
					Message msg = new Message();
					char uiCommand = MainFragment.MSG_CONFIGURED_FIRST_TIME;
					msg.arg1 = uiCommand;
					msg.obj = name;
					MainFragment.handler.sendMessage(msg);
		 		}
				DarknetAppConnector.lastSynched = System.currentTimeMillis();
			}
			
		}

}
