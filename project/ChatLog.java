package client;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ChatLog {
	private List<String> messageList = new ArrayList<String>();
	private File file;
	
	public void log(String message) {
		messageList.add(message);
	}
	
	public void clearLog(String message) {
		messageList.clear();
	}
	
	public boolean exportLog(){
		file = new File("log1.txt");
		int i = 1;
		while (file.exists()) {
			//Checks if a log file exists and changes the name of the new log file if so to avoid overwriting the old one
			i++;								
			file = new File("log" + i + ".txt");
			
		}
		
		try {	
			//Writes each message to the new log file
			FileWriter writer = new FileWriter("log" + i + ".txt");	
			for (String message : messageList) {
				writer.write(message + "\n");
			}
			writer.close();
		} catch(Exception e) {
				e.printStackTrace();
				return false;
		}
		return true;
	}
}