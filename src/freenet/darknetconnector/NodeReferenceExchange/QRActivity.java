package freenet.darknetconnector.NodeReferenceExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import freenet.darknetconnector.DarknetAppConnector.DarknetAppConnector;
import freenet.darknetconnector.DarknetAppConnector.QRDisplayActivity;
import freenet.darknetconnector.DarknetAppConnector.R;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
	private void setListeners() {
		TextView text = (TextView) view.findViewById(R.id.authorization_text);
		text.setText("An external barcode reader app might be needed");
		Button encode = (Button) view.findViewById(R.id.verify_button_true); 
		Button  decode = (Button) view.findViewById(R.id.verify_button_false);
		encode.setText("Make QR");
		decode.setText("Scan");
		//final IntentIntegrator qr = new IntentIntegrator(DarknetAppConnector.activity);
		encode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getActivity(),QRDisplayActivity.class);
				startActivity(intent);
			}
			
		});
		decode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent("freenet.darknetconnector.QRCode.SCAN");
				intent.putExtra("freenet.darknetconnector.QRCode.SCAN.SCAN_MODE", "QR_CODE_MODE");
				getActivity().startActivityForResult(intent, 210);
				//qr.initiateScan();
			}
			
		});
	}
}
