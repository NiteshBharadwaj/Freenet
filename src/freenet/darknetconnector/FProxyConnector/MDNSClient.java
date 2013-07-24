package freenet.darknetconnector.FProxyConnector;

/**
 * A helper class with  methods to start and stop MDNS Discovery
 * If a broadcast is discovered, it verifies the sign and sends data to the MDNSReceiver
 * @author Illutionist 
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;


import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;


public class MDNSClient {
	WifiManager.MulticastLock lock;
	Handler handler = new android.os.Handler();
	private String type = "_darknetAppServer._tcp.local.";
    private JmDNS jmdns = null;
    private ServiceListener listener = null;
    private Context context;
    private WifiManager wifi;
	
    public MDNSClient(Context context) {
    	this.context = context;
    	wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("lock");
        lock.setReferenceCounted(true);
	}
    
    public void startListening() {
    	//Start a thread that listens asynchronously for MDNS broadcasts
    	final Runnable r = new Runnable()
    	{
    		@Override
    	    public void run() 
    	    {
    	    	new AsynchronousExecutor().execute(jmdns); 	
    	    }
    	};

    	new Thread(r).start();
    	
    }
    
    public void stopListening() {
    	if (jmdns != null) {
            if (listener != null) {
                jmdns.removeServiceListener(type, listener);
                listener = null;
            }
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            jmdns = null;
    	}
        lock.release();
    }
    
    class AsynchronousExecutor extends AsyncTask<JmDNS, Void,JmDNS> {
    	
        private byte[] int2Bytes(int IP) {
			byte[] inetBytes = new byte[] {
					(byte) (IP & 0xff),
					(byte) (IP >> 8 & 0xff),
					(byte) (IP >> 16 & 0xff),
					(byte) (IP >> 24 & 0xff)
			};
			return inetBytes;
        }
        /*
         * Listen for MDNS broadcasts. If we get hold of a broadcast check for signature
         * If signature matches, raise Intent through MDNSReceiver Class
         * TODO: Verify if the name contains Freenet. If it doesn't ignore the broadcast instead of signature verification
         * TODO: Change the dumb function to do the same task onPostExecute 
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
		@Override
		protected JmDNS doInBackground(JmDNS... params) {
				 WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				 WifiInfo connectionInfo = wifiManager.getConnectionInfo();
					if(connectionInfo == null) {
						return null;
					}

					int WiFiIP = connectionInfo.getIpAddress();
					InetAddress myAddress;
					try {
					myAddress = InetAddress.getByAddress(int2Bytes(WiFiIP));
					lock.acquire();
					jmdns = JmDNS.create(myAddress);
					}
					catch (UnknownHostException e) {
						//Ignore as of now
					} catch (IOException e) {
						//Ignore as of now
					}
					if (jmdns==null) return null;
		            jmdns.addServiceListener(type, listener = new ServiceListener() {

		                @Override
		                public void serviceResolved(ServiceEvent ev) {
		                    String additions = "";
		                    if (ev.getInfo().getInetAddresses() != null && ev.getInfo().getInetAddresses().length > 0) {
		                        additions = ev.getInfo().getInetAddresses()[0].getHostAddress();
		                    }
		                    byte[] signal = ev.getInfo().getTextBytes();  // This contains the publickey, signature and a pointer
		                    byte[] signature = null;
		                    byte[] publickey = null;
		                    byte[] pinBytes = null;
		                    int signEndPointer = signal[signal.length-2]*16 + signal[signal.length-1];
		                    int pubkeyEndPointer = signal[signal.length-4]*16 + signal[signal.length-3];
		                    signature = new byte[signEndPointer];
		                    publickey = new byte[pubkeyEndPointer-signEndPointer];
		                    pinBytes = new byte[signal.length-pubkeyEndPointer-4];
		                    for (int i=0;i!=signEndPointer;i++) {
		                        signature[i] = signal[i];
		                    }
		                    for (int i=signEndPointer;i!=pubkeyEndPointer;i++) {
		                        publickey[i-signEndPointer] = signal[i];
		                    }
		                    for (int i=pubkeyEndPointer;i!=signal.length-4;i++) {
		                        pinBytes[i-pubkeyEndPointer] = signal[i];
		                    }
		                    boolean verification = ECDSA.verify(ev.getInfo().getName(), signature, publickey);
		                    Log.d("dumb","+verification" + ev.getInfo().getName());
		                    String pin = "";
							try {
								pin = new String(pinBytes,"UTF-8");
								Log.d("dumb","pin  "+pin);
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                    if (verification)  {		                    	
		                    	Map<String,String> map = new HashMap<String,String>();
		                    	map.put("name",ev.getInfo().getName());
		        			 	map.put("ip",additions);
		        			 	map.put("port",String.valueOf(ev.getInfo().getPort()));
		        			 	map.put("pin", pin);
		                    	dumb(map);		                    	
		                    }
		                }

		                @Override
		                public void serviceRemoved(ServiceEvent ev) {
		                	// Doing nothing
		                }

		                @Override
		                public void serviceAdded(ServiceEvent event) {
		                    // Initial Addition
		                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1);
		                }
		            });

			return jmdns;
		}
		 protected void onPostExecute(JmDNS params) {
			 	//Do Nothing
	        }
		 protected void dumb(Map<String,String> map) {
			 Intent i = new Intent(context.getApplicationContext(),MDNSReceiver.class);
			 i.putExtra("name", (String)map.get("name"));
			 i.putExtra("ip",(String)map.get("ip"));
			 i.putExtra("port",Integer.parseInt((String) map.get("port")));
			 i.putExtra("pin",map.get("pin"));
			 FProxyConnector.activity.startActivity(i);
		 }
		 
     }
    
    
}
