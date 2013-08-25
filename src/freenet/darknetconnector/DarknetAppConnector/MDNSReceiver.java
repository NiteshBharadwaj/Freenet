package freenet.darknetconnector.DarknetAppConnector;

/**
 * A MDNS receiver class that receives MDNS broadcasts from the Asynchronous background task MDNSClient
 * It raises intent asking if home node is to be changed or if to be associated with a home node for the first time as soon as it discovers a broadcast
 * If already associated with a homeNode, it automatically synchronizes the nodereferences 
 * @author Illutionist
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import freenet.darknetconnector.DarknetAppConnector.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class MDNSReceiver extends Thread {
	
    	public static String name;
    	public static String ip;
    	public static int port;
    	public static String pin;
    	public MDNSReceiver(String name, String ip, int port, String pin) {
    			this.name = name;
        		this.ip = ip;
        		this.port = port;
        		this.pin = pin;
        		Log.d("dumb",pin);
    	}
    	public void run() {
    		if (DarknetAppConnector.configured && HomeNode.getName().equals(name)) {
    			//We reach here if newly discovered MDNS is same as our homenode
    			// TODO: Add pin to homenode and also check if the pins match
    			if ((DarknetAppConnector.newDarknetPeersCount!=DarknetAppConnector.newDarknetPeersCountPrev && DarknetAppConnector.newDarknetPeersCount>0) || System.currentTimeMillis()-DarknetAppConnector.lastSynched > 60*60*60*1000) {
    				Runnable r = new ConnectionWithServer(ip,port,pin,name,false);
    				new Thread(r).start();
    			}
			}
			else {
				boolean change = false;
				if (DarknetAppConnector.configured)
					change = true;
				Log.d("dumb","Raising Intent");
				raiseIntent(change);
			}
    	}
		public void raiseIntent(boolean change) {
			Activity uiActivity = DarknetAppConnector.activity;
			String text  = uiActivity.getString(R.string.main_screen_connect1)+" " + name+ " " + uiActivity.getString(R.string.main_screen_connect2);
			if (change) text = uiActivity.getString(R.string.main_screen_change)+" " + name;
			Fragment fragment = AuthorizationFragment.instantiate(text, DarknetAppConnector.handler);
			FragmentTransaction transaction = DarknetAppConnector.fragmentManager.beginTransaction();
			transaction.add(R.id.fragment_view, fragment,AuthorizationFragment.TAG);
			transaction.addToBackStack(AuthorizationFragment.TAG);
			transaction.commit();
		}
		
		public static void handleResult(boolean result) {
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
	       		 		Runnable r = new ConnectionWithServer(ip,port,pin,name,true);
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
