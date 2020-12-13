package server;
import java.io.IOException;    
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class SocketServer {
	private final static Logger log = Logger.getLogger(SocketServer.class.getName());
	int port = 3099;
	public static boolean isRunning = false;
	private List<Room> rooms = new ArrayList<Room>();
	private Room lobby;
	private List<Room> isolatedPrelobbies = new ArrayList<Room>();
	private final static String PRELOBBY = "PreLobby";
	protected final static String LOBBY = "LOBBY";
	
	
	private void start(int port) {
		this.port = port;
		log.log(Level.INFO,"Waiting for client");
		
		try (ServerSocket serverSocket = new ServerSocket(port);) {
			isRunning = true;
			Room.setServer(this);
			lobby = new Room(LOBBY); 	//Creates lobby room
			rooms.add(lobby);
			while (SocketServer.isRunning) {
				try {
					Socket client = serverSocket.accept();
					log.log(Level.INFO,"Client connecting...");
					ServerThread thread = new ServerThread(client, lobby);
					thread.start();
					Room prelobby = new Room(PRELOBBY);
					prelobby.addClient(thread);
					isolatedPrelobbies.add(prelobby);
					log.log(Level.INFO,"client added to clients pool");
					//TODO Make it so the mute list saves every 10 minutes or at server shutdown instead of every time a client joins
					saveMuteLists();
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
				log.log(Level.INFO,"closing server socket");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
	}
	
	private synchronized void saveMuteLists() {
		try {
			String output = "";
			String[] currentMuteListArray;
			String currentMuteList;	
			List<String> currentRoomMuteLists;
			//Gets each user's mute list from each room, then writes them all on a different line to a file called MuteLists.txt
			for (Room room : rooms) {
				currentRoomMuteLists = room.getMuteLists();
				for (String muteList : currentRoomMuteLists) {
					output += muteList + "\n";
					
				}
				File muteLists = new File("MuteLists.txt");
				Scanner scanner = new Scanner(muteLists);
				//While loop goes through each entry that was already in the MuteLists.txt file and saves it to write back in if there isn't a new entry to replace it
				while (scanner.hasNext()) {
					currentMuteList = scanner.nextLine();
					currentMuteListArray = currentMuteList.split(": ");
					if (output.indexOf(currentMuteListArray[0]) == -1) {
						output += currentMuteList + "\n";
					}
				}
				
				
				FileWriter writer = new FileWriter(muteLists);
				writer.write(output);
				writer.close();
			}
				
			} catch (Exception e) {
				e.printStackTrace();

		}
	}
	
	protected synchronized String[] loadMuteList(String clientName) throws FileNotFoundException {
		File muteLists = new File("MuteLists.txt");
		//currentMuteListArray will contain the client's name in index 0 and the name of their muted clients in index 1
		String[] currentMuteListArray;
		String currentMuteList;	
		String[] output = new String[0];
		Scanner scanner = new Scanner(muteLists);
		while (scanner.hasNext()) {
			currentMuteList = scanner.nextLine();
			currentMuteListArray = currentMuteList.split(": ");
			System.out.println(currentMuteListArray[0] + " " + clientName);
			if (currentMuteListArray[0].equals(clientName)) {
				output = currentMuteListArray[1].split(", ");
				System.out.println(output.toString());
			}
		}
		scanner.close();
		return output;
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
	
    protected List<String> getRooms() {
		// not the most efficient way to do it, but it works
		List<String> roomNames = new ArrayList<String>();
		Iterator<Room> iter = rooms.iterator();
		while (iter.hasNext()) {
		    Room r = iter.next();
		    if (r != null && r.getName() != null) {
				roomNames.add(r.getName());
		    }
		}
		return roomNames;
    }
	
	protected void joinLobby(ServerThread client) {
		Room prelobby = client.getCurrentRoom();
		try {
			//Tries to get the user's saved mute list if it exists
			String[] muteList = loadMuteList(client.getClientName());
			for (String clientName : muteList) {
				client.mute(clientName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (joinRoom(LOBBY, client)) {
			prelobby.removeClient(client);
			log.log(Level.INFO,"Added " + client.getClientName() + " to Lobby; Prelobby should self destruct");
		}
		else {
			log.log(Level.INFO,"Problem moving " + client.getClientName() + " to lobby");
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
		    	log.log(Level.INFO,client.getClientName() + " leaving room " + oldRoom.getName());
		    	oldRoom.removeClient(client);
		    }
		    	log.log(Level.INFO,client.getClientName() + " joining room " + newRoom.getName());
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
		    log.log(Level.INFO,"Room already exists");
		    return false;
		}
		else {
		    Room room = new Room(roomName);// , this);
		    rooms.add(room);
		    log.log(Level.INFO,"Created new room: " + roomName);
		    return true;
		}
    }
	
    public static void main(String[] args) {
	int port = -1;
	try {
	    port = Integer.parseInt(args[0]);
	}
	catch (Exception e) {
		log.log(Level.INFO,"Input Error");
	}
	if (port > -1) {
	    log.log(Level.INFO,"Starting Server");
	    SocketServer server = new SocketServer();
	    log.log(Level.INFO,"Listening on port " + port);
	    server.start(port);
	    log.log(Level.INFO,"Server Stopped");
	}
    }
}