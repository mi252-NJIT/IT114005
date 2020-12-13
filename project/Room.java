
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
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";
	
	
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
	
	protected synchronized List<String> getMuteLists() {
		List<String> muteLists = new ArrayList<String>();
		String muteList;
		for (ServerThread client : clients) {
			muteList = client.getMuteList();
			muteLists.add(muteList);
		}
		return muteLists;
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
				String commandOutput = "";
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
						commandOutput = flip(client);
						sendCommandResult(client, commandOutput);	
						wasCommand = true;
						break;
					case ROLL:
						commandOutput = roll(comm2[1], client);
						sendCommandResult(client, commandOutput);	
						wasCommand = true;
						break;
					case MUTE:
						boolean muted = client.mute(comm2[1]);
						if (muted) {
							sendCommandResult(client, "@" + comm2[1] + " " + client.getClientName() + " muted you.");
							sendCommandResult(client, "@" + client.getClientName() + " muted " + comm2[1]);
						} else {
							sendCommandResult(client, "@" + client.getClientName() + " unable to mute " + comm2[1] +": This user is already muted.");
						}
						wasCommand = true;
						break;
					case UNMUTE:
						boolean unmuted = client.unmute(comm2[1]);
						if (unmuted) {
							sendCommandResult(client, "@" + comm2[1] + " " + client.getClientName() + " unmuted you.");
							sendCommandResult(client, "@" + client.getClientName() + " unmuted " + comm2[1]);
						} else {
							sendCommandResult(client, "@" + client.getClientName() + " unable to unmute " + comm2[1] +": This user is not muted.");
						}
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
    
    protected String roll(String dice, ServerThread client) {
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
    	return outputMessage;
    }
    
    protected String flip(ServerThread client) {
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
    			return "";
    	}
    	log.log(Level.INFO, "flipped coin for " + client.getClientName());
    	return client.getClientName() + " flips a coin: " + flipStringResult;
    	
    }
    
    protected void sendCommandResult(ServerThread sender, String message ) {
    	//TODO reduce redundant code by finding a way to merge this method into the sendMessage method
    	
    	/* Unlike sendMessage(), this method does not send a private message back to the sender.
    	 * This is because in some cases such as the mute command, the sender and the target of
    	 * the command will need to see different messages.
    	 * */
    	
		Iterator<ServerThread> iter = clients.iterator();
		
		boolean isDM = false;
		String recipient = "";
		try {
			if ( message.substring(0, 1).equals("@")) {									//Checks if the message is a private message
				int messageStart = message.indexOf(" ");
				isDM = true;
				recipient = message.substring(1, messageStart);
				message = message.substring(messageStart);
			}
		}
		catch (Exception e) {
			log.log(Level.INFO,"Invalid DM");
			return;
		}
		
		while (iter.hasNext()) {
			ServerThread client = iter.next();
		    if (!client.isMuted(sender.getClientName()) 							//Send message if the current client is not muted  																
			    && (																//AND at least one of the following is true:
			    		!isDM 														//The message is not a private message
			    		|| client.getClientName().equals(recipient) 				//The current client is the intended recipient of the message
			    	)																
			    ) {
			    boolean messageSent = client.sendCommandOutput(sender.getClientName(), message);
			    if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
			    }
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
		boolean isDM = false;
		String recipient = "";
		try {
			if ( message.substring(0, 1).equals("@")) {									//Checks if the message is a private message
				int messageStart = message.indexOf(" ");
				isDM = true;
				recipient = message.substring(1, messageStart);
				message = " (DM@" + recipient + "):" + message.substring(messageStart);
			}
		}
		catch (Exception e) {
			log.log(Level.INFO,"Invalid DM");
			return;
		}
		
		message = processMessageTags(message);
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
		    ServerThread client = iter.next();
		    if (!client.isMuted(sender.getClientName()) 							//Send message if the current client is not muted  																
		    	&& (																//AND at least one of the following is true:
		    			!isDM 														//The message is not a private message
		    			|| client.getClientName().equals(recipient) 				//The current client is the intended recipient of the message
		    			|| sender.getClientName().equals(client.getClientName())	//The current client is the sender
		    		)
		    	) {
		    	boolean messageSent = client.send(sender.getClientName(), message);
			    if (!messageSent) {
					iter.remove();
					log.log(Level.INFO,"Removed client " + client.getId());
			    }
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
					i++;
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

