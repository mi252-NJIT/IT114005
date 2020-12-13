package client;

import utils.Countdown;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextField;

import utils.Countdown;

public class User extends JPanel {
	private String name;
	private JTextField nameField;
	private Countdown countdown;
	
	public User(String name) {
		this.name = name;
		nameField = new JTextField(name);
		nameField.setBackground(Color.white);
		nameField.setEditable(false);
		this.setLayout(new BorderLayout());
		this.add(nameField);
	}
	
	public String getName() {
		return name;
	}
	
	public void highlight(int seconds) {
		//Highlights the user in the userpanel for the given amount of seconds
		nameField.setEditable(true);
		nameField.setBackground(new Color(200, 245, 255));
		
		//If the given amount of seconds is 0 or less, leave the user highlighted until it is removed, otherwise set a countdown to unhighlight the user. 
		if (seconds > 0) {
			if (countdown != null) {
				countdown.cancel();
			}
			countdown = new Countdown("", seconds, (x) -> this.unhighlight());	
		}
		
		nameField.setEditable(false);
	}
	
	public void unhighlight() {
		nameField.setEditable(true);
		nameField.setBackground(Color.white);
		nameField.setEditable(false);
	}
}