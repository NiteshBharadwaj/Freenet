package freenet.darknetconnector.NodeReferenceExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.zxing.integration.android.IntentIntegrator;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;
import freenet.darknetconnector.DarknetAppConnector.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class QRActivity extends Fragment {
	private View view;
	public static final String TAG = "AuthorizationFragment";
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		view = inflater.inflate(R.layout.authorization_layout, null);
		setListeners();
		return view;
	}
	@Override
	public void onActivityResult(int requestCode,
                                     int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void setListeners() {
		TextView text = (TextView) view.findViewById(R.id.authorization_text);
		text.setText("An external barcode reader app might be needed");
		Button encode = (Button) view.findViewById(R.id.verify_button_true); 
		Button  decode = (Button) view.findViewById(R.id.verify_button_false);
		encode.setText("Make QR");
		decode.setText("Scan");
		final IntentIntegrator qr = new IntentIntegrator(DarknetAppConnector.activity);
		encode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String ref = "";
				File file = new File(DarknetAppConnector.nodeRefFileName);
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));
					String line = null;
					while ((line = br.readLine()) != null) {
						ref = ref.concat(line+'\n');
						Log.d("dumb","here-- " +line);
					}
					
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				qr.shareText(ref);
			}
			
		});
		decode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				qr.initiateScan();
			}
			
		});
	}
}
