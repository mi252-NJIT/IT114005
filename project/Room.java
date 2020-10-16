import java.util.ArrayList; 
import java.util.Iterator;
import java.util.List;

import utils.Debug;

public class Room implements AutoCloseable {
	private static SocketServer server;
	private String name;
	
	//commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	
	public Room(String name) {
		this.name = name;
	}
	
	public static void setServer(SocketServer server) {
		Room.server = server;
	}
	
	public String getName() {
		return name;
	}
	
	private List<ServerThread> clients = new ArrayList<ServerThread>();
	
	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			Debug.log("Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				sendMessage(client, "joined the room " + getName());
			}
		}
	}
	
	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			sendMessage(client, "left the room");
		}
		else {
			cleanupEmptyRoom();
		}
	}
	
	private void cleanupEmptyRoom() {
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			Debug.log("closing empty room: " + name);
			close();
		}
		catch (Exception e) {
			//TODO auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room,  client);
	}
	
	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}
	
    private boolean processCommands(String message, ServerThread client) {
		boolean wasCommand = false;
		try {
		    if (message.indexOf(COMMAND_TRIGGER) > -1) {
			String[] comm = message.split(COMMAND_TRIGGER);
			Debug.log(message);
			String part1 = comm[1];
			String[] comm2 = part1.split(" ");
			String command = comm2[0];
			if (command != null) {
			    command = command.toLowerCase();
			}
			String roomName;
			switch (command) {
			case CREATE_ROOM:
			    roomName = comm2[1];
			    if (server.createNewRoom(roomName)) {
				joinRoom(roomName, client);
			    }
			    wasCommand = true;
			    break;
			case JOIN_ROOM:
			    roomName = comm2[1];
			    joinRoom(roomName, client);
			    wasCommand = true;
			    break;
			}
		    }
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
		return wasCommand;
    }
    
    protected void sendConnectionStatus(String clientName, boolean isConnect) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread client = iter.next();
		    boolean messageSent = client.sendConnectionStatus(clientName, isConnect);
		    if (!messageSent) {
			iter.remove();
			Debug.log("Removed client " + client.getId());
		    }
		}
    }
	
    protected void sendMessage(ServerThread sender, String message) {
		Debug.log(getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
		    // it was a command, don't broadcast
		    return;
		}
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread client = iter.next();
		    boolean messageSent = client.send(sender.getClientName(), message);
		    if (!messageSent) {
			iter.remove();
			Debug.log("Removed client " + client.getId());
		    }
		}
    }
	
    @Override
    public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
		    Debug.log("Migrating " + clients.size() + " to Lobby");
		    Iterator<ServerThread> iter = clients.iterator();
		    Room lobby = server.getLobby();
		    while (iter.hasNext()) {
			ServerThread client = iter.next();
			lobby.addClient(client);
			iter.remove();
		    }
		    Debug.log("Done Migrating " + clientCount + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
    }
}

