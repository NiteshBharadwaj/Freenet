/**
 * A separate activity with full screen to display the QR image of the complete reference
 */
package freenet.darknetconnector.DarknetAppConnector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

public class QRDisplayActivity extends Activity {
	private UIHandler uiHandler = new UIHandler();
	private Bitmap bitmap;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qr_image_layout);
		SlaveThread thread = new SlaveThread();
		thread.start();
	}
	
	private class SlaveThread extends Thread {
		public void run() {
			String ref = "";
			int dimension = 600;
			File file = new File(DarknetAppConnector.nodeRefFileName);
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = br.readLine()) != null) {
					ref = ref.concat(line+'\n');
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			int[] pixels = generateQR(ref,dimension);
			bitmap = Bitmap.createBitmap(dimension,dimension, Bitmap.Config.ARGB_8888);
	        bitmap.setPixels(pixels, 0, dimension, 0, 0, dimension, dimension);
	        Message msg = new Message();
	        msg.arg1 = 11;
	        uiHandler.sendMessage(msg);
	   }
	}
	private class UIHandler extends Handler {
		@Override
        public void handleMessage(Message msg) {
			if (msg.arg1 == 11) {
				ImageView imageview = (ImageView)findViewById(R.id.qr_view_img);
		        imageview.setImageBitmap(bitmap);
				findViewById(R.id.img_alt).setVisibility(View.GONE);
		        imageview.setVisibility(View.VISIBLE);
			}
		}
	}
	public static int[] generateQR(String text,int dimension) {
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix matrix = null;
		try {
			matrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, dimension, dimension);
		} catch (WriterException e) {
			e.printStackTrace();
		}
		int[] pixels = new int[dimension*dimension];
        for (int y = 0;y < dimension; y++) {
            int offset = y * dimension;
            for (int x = 0; x < dimension; x++) {
            	if (matrix.get(x, y))
            		pixels[offset + x] = Color.BLACK;
                else
                	pixels[offset + x] = Color.WHITE;
            }
        }
        return pixels;
	}
	
}
