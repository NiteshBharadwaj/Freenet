package freenet.darknetconnector.DarknetAppConnector;
/**
 * A static framework to hold properties of home node assuming each mobile 
 * can be associated with only one home freenet node at a time. 
 * @author Illutionist
 */
public class HomeNode {
	
	private static String name;
	private static String pin;
	private static int port;
	private static String ip;
	private static String tempName;
	private static int tempPort;
	private static String tempIP;
	private static String tempPin;

	public static void setIP(String IP) {
		ip = IP;
	}
	public static void setName(String nodeName) {
		name = nodeName;
	}
	public static void setPin(String pinNew) {
		pin = pinNew;
	}
	public static void setPort(int newPort) {
		port = newPort;
	}
	public static String getName() {
		return name;
	}
	public static String getPin() {
		return pin;
	}
	public static int getPort() {
		return port;
	}
	public static String getIP() {
		return ip;
	}
	
	public static void setTemp(String nam, int portT, String Ip, String pinT) {
		tempName = nam;
		tempPort = portT;
		tempIP = Ip;
		tempPin = pinT;
	}
	public static boolean check(String nam, int portT, String Ip, String pinT) {
		return nam.equals(name) && portT==port && ip.equals(Ip) && pinT.equals(pin);
	}
	public static boolean check(String nam){
		return nam.equals(name);
	}
	public static boolean check(String nam, String pinT) {
		return nam.equals(name) && pin.equals(pinT);
	}
	public static void finalizeHome() {
		name = tempName;
		port = tempPort;
		pin = tempPin;
		ip = tempIP;
	}
}
