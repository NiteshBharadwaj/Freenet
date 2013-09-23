package freenet.darknetconnector.DarknetAppConnector;

/**
 * A MDNS receiver class that receives MDNS broadcasts from the Asynchronous background task MDNSClient
 * It raises intent asking if home node is to be changed or if to be associated with a home node for the first time as soon as it discovers a broadcast
 * If already associated with a homeNode, it automatically synchronizes the nodereferences 
 * @author Illutionist
 */

import java.io.File;
import java.io.IOException;

import freenet.darknetconnector.DarknetAppConnector.R;

import android.app.Activity;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class MDNSReceiver extends Thread {
	
    	private static final String TAG = "MDNSReceiver";
		private String name;
    	private String ip;
    	private int port;
    	private String pin;
    	public MDNSReceiver(String name, String ip, int port, String pin) {
    			this.name = name;
        		this.ip = ip;
        		this.port = port;
        		this.pin = pin;
        		Log.d(MDNSReceiver.TAG,pin);
    	}
    	public void run() {
    		if (DarknetAppConnector.configured && HomeNode.check(name)) {
    			//We reach here if newly discovered MDNS is same as our homenode
    			if (!HomeNode.check(name,pin)) {
    				// Pin provided last time doesn't match with presently provided pin
    				// This might be an attack or old certificate might have expired or old certificate might have been replaced by the user
    				// TODO: Ask user to confirm
    				// If user confirms
    				HomeNode.setPin(pin);
    			}
    			if (HomeNode.check(name, port, ip, pin)) Log.d(MDNSReceiver.TAG,"All is super fine");
    			HomeNode.setIP(ip);
    			HomeNode.setPort(port);
    			// If we have new references/ it's been long - connect to homeNode
    			if ((DarknetAppConnector.newDarknetPeersCount!=DarknetAppConnector.newDarknetPeersCountPrev && DarknetAppConnector.newDarknetPeersCount>0) || System.currentTimeMillis()-DarknetAppConnector.lastSynched > 60*60*60*1000) {
    				Runnable r = new ConnectionWithServer(ip,port,pin,name,false);
    				new Thread(r).start();
    			}
			}
			else {
				boolean change = false;
				if (DarknetAppConnector.configured)
					change = true;
				Log.d(MDNSReceiver.TAG,"Raising Intent");
				raiseIntent(change);
			}
    	}
    	/**
    	 * Raise intent asking user if he wishes a change in homenode
    	 * @param change false - first configuration ; true - when a new node is detected
    	 */
		public void raiseIntent(boolean change) {
			HomeNode.setTemp(name,port,ip,pin);
			Activity uiActivity = DarknetAppConnector.activity;
			String text  = uiActivity.getString(R.string.main_screen_connect1)+" " + name+ " " + uiActivity.getString(R.string.main_screen_connect2);
			if (change) text = uiActivity.getString(R.string.main_screen_change)+" " + name;
			if (DarknetAppConnector.fragmentManager.getBackStackEntryCount()>0) {
				DarknetAppConnector.fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
			Fragment fragment = AuthorizationFragment.instantiate(text, DarknetAppConnector.handler);
			FragmentTransaction transaction = DarknetAppConnector.fragmentManager.beginTransaction();
			transaction.add(R.id.fragment_view, fragment,AuthorizationFragment.TAG);
			transaction.addToBackStack(AuthorizationFragment.TAG);
			transaction.commit();
		}
		
		/**
		 * Result of the intent raised
		 * If user has accepted, connect with homenode and pull homereference
		 * @param result  accept/reject status
		 */
		public static void handleResult(boolean result) {
	       	 if (result) {
	       		 		HomeNode.finalizeHome();
	       		 		File file = new File(DarknetAppConnector.nodeRefFileName);
	       		 		if (!file.exists()) {
	       		 			try {
									file.createNewFile();
								} catch (IOException e) {
									e.printStackTrace();
								}
	       		 		}
	       		 		Log.d(MDNSReceiver.TAG,HomeNode.getIP());
	       		 		Log.d(MDNSReceiver.TAG,""+HomeNode.getPort());
	       		 		Runnable r = new ConnectionWithServer(HomeNode.getIP(),HomeNode.getPort(),HomeNode.getPin(),HomeNode.getName(),true);
	       		 		new Thread(r).start();
	       		 	}
	       	 else {
	       		 if(!DarknetAppConnector.configured) { 
		       		 Message message = new Message();
		       		 message.arg1 = DarknetAppConnector.MESSSAGE_UNINITIALIZED;
		       		 DarknetAppConnector.handler.sendMessage(message);
	       		 }
	       	 }
	}


}
