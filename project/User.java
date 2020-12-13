package client;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class User extends JPanel {
	private String name;
	private JTextField nameField;
	
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
	
	public void highlight() {
		nameField.setEditable(true);
		nameField.setBackground(new Color(0, 20, 0));
		nameField.setEditable(false);
	}
	
	public void unhighlight() {
		nameField.setEditable(true);
		nameField.setBackground(Color.white);
		nameField.setEditable(false);
	}
}