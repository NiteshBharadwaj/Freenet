package freenet.darknetconnector.DarknetAppConnector;
/**
 * The main app screen is divided into two halfs. with upper half displaying important text information
 * This is the lower half which handles all the user interactions (button clicks etc.)
 * Use this fragment itself for all the activities' UI.. Add the layout for an activity as an xml and call it a state. This way states can be reused 
 * @author Illutionist
 */
import freenet.darknetconnector.FProxyConnector.R;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainFragment extends Fragment{
	public static View view;
	public static Activity activity;
	private static LayoutInflater inflater;
	public static ViewGroup container;
	public final static char FRAGMENT_STATE_UNINITIALIZED= 0;
	public final static char FRAGMENT_STATE_AUTHORIZATION= 1;
	public final static char FRAGMENT_STATE_OPTIONS = 2;
	public final static char FRAGMENT_STATE_WIFI = 3;
	public final static char MSG_CONFIGURED_FIRST_TIME = 0;
	private static char fragmentState = 0;
	private static boolean auth_result = false;
	public static Handler handler;
	
	public static void setState(char state) {
		MainFragment.fragmentState = state;
		MainFragment.redraw();
	}
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MainFragment.inflater = inflater;
		MainFragment.container = container;
		MainFragment.view = inflater.inflate(R.layout.main_fragment_layout,null);
		activity = getActivity();
		handler = new UIHandler();
		return MainFragment.view;
	}
	
	// Some operations that are to be performed only from main thread
	// Slave threads send commands
	public class UIHandler extends Handler {
		public UIHandler() {
			
		}
		@Override
        public void handleMessage(Message msg) {
            int command = (int)msg.arg1;
            switch(command) {
            	case MainFragment.MSG_CONFIGURED_FIRST_TIME: {
            		String name = (String)msg.obj;
		 			DarknetAppConnector.updatePropertiesFile(name);
		 			HomeNode.setName(name);
		 			DarknetAppConnector.configured = true;
            	}
            }
            
        }
	}
	public static void redraw() {
		if (fragmentState == MainFragment.FRAGMENT_STATE_AUTHORIZATION) {
			activity.findViewById(R.id.authorization_layout).setVisibility(View.VISIBLE);
			activity.findViewById(R.id.node_configured_layout).setVisibility(View.GONE);
		}
		else if (fragmentState == MainFragment.FRAGMENT_STATE_OPTIONS)  {
			activity.findViewById(R.id.authorization_layout).setVisibility(View.GONE);
			activity.findViewById(R.id.node_configured_layout).setVisibility(View.VISIBLE);
		}
		else if (fragmentState == MainFragment.FRAGMENT_STATE_WIFI) {
			activity.findViewById(R.id.authorization_layout).setVisibility(View.GONE);
			activity.findViewById(R.id.node_configured_layout).setVisibility(View.VISIBLE);
		}
		else {
			activity.findViewById(R.id.authorization_layout).setVisibility(View.GONE);
			activity.findViewById(R.id.node_configured_layout).setVisibility(View.GONE);
		}
	}
	
}
