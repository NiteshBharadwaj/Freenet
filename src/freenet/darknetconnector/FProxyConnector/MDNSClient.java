package freenet.darknetconnector.FProxyConnector;

/**
 * A helper class with  methods to start and stop MDNS Discovery
 * If a broadcast is discovered, it verifies the sign and sends data to the MDNSReceiver
 * @author Illutionist 
 */

import java.io.IOException;
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


public class MDNSClient {
	WifiManager.MulticastLock lock;
	Handler handler = new android.os.Handler();
	private String type = "_http._tcp.local.";
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
		                    byte[] go = ev.getInfo().getTextBytes();  // This contains the publickey, signature and a pointer
		                    byte[] signature = null;
		                    byte[] publickey = null;
		                    int point = go[go.length-1]; //Extract Pointer
                            signature = new byte[point];  //Initialize Variables
                            publickey = new byte[go.length-signature.length-1];
                            for (int i=0;i!=point;i++) {
                                signature[i] = go[i]; //Signature is obtained
                            }
                            for (int i=point;i!=go.length-1;i++) {
                                publickey[i-point] = go[i]; //Public Key is obtained
                            }  
		                    boolean verification = DigitalSignature.verify(ev.getInfo().getName(), signature, publickey);
		                    if (verification)  {		                    	
		                    	Map<String,String> map = new HashMap<String,String>();
		                    	map.put("name",ev.getInfo().getName());
		        			 	map.put("ip",additions);
		        			 	map.put("port",String.valueOf(ev.getInfo().getPort()));
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
			 FProxyConnector.activity.startActivity(i);
		 }
		 
     }
    
    
}
