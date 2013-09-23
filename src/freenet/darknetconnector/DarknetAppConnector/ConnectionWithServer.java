package freenet.darknetconnector.DarknetAppConnector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.os.Message;

public class ConnectionWithServer implements Runnable {
	String ip;
	int port;
	String pin;
	String name;
	boolean firstTime =false;
	public ConnectionWithServer(String ip, int port, String pin, String name, Boolean firstTime) {
		this.ip = ip;
		this.port = port;
		this.pin = pin;
		this.name = name;
		this.firstTime = firstTime;
	}

	@Override
	public void run() {
		boolean done = false;
		File fl = new File(DarknetAppConnector.nodeRefFileName);
		DarknetAppClient context = new DarknetAppClient(ip,port,true,DarknetAppConnector.activity.getApplicationContext(),pin);
		boolean connected = context.startConnection();
		String ref;
		try {
			if (connected) {
				ref = context.pullHomeReference();
				if (DarknetAppConnector.newDarknetPeersCount>0) 
					context.pushReferences();
				//FileUtils.writeStringToFile(fl, ref);
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fl));
				outputStreamWriter.write(ref);
		        outputStreamWriter.close();
				context.closeConnection();
				done = true;
			}
		} catch (IOException e) {
			if (DarknetAppConnector.newDarknetPeersCount>0) {
				Message msg2 = new Message();
       		 	msg2.arg1 = DarknetAppConnector.MESSAGE_TIMER_TASK;
       		 	DarknetAppConnector.handler.sendMessageDelayed(msg2, 60*1000);
			}
			e.printStackTrace();
		}
		if (done && firstTime) {
			Message msg = new Message();
			char uiCommand = DarknetAppConnector.MESSAGE_CONFIGURED_FIRST_TIME;
			msg.arg1 = uiCommand;
			msg.obj = name+"-->>"+pin;
			DarknetAppConnector.handler.sendMessage(msg);
 		}
		DarknetAppConnector.lastSynched = System.currentTimeMillis();
	}
}
