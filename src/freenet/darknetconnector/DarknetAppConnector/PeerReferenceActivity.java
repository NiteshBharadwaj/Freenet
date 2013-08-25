package freenet.darknetconnector.DarknetAppConnector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PeerReferenceActivity extends Activity {
	private String reference;
	private int code;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.authorization_layout);
		Bundle extras = getIntent().getExtras();
		reference = extras.getString("text");
		code = extras.getInt("code", 100);
		TextView maintext = (TextView) findViewById(R.id.authorization_text);
		maintext.setMovementMethod(new ScrollingMovementMethod());
		maintext.setText(reference);
		if (((DarknetAppConnector.activity.getResources().getConfiguration().screenLayout & 
			    Configuration.SCREENLAYOUT_SIZE_MASK) == 
			        Configuration.SCREENLAYOUT_SIZE_SMALL) || (DarknetAppConnector.activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
			    // on a small screen device ...
				maintext.setMaxLines(4);
		}
		Button verify_true = (Button) findViewById(R.id.verify_button_true); 
		Button verify_false = (Button) findViewById(R.id.verify_button_false);
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
		Intent in = new Intent();
		in.putExtra("check", result);
		in.putExtra("reference", reference);
		this.setResult(code,in);
		this.finish();
	}
	
}
