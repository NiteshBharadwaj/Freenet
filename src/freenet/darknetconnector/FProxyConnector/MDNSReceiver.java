package freenet.darknetconnector.FProxyConnector;

/**
 * A MDNS receiver class that receives MDNS broadcasts from the Asynchronous background task MDNSClient
 * It raises intent asking if home node is to be changed or if to be associated with a home node for the first time
 * TODO: Make the node reference pull asynchronous and remove the security override
 * @author Illutionist
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
    		// Security override - If android SDK version is less than 9 (GingerBread 2.3), it works without overriding
    		if (android.os.Build.VERSION.SDK_INT > 9) {
    			StrictMode.ThreadPolicy policy = 
    			        new StrictMode.ThreadPolicy.Builder().permitAll().build();
    			StrictMode.setThreadPolicy(policy);
    			}
    		Bundle extras = getIntent().getExtras();
    		if(extras !=null) {
    			this.name = extras.getString("name");
        		this.ip = extras.getString("ip");
        		this.port = extras.getInt("port", 8888);
        		this.pin = extras.getString("pin");
        		Log.d("dumb",pin);
    		}
    		
    		if (FProxyConnector.configured && HomeNode.getName().equals(name)) {
    			//We reach here if newly discovered MFNS is same as our homenode
				//pullnoderef(ip,port);
				finish();
			}
			else {
				boolean change = false;
				if (FProxyConnector.configured)
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
	        		 		File file = new File(FProxyConnector.nodeRefFileName);
	        		 		if (!file.exists()) {
	        		 			try {
									file.createNewFile();
								} catch (IOException e) {
									e.printStackTrace();
								}
	        		 		}
	        		 		Log.d("dumb",ip);
	        		 		Log.d("port",""+port);
	        		 		boolean done = pullnoderef(ip, port);
	        		 		if (done) {
	        		 			FProxyConnector.updatePropertiesFile(name);
	        		 			HomeNode.setName(name);
	        		 			FProxyConnector.configured = true;
	        		 		}
	        		 	}
	        	 else {
	        		 MainFragment.setState(MainFragment.FRAGMENT_STATE_UNINITIALIZED);
	        	 }
	             finish();
	        	 }
	            
	        }

		public void raiseIntent(boolean change) {
			Activity activity = FProxyConnector.activity;
			String text  = this.getString(R.string.main_screen_connect1)+" " + name+ " " + activity.getString(R.string.main_screen_connect2);
			if (change) text = this.getString(R.string.main_screen_change)+" " + name;
			Intent i = new Intent(this,AuthorizationActivity.class);
			i.putExtra("text", text);
			i.putExtra("code", 100);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			this.startActivityForResult(i,100);
		}
		public static boolean pullnoderef(String ip,int port) {
			boolean done = false;
	    	/*String uri = "http://" +ip + ":" +port +"/addfriend/myref.txt";
	    	File fl = new File(FProxyConnector.nodeRefFileName);
		    try {

		    	URL dl = new URL(uri);
		    	FileUtils.copyURLToFile(dl, fl);
		    	done = true;
		    }
		    catch(IOException e) {
		    	e.printStackTrace();
		    }*/
			File fl = new File(FProxyConnector.nodeRefFileName);
			DarknetAppClient context = new DarknetAppClient(ip,port,true,FProxyConnector.activity.getApplicationContext(),pin);
			boolean connected = context.startConnection();
			String ref;
			try {
				if (connected) {
					ref = context.pullHomeReference();
					FileUtils.writeStringToFile(fl, ref);
					context.closeConnection();
					done = true;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    return done;
		}

}
