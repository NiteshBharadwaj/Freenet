package freenet.darknetconnector.FProxyConnector;

/**
 * A helper activity for making boolean requests to the User
 * Displays an activity with a text and two buttons 
 * @param an intent with a text and an integer code to identify the requester class
 * The requester must implement the onActivityResult
 * TODO: Change from activity to a much user friendly framework
 * @author Illutionist 
 */

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AuthorizationActivity extends Activity {
	
	private static int code;
	private static Activity activity;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.activity_fproxy_authorize);
		this.activity = this;
		Bundle extras = getIntent().getExtras();
		String text = extras.getString("text");
		code = extras.getInt("code", 100);
		TextView maintext = (TextView) findViewById(R.id.main_screen_text1);
				maintext.setText(text);
		Button verify_true = (Button) this.findViewById(R.id.verify_button_true); 
		Button verify_false = (Button) this.findViewById(R.id.verify_button_false);
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
	
	private static void returnResult(boolean bool) {
		Intent in = new Intent();
		in.putExtra("check", bool);
		activity.setResult(code,in);
		activity.finish();
	}

}
