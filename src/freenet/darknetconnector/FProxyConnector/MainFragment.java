package freenet.darknetconnector.FProxyConnector;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
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
	
	private static char fragmentState = 0;
	private static boolean auth_result = false;
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
		return MainFragment.view;
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
