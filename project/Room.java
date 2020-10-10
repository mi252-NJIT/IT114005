import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import utils.Debug;

public class Room {
	private SocketServer server;
	private String name;
	
	public Room(String name, SocketServer server) {
		this.name = name;
		this.server = server;
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
			sendMessage(client, "joined the room " + getName());
		}
	}
	
	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		sendMessage(client, "left the room");
	}
	
	private boolean processCommands(String message, ServerThread client) {
		boolean wasCommand = false;
		try {
			if (message.indexOf("/") > -1) {
				String[] comm = message.split("/");
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				String roomName;
				switch (command) {
				case "createroom":
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						server.joinRoom(roomName, client);
					}
					wasCommand = true;
					break;
				
				case "joinroom":
					roomName = comm2[1];
					server.joinRoom(roomName, client);
					wasCommand = true;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return wasCommand;
	}
	
	protected void sendMessage(ServerThread sender, String message) {
		Debug.log(getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
			return;
		}
		Iterator<ServerThread> iter = clients.iterator();
		message = String.format("User[%s]: %s", sender.getName(), message);
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			boolean messageSent = client.send(message);
			if (!messageSent) {
				iter.remove();
				Debug.log("Removed client " + client.getId());
			}
		}
	}
	
	
}

