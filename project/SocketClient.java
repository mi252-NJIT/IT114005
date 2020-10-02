import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class SocketClient {
	Socket server;
	public SocketClient() {
		
	}
	
	public void connect(String address, int port) {
		try {
			server = new Socket(address, port);			
		} 
		catch (UnknownHostException e) {
			e.printStackTrace();
			
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void start() throws IOException {
		if(server == null) {
			return;
		}
		System.out.println("Listening for input");
		try(Scanner si = new Scanner(System.in)) {
			PrintWriter out = new PrintWriter(server.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
			
			String line = "";
			while(true) {
				try {
					System.out.println("Waiting for input");
					line = si.nextLine();
					if(!"quit".equalsIgnoreCase(line)) {
						out.println(line);
					} 
					else {
						break;
					}
					line = "";
					String fromServer = in.readLine();
					
					if(fromServer != null) {
						System.out.println("Reply from server: " + fromServer);
					}
					else {
						System.out.println("Server disconnected");
						break;
					}
				} 
				catch(Exception e) {
					System.out.println("Connection dropped");
					break;
				}
			}
			System.out.println("Exited loop");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			close();
		}
	}
	private void close() {
		if (server != null) {
			try {
				server.close();
				System.out.println("Closed socket");
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
			System.out.println("Invalid port");
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