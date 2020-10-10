import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import utils.Debug;

public class SocketServer {
	int port = 3099;
	public static boolean isRunning = false;
	private List<Room> rooms = new ArrayList<Room>();
	private Room lobby;
	
	private void start(int port) {

		this.port = port;
		Debug.log("Waiting for client");
		
		try (ServerSocket serverSocket = new ServerSocket(port);) {
			isRunning = true;
			lobby = new Room("Lobby", this); 	//Creates lobby room
			rooms.add(lobby);
			while (SocketServer.isRunning) {
				try {
					Socket client = serverSocket.accept();
					Debug.log("Client connecting...");
					ServerThread thread = new ServerThread(client, lobby);
					thread.start();
					lobby.addClient(thread);
					Debug.log("client added to clients pool");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				isRunning = false;
				Debug.log("closing server socket");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
	}
	
	protected Room getLobby() {
		return lobby;
	}
	
	private Room getRoom(String roomName) {
		for (int i = 0, l = rooms.size(); i< l; i++) {
			if (rooms.get(i).getName().equalsIgnoreCase(roomName)) {
				return rooms.get(i);
			}
		}
		return null;
	}
	
	protected synchronized boolean joinRoom(String roomName, ServerThread client) {
		Room newRoom = getRoom(roomName);
		Room oldRoom = client.getCurrentRoom();
		if (newRoom != null) {
			if (oldRoom != null) {
				Debug.log(client.getName() + " leaving room " + oldRoom.getName());
				oldRoom.removeClient(client);
			}
			Debug.log(client.getName() + " joining room " + newRoom.getName());
			newRoom.addClient(client);
			return true;
		}
		return false;
	}
	

	protected synchronized boolean createNewRoom(String roomName) {
		if (getRoom(roomName) != null) {
			//TODO can't create room
			Debug.log("Room already exists");
			return false;
		} else {
			Room room = new Room(roomName, this);
			rooms.add(room);
			Debug.log("Created new room: " + roomName);
			return true;
		}
	}
	
	public static void main(String[] args) {
		int port = -1;
		
		if (args.length >= 1) {
			String arg = args[0];
			try {
				port = Integer.parseInt(arg);
			} catch(Exception e) {
				Debug.log("input error");
			}
		}
		
		if (port > -1) {
			Debug.log("Starting Server");
			SocketServer server = new SocketServer();
			Debug.log("Listening on port " + port);
			server.start(port);
			Debug.log("Server Stopped");
		}
	}
}