package freenet.darknetconnector.DarknetAppConnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import freenet.darknetconnector.NodeReferenceExchange.BluetoothActivity;
import freenet.darknetconnector.NodeReferenceExchange.WifiDirectActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class OptionsFragment extends Fragment {
	public static final String TAG = "OptionsFragment";
	private View view;
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.options_fragment_layout, null);
		setListeners();
		return view;
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
	    	super.onActivityResult(requestCode, resultCode, intent);
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (((DarknetAppConnector.activity.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_SMALL) || (DarknetAppConnector.activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			    // on a small screen device ...
				LinearLayout optionsLayout = (LinearLayout) view.findViewById(R.id.exchange_options);
				optionsLayout.setOrientation(LinearLayout.HORIZONTAL);
		}
		else {
			//Bigger Device
			LinearLayout optionsLayout = (LinearLayout) view.findViewById(R.id.exchange_options);
			optionsLayout.setOrientation(LinearLayout.VERTICAL);
		}
	}
	private void setListeners() {
		Button wifi_button = (Button) view.findViewById(R.id.wifi_button);
		Button QR_button = (Button) view.findViewById(R.id.QR_button);
		Button bluetooth_button = (Button) view.findViewById(R.id.bluetooth_button);
		if (((DarknetAppConnector.activity.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_SMALL) || (DarknetAppConnector.activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			    // on a small screen device ...
				LinearLayout optionsLayout = (LinearLayout) view.findViewById(R.id.exchange_options);
				optionsLayout.setOrientation(LinearLayout.HORIZONTAL);
		}
		wifi_button.setOnClickListener(new OnClickListener() {
			@Override
			// Reference Exchange support is disabled until bugs in WiFi Activity are fixed
			public void onClick(View view) {
				//Intent i = new Intent(DarknetAppConnector.this,WiFiActivity.class);
				//activity.startActivityForResult(i,100);
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_WIFI_DIRECT;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
				Log.d("dumb","wifi");
			}
		});
		QR_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("dumb","qr");
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_QR;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
			}
		});
		bluetooth_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("dumb","bluetooth");
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_BLUETOOTH;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
			}
		});
	}
}
