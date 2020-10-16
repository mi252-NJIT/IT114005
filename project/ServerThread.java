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
	private String clientName;
	
	public String getClientName() {
		return clientName;
	}
	
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
	
	@Deprecated
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
	
	//New version of send:
	protected boolean send(String clientName, String message) {
		Payload payload = new Payload();
		payload.setPayloadType(PayloadType.MESSAGE);
		payload.setClientName(clientName);
		payload.setMessage(message);
		return sendPayload(payload);
	}
	
    protected boolean sendConnectionStatus(String clientName, boolean isConnect) {
		Payload payload = new Payload();
		if (isConnect) {
		    payload.setPayloadType(PayloadType.CONNECT);
		}
		else {
		    payload.setPayloadType(PayloadType.DISCONNECT);
		}
		payload.setClientName(clientName);
		return sendPayload(payload);
    }
	
	private boolean sendPayload(Payload p) {
		try {
			out.writeObject(p);
			return true;
		}
		catch (IOException e) {
			Debug.log("Error sending message to client (most likely disconnected)");
			e.printStackTrace();
			cleanup();
			return false;
		}
	}
	
    private void processPayload(Payload p) {
		switch (p.getPayloadType()) {
		case CONNECT:
		    // here we'll fetch a clientName from our client
		    String n = p.getClientName();
		    if (n != null) {
				clientName = n;
				Debug.log("Set our name to " + clientName);
				if (currentRoom != null) {
				    currentRoom.joinLobby(this);
				}
		    }
		    break;
		case DISCONNECT:
		    isRunning = false;// this will break the while loop in run() and clean everything up
		    break;
		case MESSAGE:
		    currentRoom.sendMessage(this, p.getMessage());
		    break;
		default:
		    Debug.log("Unhandled payload on server: " + p);
		    break;
		}
    }
	
    @Override
    public void run() {
		try {
		    isRunning = true;
		    Payload fromClient;
		    while (isRunning &&
			    !client.isClosed() // breaks the loop if our connection closes
			    && (fromClient = (Payload) in.readObject()) != null) {
					System.out.println("Received from client: " + fromClient);
					processPayload(fromClient);
		    } // end of loop
		}
		catch (Exception e) {
		    // happens when client disconnects
		    e.printStackTrace();
		    Debug.log("Client Disconnected");
		}
		finally {
		    isRunning = false;
		    Debug.log("Cleaning up connection for ServerThread");
		    cleanup();
		}
    }
	
    private void cleanup() {
    	if (currentRoom != null) {
    	    Debug.log(getName() + " removing self from room " + currentRoom.getName());
    	    currentRoom.removeClient(this);
    	}
    	if (in != null) {
    	    try {
    		in.close();
    	    }
    	    catch (IOException e) {
    		Debug.log("Input already closed");
    	    }
    	}
    	if (out != null) {
    	    try {
    		out.close();
    	    }
    	    catch (IOException e) {
    		Debug.log("Client already closed");
    	    }
    	}
    	if (client != null && !client.isClosed()) {
    	    try {
    		client.shutdownInput();
    	    }
    	    catch (IOException e) {
    		Debug.log("Socket/Input already closed");
    	    }
    	    try {
    		client.shutdownOutput();
    	    }
    	    catch (IOException e) {
    		Debug.log("Socket/Output already closed");
    	    }
    	    try {
    		client.close();
    	    }
    	    catch (IOException e) {
    		Debug.log("Client already closed");
    	    }
    	}
        }
}