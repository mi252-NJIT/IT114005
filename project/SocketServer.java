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
	private List<Room> isolatedPrelobbies = new ArrayList<Room>();
	private final static String PRELOBBY = "PreLobby";
	protected final static String LOBBY = "LOBBY";
	
	
	private void start(int port) {
		this.port = port;
		Debug.log("Waiting for client");
		
		try (ServerSocket serverSocket = new ServerSocket(port);) {
			isRunning = true;
			Room.setServer(this);
			lobby = new Room(LOBBY); 	//Creates lobby room
			rooms.add(lobby);
			while (SocketServer.isRunning) {
				try {
					Socket client = serverSocket.accept();
					Debug.log("Client connecting...");
					ServerThread thread = new ServerThread(client, lobby);
					thread.start();
					Room prelobby = new Room(PRELOBBY);
					prelobby.addClient(thread);
					isolatedPrelobbies.add(prelobby);
					
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
				cleanup();
				Debug.log("closing server socket");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
	}
	
	protected void cleanupRoom(Room r) {
		isolatedPrelobbies.remove(r);
	}
	
	private void cleanup() {
		Iterator<Room> rooms = this.rooms.iterator();
		while (rooms.hasNext()) {
			Room r = rooms.next();
			try {
				r.close();
			}
			catch (Exception e) {
				//pass
			}
		}
		Iterator<Room> pl = isolatedPrelobbies.iterator();
		while (pl.hasNext()) {
		    Room r = pl.next();
		    try {
			r.close();
		    }
		    catch (Exception e) {
			//pass
		    }
		}
		try {
			lobby.close();

		}
		catch (Exception e) {
			//pass
		}
	}
	
	protected Room getLobby() {
		return lobby;
	}
	
	protected void joinLobby(ServerThread client) {
		Room prelobby = client.getCurrentRoom();
		if (joinRoom(LOBBY, client)) {
			prelobby.removeClient(client);
			Debug.log("Added " + client.getClientName() + " to Lobby; Prelobby should self destruct");
		}
		else {
			Debug.log("Problem moving " + client.getClientName() + " to lobby");
		}
	}
	
    private Room getRoom(String roomName) {
		for (int i = 0, l = rooms.size(); i < l; i++) {
		    Room r = rooms.get(i);
		    if (r == null || r.getName() == null) {
		    	continue;
		    }
		    if (r.getName().equalsIgnoreCase(roomName)) {
		    	return r;
		    }
		}
		return null;
    }
	
    protected synchronized boolean joinRoom(String roomName, ServerThread client) {
		if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)) {
		    return false;
		}
		Room newRoom = getRoom(roomName);
		Room oldRoom = client.getCurrentRoom();
		if (newRoom != null) {
		    if (oldRoom != null) {
		    	Debug.log(client.getClientName() + " leaving room " + oldRoom.getName());
		    	oldRoom.removeClient(client);
		    }
		    	Debug.log(client.getClientName() + " joining room " + newRoom.getName());
		    	newRoom.addClient(client);
		    	return true;
		}
		return false;
    }
	

    protected synchronized boolean createNewRoom(String roomName) {
		if (roomName == null || roomName.equalsIgnoreCase(PRELOBBY)) {
		    return false;
		}
		if (getRoom(roomName) != null) {
		    // TODO can't create room
		    Debug.log("Room already exists");
		    return false;
		}
		else {
		    Room room = new Room(roomName);// , this);
		    rooms.add(room);
		    Debug.log("Created new room: " + roomName);
		    return true;
		}
    }
	
    public static void main(String[] args) {
	int port = -1;
	try {
	    port = Integer.parseInt(args[0]);
	}
	catch (Exception e) {
		Debug.log("Input Error");
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