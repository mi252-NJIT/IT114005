
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import utils.Debug;

public class SocketClient {
	private Socket server;
	private Thread inputThread;
	private Thread fromServerThread;
	
	public void connect(String address, int port) {
		try {
			server = new Socket(address, port);
			Debug.log("Client Connected");
		} 
		catch (UnknownHostException e) {
			e.printStackTrace();
			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void listenForKeyboard(Scanner si, ObjectOutputStream out) {
		inputThread = new Thread() {
			@Override
			public void run() {
				try {
					while (!server.isClosed()) {
						Debug.log("Waiting for input");
						String line = si.nextLine();
						
						if (!"quit".equalsIgnoreCase(line) && line != null) {
							out.writeObject(line);
						} else {
							Debug.log("Stopping input thread");
							out.writeObject("bye");
							break;
						}
						try {
							sleep(50);
						} catch (Exception e) {
							Debug.log("Problem sleeping thread");
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					close();
					Debug.log("Stopped listening to console input");
				}
			}
			
		};
		inputThread.start();
	}
	
	
	private void listenForServerMessage(ObjectInputStream in) {
		fromServerThread = new Thread() {
			@Override
			public void run() {
				try {
					String fromServer;
					while (!server.isClosed() && (fromServer = (String) in.readObject()) != null) {
						System.out.println(fromServer);
					}
				} catch (Exception e) {
					if (!server.isClosed()) {
						e.printStackTrace();
						Debug.log("Connection closed");
					} else {
						Debug.log("Connection closed");
					}
				} finally {
					close();
					Debug.log("Stopped Listening to server input");
				}
			}
		};
		fromServerThread.start();
	}
	
	
	public void start() throws IOException {
		if(server == null) {
			return;
		}
		Debug.log("Client Started");
		try(Scanner si = new Scanner(System.in)) {
			ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(server.getInputStream());
			
			listenForKeyboard(si, out);
			
			listenForServerMessage(in);
			
			while (!server.isClosed()) {
				Thread.sleep(50);
			}
			Debug.log("Exited loop");
			Debug.log("Press enter to stop the program");

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}
	
	
	private void close() {
		if (server != null) {
			try {
				server.close();
				Debug.log("Closed socket");;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		SocketClient client = new SocketClient();
		int port = -1;
		
		try {
			port = Integer.parseInt(args[0]);
		}
		catch (Exception e) {
			Debug.log("Invalid port");
		}
		
		if(port == -1) {
			return;
		}
		
		client.connect("127.0.0.1", port);
		try {
			client.start();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}