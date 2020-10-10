import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import utils.Debug;

public class ServerThread extends Thread {
	private Socket client;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private boolean isRunning = false;
	private Room currentRoom;
	
	protected synchronized Room getCurrentRoom() {
		return currentRoom;
	}
	
	protected synchronized void setCurrentRoom(Room room) {
		if (room != null) {
			currentRoom = room;
		} else {
			Debug.log("Passed in room was null, this shouldn't happen");
		}
	}
	
	public ServerThread(Socket myClient, Room room) throws IOException {
		this.client = myClient;
		this.currentRoom = room;
		out = new ObjectOutputStream(client.getOutputStream());
		in = new ObjectInputStream(client.getInputStream());
	}
	
	public boolean send(String message) {
		try {
			out.writeObject(message);
			return true;
		} catch (IOException e ) {
			Debug.log("Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}
	
	@Override
	public void run( ) {
		try {
			isRunning = true;
			String fromClient;
			while (isRunning &&
					!client.isClosed() 
					&& (fromClient = (String) in.readObject())!= null) {
				System.out.println("Received from client: " + fromClient);
				currentRoom.sendMessage(this, fromClient);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Debug.log("Client Disconnected");
		} finally {
			isRunning = false;
			Debug.log("Cleaning up connection for ServerThread");
			cleanup();
		}
	}
	
	private void cleanup( ) {
		if (currentRoom != null ) {
			Debug.log(getName() + " removing self from room " + currentRoom.getName());
			currentRoom.removeClient(this);
		}
		if (in != null ) {
			try {
				in.close();
			} catch (IOException e) {
				Debug.log("Input already closed");
			}
		}
		if (out != null ) {
			try {
				out.close();
			} catch (IOException e) {
				Debug.log("Client already closed");
			}
		}
		if (client != null && !client.isClosed()) {
			try {
				client.shutdownInput();
			} catch (IOException e) {
				Debug.log("Socket/Input already closed");
			}
			try {
				client.shutdownOutput();
			} catch (IOException e) {
				Debug.log("Client already closed");
			}
		}
	}
}