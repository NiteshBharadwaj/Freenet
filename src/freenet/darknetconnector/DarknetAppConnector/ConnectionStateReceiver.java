package freenet.darknetconnector.DarknetAppConnector;

import freenet.darknetconnector.NodeReferenceExchange.WifiDirectActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

public class ConnectionStateReceiver extends BroadcastReceiver {
	private final String TAG = "receiver";
	private boolean sendMessage;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
			NetworkInfo info =  intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			if (info.isConnected()) {
				Fragment fragment = (Fragment) DarknetAppConnector.fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
				if (fragment!=null) {
					Log.d(TAG,"network state changed");
					Message msg = new Message();
					msg.arg1 = WifiDirectActivity.MESSAGE_NETWORK_CONNECTED;
					WifiDirectActivity.handler.sendMessage(msg);
				}
						
			}
		}
		else if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
			Log.d(TAG,"supplicant state changed");
		}
	}

}
