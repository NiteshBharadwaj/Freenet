package freenet.darknetconnector.NodeReferenceExchange;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ClientThread extends Thread {
	private static final String TAG = "ClientThread";
	private Socket socket;
	private BluetoothSocket bsocket;
	private int port = 7826;
	private InetAddress groupOwnerAddress;
	private ConnectionThread connectionThread;
	private boolean isBluetooth = false;
	private BluetoothAdapter bluetoothAdapter;
	public ClientThread(InetAddress groupOwnerAddress) {
		this.groupOwnerAddress = groupOwnerAddress;
		isBluetooth = false;
	}
	public ClientThread(BluetoothDevice device, BluetoothAdapter bluetoothAdapter) {
		// Get a BluetoothSocket to connect with the given BluetoothDevice
		this.bluetoothAdapter = bluetoothAdapter;
	     try {
	    	 Log.d(ClientThread.TAG,"acquiring client socket");
	    	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
	    	bsocket = (BluetoothSocket) m.invoke(device, 9);
	    	//bsocket = device.createRfcommSocketToServiceRecord(BluetoothActivity.APP_UUID);
	    	Thread.sleep(100);
	     } catch (Exception e) {
			Log.e("ClientThread","Socket not acquired",e);
		}
	     isBluetooth = true;
	}
	public void run() {
		Log.d("ClientThread","Waiting for socket");
		if (!isBluetooth) {
			try {
				socket = new Socket(groupOwnerAddress,port);
			} catch (IOException e) {
				Log.e("ClientThread","Could not connect to server",e);
			}
			Log.d("ClientThread","Socket acquired");
			if (socket!=null)
				manageConnectedSocket(socket);
		}
		else {
			 // Cancel discovery because it will slow down the connection
		    bluetoothAdapter.cancelDiscovery();
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	        	Log.d("ClientThread","Asking for approval");
	            bsocket.connect();
	        } catch (IOException connectException) {
	        	Log.e("ClientThread","Of course exception during connection" + " --- -- -"+ connectException.toString(),connectException);
	        	connectException.printStackTrace();
	        	
	            // Unable to connect; close the socket and get out
	            try {
	                bsocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	        // Do work to manage the connection (in a separate thread)
	        Log.d("ClientThread","Approved and socket acquired");
	        BluetoothActivity.manageConnectedSocket(bsocket,true);
		}
	}
	private void manageConnectedSocket(Socket socket) {
		if (connectionThread!=null) connectionThread.cancel();
		connectionThread = new ConnectionThread(socket);
		connectionThread.start();
		Log.d("ClientThread","Connection thread started");
	}
	public void cancel() {
		Log.d("ClientThread","Client is being closed");
		if (socket!=null)
			try {
				socket.close();
			} catch (IOException e) {
				Log.e("ClientThread","ErrorClosingConnection",e);
			}
		if (bsocket!=null) 
			try {
				bsocket.close();
			} catch (IOException e) {
				Log.e("ClientThread","ErrorClosingConnection",e);
			}
		if (connectionThread!=null) connectionThread.cancel();
		Log.d("ClientThread","Client closed");
		
	}
}
