package freenet.darknetconnector.FProxyConnector;

/**
 * The main activity that starts when application is started
 * The application currently pulls Freenet node reference from fproxy if the updated plugin MDNSDiscovery is enabled 
 * @author Illutionist
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FProxyConnector extends Activity {

	public final static String propertiesFileName = Environment.getExternalStorageDirectory() + File.separator +"config.properties";
	public final static String nodeRefFileName = Environment.getExternalStorageDirectory() + File.separator +"myref.txt"; 
	public static boolean configured = false;
	private MDNSClient mdnsClient; 
	private static Properties prop = new Properties(); 
	private static Context context;
	public static boolean mdnsCallReceived = false;
	public static Activity activity;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		context = getBaseContext();
		activity = this;	
		initialize();
		mdnsClient = new MDNSClient(context);
		mdnsClient.startListening();
		if (configured) {
			setListeners();
		}
		Log.d("dumb","notified");

	}
	
	protected void onPause() {
		super.onPause();
		//mdnsClient.stopListening();
	}
	protected void onResume() {
		super.onResume();
		//setContentView(R.layout.activity_fproxy_connector);
		//mdnsClient.startListening();
	}
	protected void onStop() {
		super.onStop();
		//mdnsClient.stopListening();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_fproxy_connector, menu);
		return true;
	}
	
	private void initialize() {
		File pfile = new File(propertiesFileName);	
		File nfile = new File(nodeRefFileName);	
		try {
			if (!pfile.exists()) pfile.createNewFile();
			if (!nfile.exists()) nfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			String name = prop.getProperty("homeNodeName");
			if (name!=null && name.length()!=0) {
				HomeNode.setName(name);
				TextView text = (TextView) findViewById(R.id.main_screen_text);
				text.setText(this.getString(R.string.main_screen_configured)+" " +name);
				configured = true;
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public static void updatePropertiesFile(String name) {
		File pfile = new File(propertiesFileName);	
		try {
			if (!pfile.exists()) pfile.createNewFile();
			prop.load(new FileInputStream(pfile));
			if (name!=null && name.length()!=0) {
				prop.setProperty("homeNodeName", name);
				FProxyConnector.configured = true;
				prop.store(new FileOutputStream(pfile), null);
				TextView text = (TextView) activity.findViewById(R.id.main_screen_text);
				text.setText(activity.getString(R.string.main_screen_configured)+" " +name);
				setListeners();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	public static void setListeners() {
		MainFragment.setState(MainFragment.FRAGMENT_STATE_OPTIONS);
		View view = MainFragment.view;
		Button wifi_button = (Button) view.findViewById(R.id.wifi_button);
		Button QR_button = (Button) view.findViewById(R.id.QR_button);
		Button bluetooth_button = (Button) view.findViewById(R.id.bluetooth_button);
		wifi_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				//Intent i = new Intent(FProxyConnector.this,AuthorizationActivity.class);
				//activity.startActivityForResult(i,100);
				Log.d("dumb","wifi");
			}
		});
		QR_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("dumb","qr");
			}
		});
		bluetooth_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.d("dumb","bluetooth");
			}
		});
	}
}
