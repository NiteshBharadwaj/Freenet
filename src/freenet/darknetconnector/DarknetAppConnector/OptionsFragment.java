/**
 * Displays different options to exchange nodereferences
 * On choosing an option, the message is transferred to DarknetAppConnector to switch to the corresponding fragment
 */
package freenet.darknetconnector.DarknetAppConnector;

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
import android.widget.TextView;

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
		int count = DarknetAppConnector.newDarknetPeersCount;
		TextView text = (TextView) view.findViewById(R.id.options_title_unsynced_peers);
		if (count==0) text.setText(this.getString(R.string.options_title_no_peers));
		else text.setText(this.getString(R.string.options_title)+count);
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
			public void onClick(View view) {
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_WIFI_DIRECT;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
				Log.d(OptionsFragment.TAG,"wifi");
			}
		});
		
		QR_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d(OptionsFragment.TAG,"qr");
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_QR;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
			}
		});
		
		bluetooth_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d(OptionsFragment.TAG,"bluetooth");
				Message msg = new Message();
				msg.arg1 = DarknetAppConnector.MESSAGE_FRAGMENT_BLUETOOTH;
				msg.arg2 = DarknetAppConnector.MESSAGE_START_FRAGMENT;
				DarknetAppConnector.handler.sendMessage(msg);
			}
		});
	}
}
