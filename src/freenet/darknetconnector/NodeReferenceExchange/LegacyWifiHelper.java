package freenet.darknetconnector.NodeReferenceExchange;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class LegacyWifiHelper extends Thread {
	private static final String TAG = "LegacyWifiHelper";
	private String SSID;
	private Context context;
	private String passkey;
	private String IPString = "192.168.49.1";
	private String MAC_ID;
	private WifiManager wifiManager;
	private String prevSSID;
	private boolean initialization =false;
	public LegacyWifiHelper(Context context, String SSID, String passkey, String IP, String MAC)  {
		this.SSID = SSID;
		this.context = context;
		this.passkey = passkey;
		String[] split = IP.split(""+'/');
		for (String spl :split) {
			if (spl.startsWith("192.")) 
				this.IPString = spl;
		}
		this.MAC_ID = MAC;
	}
	public void add(String SSID, String passkey, String IP, String MAC) {
		this.SSID = SSID;
		this.passkey = passkey;
		String[] split = IP.split(""+'/');
		for (String spl :split) {
			if (spl.startsWith("192.")) 
				this.IPString = spl;
		}
		this.MAC_ID = MAC;
	}
	public LegacyWifiHelper(Context context) {
		initialization = true;
		this.context = context;
	}
	public void run() {
		if (initialization) {
			saveNetwork();
			synchronized(this) {
				try {
					this.wait();
					initialization = false;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		if (!initialization)
		try {
			changeNetwork();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	private void saveNetwork() {
		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
        WifiInfo currentWifi = wifiManager.getConnectionInfo();
        if (currentWifi!=null) prevSSID = currentWifi.getSSID();
        wifiManager.disconnect();
	}
	private void changeNetwork() throws UnknownHostException {
		Log.d(LegacyWifiHelper.TAG,"starting stuff");
        Log.d(LegacyWifiHelper.TAG,prevSSID);
		InetAddress IP = null;
		IP = InetAddress.getByName(IPString);
		if (IP==null) return;
		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + SSID + "\"";
		Log.d("ssid","SSID-->>"+"\"" + SSID + "\"");
		conf.preSharedKey = "\""+ passkey +"\"";
		 conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		    conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA); // For WPA
		    conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN); // For WPA2
		    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
		    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		    conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		    conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		Log.d("passkey","passkey-->>"+"\""+ passkey +"\"");
		wifiManager.addNetwork(conf);
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
		    if(i.SSID != null && (i.SSID.equals("\"" + SSID+ "\"") || i.SSID.equals(SSID))) {
		    	 wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
		         wifiManager.enableNetwork(i.networkId, true);
		         Log.d(LegacyWifiHelper.TAG,"Requested OS to connect to "+ i.SSID);
		         if (!wifiManager.isWifiEnabled()) wifiManager.setWifiEnabled(true);
		         wifiManager.reconnect();               
		         break;
		    }           
		}
		Log.d(LegacyWifiHelper.TAG,"finishing stuff");
	}
	public boolean isCorrectSSID(String checkSSID) {
		return checkSSID.equals("\"" + SSID+ "\"");
	}
	
	public boolean isCorrectAP() {
		boolean result  = false;
		Log.d(LegacyWifiHelper.TAG,"expected mac " + MAC_ID);
		Log.d(LegacyWifiHelper.TAG,"got mac " +  getMacId());
		if (getSSID().equals(SSID) || getSSID().equals("\"" + SSID+ "\"")) result = true;
		int d = hammingDistance(getMacId(),MAC_ID);
		if (d>-1 && d<2 && result ) result = true;
		else result = false;
		return result;
	}
	public InetAddress getIP() {
		try {
			return InetAddress.getByName(IPString);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	public void finish() {
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        wifiManager.disconnect();
		for( WifiConfiguration i : list ) {
		    if(i.SSID != null) {
		    	 if (i.SSID.equals("\"" + SSID+ "\"") || i.SSID.equals(SSID)) {
		         	wifiManager.disableNetwork(i.networkId);
		    	 }
		    	 if (prevSSID!=null && !prevSSID.equals("")) {
		    		 if (i.SSID.equals("\"" + prevSSID+ "\"") || i.SSID.equals(prevSSID)) {
		    			 wifiManager.enableNetwork(i.networkId, true);
				         wifiManager.reconnect();
		    		 }
		    	 }
		    }           
		}
	}
	private int hammingDistance(String s1,String s2) {
		if (s1.length()!=s2.length()) return -1;
		int d = 0;
		for (int i=0; i!= s1.length();i++) {
			if (s1.charAt(i)!=s2.charAt(i)) d++;
		}
		return d;
	}
	private String getMacId() {
	    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	    return wifiInfo.getBSSID();
	}
	private String getSSID() {
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		return wifiInfo.getSSID();
	}
}
