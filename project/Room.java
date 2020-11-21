
package server;
import java.util.ArrayList;  
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;


public class Room implements AutoCloseable {
	private final static Logger log = Logger.getLogger(Room.class.getName());
	private static SocketServer server;
	private String name;
	private Random rng = new Random();
	
	//For text processing:
	String[] characters = {"~", "\\*", "-"};
	String[] tags = {"i", "b", "strike"};
	
	//commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String FLIP = "flip";
	private final static String ROLL = "roll";
	private final static String RED = "red";
	private final static String GREEN = "green";
	private final static String BLUE = "blue";
	
	
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
			log.log(Level.INFO,"Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
		}
	}
	
    private void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread c = iter.next();
		    if (c != client) {
			boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
		    }
		}
    }
	
	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			sendConnectionStatus(client, false, "left the room " + getName());
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
			log.log(Level.INFO,"closing empty room: " + name);
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
				log.log(Level.INFO,message);
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
					case FLIP:
						flip(client);
						wasCommand = true;
						break;
					case ROLL:
						roll(comm2[1], client);
						wasCommand = true;
						break;
					case GREEN:
					case BLUE:
					case RED:
						message = colorText(command, message);
						sendMessage(client, message);
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
    
    protected String colorText(String command, String message) {
    	return message.replace("/" + command, "<span color=" + command + ">");
    }
    
    protected void roll(String dice, ServerThread client) {
    	String outputMessage = "";
    	try {
	    	String[] args;
	    	args = dice.split("[d+]");
	    	
	    	int dieCount = Integer.parseInt(args[0]);
	    	int dieSize = Integer.parseInt(args[1]);
	    	int modifier = 0;
	    	if (args.length == 3) {
	    		modifier = Integer.parseInt(args[2]);
	    	}
	    	int result = 0;
	    	for (int i = 0; i < dieCount; i++) {
	    		result += rng.nextInt(dieSize) + 1;
	    	}
	    	result += modifier;
	    	outputMessage = client.getClientName() + " rolls " + dice + ": " + result;
    	}
    	catch (IllegalArgumentException e) {
    		outputMessage = "Invalid roll syntax";
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    broadcastCommandResult(client, outputMessage);	
    }
    
    protected void flip(ServerThread client) {
    	int flipIntResult = rng.nextInt(2);
    	String flipStringResult = "";
    	switch (flipIntResult) {
    		case 0:
    			flipStringResult = "Tails!";
    			break;
    		case 1:
    			flipStringResult = "Heads!";
    			break;
    		default:
    			log.log(Level.INFO, "Error calculating flip result: No cases matched");
    			return;
    	}
    	log.log(Level.INFO, "flipped coin for " + client.getClientName());
    	broadcastCommandResult(client, client.getClientName() + " flips a coin: " + flipStringResult);
    	
    }
    
    protected void broadcastCommandResult(ServerThread client, String message ) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread c = iter.next();
		    boolean messageSent = c.sendCommandOutput(client.getClientName(), message);
		    if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
		    }
		}
    }
    
    protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread c = iter.next();
		    boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
		    if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
		    }
		}
    }
	
    protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO,getName() + ": Sending message to " + clients.size() + " clients");
		if (processCommands(message, sender)) {
		    // it was a command, don't broadcast
		    return;
		}
		message = processMessageTags(message);
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread client = iter.next();
		    boolean messageSent = client.send(sender.getClientName(), message);
		    if (!messageSent) {
			iter.remove();
			log.log(Level.INFO,"Removed client " + client.getId());
		    }
		}
    }
    
    protected String processMessageTags(String message) {
		int i = 0;
		String currentCharacter;
		String currentTag;
		int arrayLength = characters.length;
		
		for (int j = 0; j < arrayLength; j++ ) {
			currentCharacter = characters[j];
			currentTag = tags[j];
			while (message.indexOf(currentCharacter.substring(currentCharacter.length() - 1)) != -1) {
				if (i == 0) {
					message = message.replaceFirst(currentCharacter, "<" + currentTag + ">");			//replaces evenly numbered occurrences of a character with an html tag
				} else if (i == 1) {
					message = message.replaceFirst(currentCharacter, "</" + currentTag + ">");			//replaces oddly numbered occurrences of a character with an html close tag
					i = 0;
				}
			}
		}
		return message;
    }
    
    public List<String> getRooms() {
		return server.getRooms();
    }
	
    @Override
    public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
		    log.log(Level.INFO,"Migrating " + clients.size() + " to Lobby");
		    Iterator<ServerThread> iter = clients.iterator();
		    Room lobby = server.getLobby();
		    while (iter.hasNext()) {
			ServerThread client = iter.next();
			lobby.addClient(client);
			iter.remove();
		    }
		    log.log(Level.INFO,"Done Migrating " + clientCount + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
    }
}

