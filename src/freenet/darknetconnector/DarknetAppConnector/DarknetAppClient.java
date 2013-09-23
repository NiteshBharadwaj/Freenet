package freenet.darknetconnector.DarknetAppConnector;
/**
 * A client to talk to the DarknetAppServer(on homeNode) in terms of sockets
 * @author Illutionist
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Formatter;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;


import freenet.support.io.LineReadingInputStream;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.thoughtcrime.ssl.pinning.PinningTrustManager;
import org.thoughtcrime.ssl.pinning.SystemKeyStore;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

public class DarknetAppClient {
	private static final String TAG = "DarknetAppClient";
	private static String REQUEST_CERTIFICATE = "Certificate";
    private static String REQUEST_HOME_REFERENCE = "HomeReference";
    private static String REQUEST_PUSH_REFERENCE = "PushReference";
    private static String REQUEST_END_MESSAGE = "End";
    private static String REQUEST_CLOSE_CONNECTION = "CloseConnection";
    private static String ASSERT_NODE_REFERENCES_RECEIVED = "ReceivedNodeReferences";
	private String ip;
	private int port;
	private boolean SSLEnabled;
	private Socket socket;
	private OutputStream out;
    private LineReadingInputStream input;
    private Context context;
    private String pin;
    public DarknetAppClient(String ip,int port,boolean SSLEnabled, Context context, String pin) {
		this.ip = ip;
		this.port = port;
		this.SSLEnabled = SSLEnabled;
		this.context = context;
		this.pin = pin;
	}

    public void closeConnection() throws IOException {
    	out.write((REQUEST_CLOSE_CONNECTION+'\n').getBytes("UTF-8"));
    	if (socket!=null && !socket.isClosed()) {
            //socket.shutdownInput();
            //socket.shutdownOutput();
            socket.close();
        }
        if(input!=null) input.close();
        if (out!=null) out.close();
        socket = null;
        input =null;
        out = null;
    }
	public boolean startConnection() {
		boolean done = false;
		try {
			if (SSLEnabled) {
				TrustManager[] trustManagers = new TrustManager[1];
				trustManagers[0] = new PinningTrustManager(SystemKeyStore.getInstance(context), new String[] {pin}, 0);
				SSLContext sslContext = SSLContext.getInstance("TLS");
				sslContext.init(null, trustManagers, null);
				socket = sslContext.getSocketFactory().createSocket(InetAddress.getByName(ip),port );
			}else {
				socket= new Socket(InetAddress.getByName(ip),port );
			}
			InputStream is = new BufferedInputStream(socket.getInputStream(), 4096);
			socket.setSoTimeout(1000);
	        input = new LineReadingInputStream(is);
	        out = socket.getOutputStream();
	        done = true;
		}
		catch(IOException e) {
			onException();
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			onException();
			e.printStackTrace();
		} catch (KeyManagementException e) {
			onException();
			e.printStackTrace();
		}
		if (input==null || out==null || socket==null) done= false;
		return done;
	}
	public String pullHomeReference() throws IOException {
		String homeref = "";
		out.write((REQUEST_HOME_REFERENCE+'\n').getBytes("UTF-8"));
		String sentence2;
		while (!(sentence2 = input.readLine(32768, 128, true)).equals("")) {
			homeref = homeref.concat(sentence2+'\n');
		}
		return homeref;
	}
	public void pushReferences() throws IOException {
		File file = new File(DarknetAppConnector.peerNodeRefsFileName);
		if (!file.exists()) return;
		Properties prop = new Properties();
		prop.load(new FileInputStream(file));
		out.write((REQUEST_PUSH_REFERENCE+'\n').getBytes("UTF-8"));
		String count = String.valueOf(DarknetAppConnector.newDarknetPeersCount);
		out.write((count+'\n').getBytes("UTF-8"));
		int n = Integer.parseInt(count);
		for (int i=1;i<=n;i++) {
			String nodereference = prop.getProperty("newPeers" + i);
			out.write((nodereference+'\n').getBytes("UTF-8"));
		}
		Log.d(DarknetAppClient.TAG,"Pushed all the peer noderefs to homeNode");
		if (input.readLine(32768, 128, true).equals(ASSERT_NODE_REFERENCES_RECEIVED))  {
			DarknetAppConnector.newDarknetPeersCount = 0;
			DarknetAppConnector.updatedPeersCount();
		}
	}
	
	private void onException() {
		// Try again in a minute
		if (DarknetAppConnector.newDarknetPeersCount>0) {
			Message msg2 = new Message();
			msg2.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
			DarknetAppConnector.handler.sendMessageDelayed(msg2, 60*1000);
		}
	}
}
