package freenet.darknetconnector.NodeReferenceExchange;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ServerThread extends Thread {
	private ServerSocket serverSocket;
	private BluetoothServerSocket bluetoothServerSocket;
	private Socket socket;
	private int port = 7826;
	private ConnectionThread connectionThread;
	private boolean isBluetooth = false;
	public ServerThread(InetAddress localAddress) {
		try {
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(localAddress,port));
			Log.d("ServerThread","Server started and bound to " + port);
		} catch (IOException e) {
			Log.e("ServerThread","server socket cannot be created: Trying another port",e);
			try {
				serverSocket.bind(new InetSocketAddress(localAddress,8878));
			} catch (IOException e1) {
				Log.e("ServerThread","server socket cannot be created",e1);
			}
		}
		isBluetooth = false;
	}
	public ServerThread(BluetoothAdapter bluetoothAdapter) {
		try {
        	Method m = bluetoothAdapter.getClass().getMethod("listenUsingRfcommOn", new Class[] { int.class });
        	bluetoothServerSocket = (BluetoothServerSocket) m.invoke(bluetoothAdapter,9 );
           //bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("name", BluetoothActivity.APP_UUID);
        } catch (Exception e) {
        	// Unable to start server
        	Log.e("ServerThread","Unable to start server",e);
        	e.printStackTrace();
        }
		isBluetooth = true;
	}
	public void run() {
		if (!isBluetooth) {
			try {
				Log.d("ServerThread","Waiting for TCP connection on " + port);
				socket = serverSocket.accept();
				Log.d("ServerThread","socket acquired");
			} catch (IOException e) {
				Log.e("ServerThread","problem accepting connection",e);
			}
			if (socket!=null)
				manageConnectedSocket(socket);
			else Log.d("ServerThread","socket destroyed");
		}
		else {
			BluetoothSocket bsocket = null;
	        
	        while (true) {
	            try {
	            	Log.d("dumb","waaitng");
	            	if (bluetoothServerSocket!=null)
	                bsocket = bluetoothServerSocket.accept();
	            } catch (IOException e) {
	            	Log.e("dumb","Connection closed on server thread"+ " -- -"+e.getMessage(),e);
	                break;
	            }
	            Log.d("dumb","Connection Accepted");
	            // If a connection was accepted
	            if (bsocket != null) {
	                // manage 
	                BluetoothActivity.manageConnectedSocket(bsocket,false);
	                try {
						bluetoothServerSocket.close();
						bluetoothServerSocket = null;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                break;
	            }
	        }
		}
	}
	private void manageConnectedSocket(Socket socket) {
		Log.d("ServerThread","trying to start connection thread");
		if (connectionThread!=null) connectionThread.cancel();
		connectionThread = new ConnectionThread(socket);
		connectionThread.start();
		Log.d("ServerThread","Finished work on server thread");
	}
	public void cancel() {
		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {
				Log.e("ServerThread","Error closing server",e);
			}
		if (bluetoothServerSocket!=null)
			try {
	            bluetoothServerSocket.close();
	            Log.d("dumb","Server Closed");
	        } catch (IOException e) { }
	}
}
