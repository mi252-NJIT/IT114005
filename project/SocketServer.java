import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SocketServer {
	int port = 51111;
	private void start(int port) {
		this.port = port;
		System.out.println("Waiting for client");
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			Socket client = serverSocket.accept();
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			
			System.out.println("Client connected, waiting for message");
			String fromClient = "";
			String toClient = "";
			
			while ((fromClient = in.readLine()) != null) {
				System.out.println("From client: " + fromClient);
				List<String> reversedInput = Arrays.asList(fromClient.split(""));
				Collections.reverse(reversedInput);
				toClient = String.join("",  reversedInput);
				
				
				if ("kill server".equalsIgnoreCase(fromClient)) {
					System.out.println("Client killed server");
					break;
				}
				else {
					out.println(toClient);
				}
				
				
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				System.out.println("closing server socket");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
				
	}
	
	public static void main(String[] args) {
		System.out.println("Starting Server");
		SocketServer server = new SocketServer();
		int port = -1;
		
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			}
			catch(Exception e) {
				System.out.println("Invalid port: " + args[0]);
			}
		}
		
		if (port > -1) {
			System.out.println("Server listening on port " + port);
			server.start(port);
		}
		
		System.out.println("Server Stopped");
	}
}