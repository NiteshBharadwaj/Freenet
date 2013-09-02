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
	private String SSID;
	private Context context;
	private String passkey;
	private String IPString = "192.168.49.1";
	private String MAC_ID;
	private WifiManager wifiManager;
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
	public void run() {
		try {
			changeNetwork();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	private void changeNetwork() throws UnknownHostException {
		Log.d("dumb","starting stuff");
		InetAddress IP = null;
		IP = InetAddress.getByName(IPString);
		if (IP==null) return;
		WifiConfiguration conf = new WifiConfiguration();
		conf.SSID = "\"" + SSID + "\"";
		Log.d("ssid","SSID-->>"+"\"" + SSID + "\"");
		conf.preSharedKey = "\""+ passkey +"\"";
		Log.d("passkey","passkey-->>"+"\""+ passkey +"\"");
		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE); 
		wifiManager.addNetwork(conf);
		
		List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
		for( WifiConfiguration i : list ) {
		    if(i.SSID != null && i.SSID.equals("\"" + SSID+ "\"")) {
		    	 Log.d("dumb","came here");
		         wifiManager.disconnect();
		         wifiManager.enableNetwork(i.networkId, true);
		         wifiManager.reconnect();               
		         break;
		    }           
		}
		Log.d("dumb","finishing stuff");
	}
	public boolean isCorrectSSID(String checkSSID) {
		return checkSSID.equals("\"" + SSID+ "\"");
	}
	
	public boolean isCorrectAP() {
		Log.d("dumb","got SSID--->" + getSSID());
		Log.d("dumb","assumed SSID--->" + SSID);
		Log.d("dumb","got MACID--->" + getMacId());
		Log.d("dumb","assumed MACID--->" + MAC_ID);

		boolean result  = false;
		if (getSSID().equals(SSID) || getSSID().equals("\"" + SSID+ "\"")) result = true;
		if ( (getMacId().equals(MAC_ID) || getMacId().equals("\"" + MAC_ID+ "\"")) && result ) result = true;
		if (getMacId().equals(MAC_ID)) Log.d("dumb","1");
		if (getMacId().equals("\"" + MAC_ID+ "\"")) Log.d("dumb","2");
		if (getMacId().length() == MAC_ID.length()) Log.d("dumb","3");
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
	private String getMacId() {
	    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	    return wifiInfo.getBSSID();
	}
	private String getSSID() {
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		return wifiInfo.getSSID();
	}
}
