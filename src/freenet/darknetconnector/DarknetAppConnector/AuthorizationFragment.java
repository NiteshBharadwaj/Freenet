package freenet.darknetconnector.DarknetAppConnector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AuthorizationFragment extends Fragment {
	public static final String TAG = "AuthorizationFragment";
	private View view;
	private static String text;
	private static Handler handler;
	
	public static Fragment instantiate(String text,Handler handler) {
		AuthorizationFragment.text = text;
		AuthorizationFragment.handler = handler;
		return new AuthorizationFragment(); 
	}
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);        
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.authorization_layout, null);
		setListeners();
		return view;
	}
	
	private void setListeners() {
		TextView maintext = (TextView) view.findViewById(R.id.authorization_text);
		maintext.setText(text);
		Button verify_true = (Button) view.findViewById(R.id.verify_button_true); 
		Button verify_false = (Button) view.findViewById(R.id.verify_button_false);
		verify_true.setVisibility(View.VISIBLE);
		verify_false.setVisibility(View.VISIBLE);
		verify_true.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				returnResult(true);
			}
		});
		verify_false.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				returnResult(false);
			}
		});
	}
	private void returnResult(boolean result) {
		view.findViewById(R.id.verify_button_false).setVisibility(View.GONE);
		view.findViewById(R.id.verify_button_true).setVisibility(View.GONE);
		text = "Please Wait";
		TextView maintext = (TextView) view.findViewById(R.id.authorization_text);
		maintext.setText(text);
		maintext.setVisibility(View.GONE);
		Message message = new Message();
		message.arg1 = DarknetAppConnector.MESSAGE_AUTHORIZATION_RESULT;
		message.obj = result;
		handler.sendMessage(message);
		handler = null;
		text = null;
	}

}
