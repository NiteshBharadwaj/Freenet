package freenet.darknetconnector.DarknetAppConnector;

/**
 * The main activity that starts when application is started
 * The application presently can pull/push nodereferences from a freenet node if the updated plugin MDNSDiscovery is enabled 
 * Once it is configured with a freenet node, that particular node becomes its homeNode.
 * @author Illutionist
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import freenet.darknetconnector.FProxyConnector.R;

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

public class DarknetAppConnector extends Activity {

	// All static variables.. Should remain same for the whole application
	public final static String propertiesFileName = Environment.getExternalStorageDirectory() + File.separator +"config.properties";
	public final static String nodeRefFileName = Environment.getExternalStorageDirectory() + File.separator +"myref.txt"; 
	public final static String peerNodeRefsFileName = Environment.getExternalStorageDirectory() + File.separator +"friendrefs.txt";
	public static boolean configured = false;
	private MDNSClient mdnsClient; 
	private static Properties prop = new Properties(); 
	private static Context context;
	public static Activity activity;
	public static int newDarknetPeersCount = 0;
	
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
			String count = prop.getProperty("newDarknetPeersCount");
			if (count!=null && !count.isEmpty()) newDarknetPeersCount = Integer.parseInt(count);
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
				DarknetAppConnector.configured = true;
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
	
	// Call this from any method to set the default screen with default listeners 
	// Caution: Call only after the mobile is configured with a home node
	public static void setListeners() {
		if (!configured) return;
		MainFragment.setState(MainFragment.FRAGMENT_STATE_OPTIONS);
		View view = MainFragment.view;
		Button wifi_button = (Button) view.findViewById(R.id.wifi_button);
		Button QR_button = (Button) view.findViewById(R.id.QR_button);
		Button bluetooth_button = (Button) view.findViewById(R.id.bluetooth_button);
		wifi_button.setOnClickListener(new OnClickListener() {
			@Override
			// Reference Exchange support is disabled until bugs in WiFi Activity are fixed
			public void onClick(View view) {
				//Intent i = new Intent(DarknetAppConnector.this,WiFiActivity.class);
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
