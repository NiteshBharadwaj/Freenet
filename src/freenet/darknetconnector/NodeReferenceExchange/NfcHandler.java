/**
 * To handle nfc based short message exchange
 * To be only called from OS version >= 14 (ICS)
 */
package freenet.darknetconnector.NodeReferenceExchange;

import java.io.UnsupportedEncodingException;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class NfcHandler implements CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	private static final String TAG = "NfcHandler";
	private Activity activity;
	private NfcAdapter nfcAdapter;
	private IntentFilter[] intentFiltersArray;
	private String[][] techLists;
	private PendingIntent pendingIntent;
	private boolean isEnabled;
	
	public NfcHandler(Activity activity) {
		this.activity = activity;
		nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
		if (nfcAdapter == null) {
			isEnabled = false;
			return;
		}
		isEnabled = true;
		// Register callback to set NDEF message
        nfcAdapter.setNdefPushMessageCallback(this,activity); 
        // Register callback to listen for message-sent success
        nfcAdapter.setOnNdefPushCompleteCallback(this,activity);
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        this.intentFiltersArray = new IntentFilter[] {ndef,};
        techLists = new String[][] { new String[] { DarknetAppConnector.class.getName() } };
        pendingIntent = PendingIntent.getActivity(activity, 0,
                new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) {
		Log.d("NfcHandler","done messaging");
		BluetoothActivity fragment = (BluetoothActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
		if (fragment!=null)  {
			fragment.onNdefPushComplete(event);
			return;
		}
		WifiDirectActivity fragment2 = (WifiDirectActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
		if (fragment2!=null) fragment2.onNdefPushComplete(event);
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		NdefMessage msg = null;
		BluetoothActivity fragment = (BluetoothActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
		if (fragment!=null)  {
			msg = fragment.createNdefMessage(event);
			if (msg!=null) return msg;
		}
		WifiDirectActivity fragment2 = (WifiDirectActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
		if (fragment2!=null)  {
			msg = fragment2.createNdefMessage(event);
			if (msg!=null) return msg;
		}
		NdefRecord ndefRec = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,"Freenet".getBytes(),"Exchange not started".getBytes());
		msg = new NdefMessage(new NdefRecord[]{ ndefRec });
	    return msg;
	}
	
	public void onResume() {
		if (nfcAdapter != null) nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray,
                techLists);
	}
	
	public void processIntent(Intent intent) throws UnsupportedEncodingException {
	    Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
	            NfcAdapter.EXTRA_NDEF_MESSAGES);
	    
	    NdefMessage msg = (NdefMessage) rawMsgs[0];
	    NdefRecord[] recs = msg.getRecords();
	    byte[] type = recs[0].getType();
	    byte[] value = recs[0].getPayload();
	    String str = new String(value,"UTF-8");
	    Log.d(NfcHandler.TAG,str+ "-->>received");
	    Log.d(NfcHandler.TAG,intent+ "-->>received");
	    BluetoothActivity fragment = (BluetoothActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(BluetoothActivity.TAG);
		if (fragment!=null)  {
		    Message msg2 = new Message();
			msg2.arg1 = BluetoothActivity.BT_DATA_RECEIVED_REQUEST_CODE;
			msg2.obj = new String(value,"UTF-8");;
			BluetoothActivity.handler.sendMessage(msg2);
		}
		WifiDirectActivity fragment2 = (WifiDirectActivity) DarknetAppConnector.fragmentManager.findFragmentByTag(WifiDirectActivity.TAG);
		if (fragment2!=null)  {
			Message msg2 = new Message();
			msg2.arg1 = WifiDirectActivity.WIFI_DIRECT_DATA_RECEIVED_REQUEST_CODE;
			msg2.obj = new String(value,"UTF-8");;
			WifiDirectActivity.handler.sendMessage(msg2);
		}
	}
	
	/**
	 * If the main activity receives an nfc message, it is handed to us here
	 * @param intent
	 */
	public void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
	    try {
			processIntent(intent);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void onPause() {
		if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(activity);		
	}

}
