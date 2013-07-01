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
import android.view.Menu;
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
		setContentView(R.layout.activity_fproxy_connector);
		context = getBaseContext();
		activity = this;	
		initialize();
		mdnsClient = new MDNSClient(context);
		mdnsClient.startListening();
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
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
