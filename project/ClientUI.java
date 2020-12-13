
package client;

import java.awt.BorderLayout; 
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

public class ClientUI extends JFrame implements Event {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ChatLog chatLog = new ChatLog();
    CardLayout card;
    ClientUI self;
    JPanel textArea;
    JPanel userPanel;
    List<User> users = new ArrayList<User>();
    private final static Logger log = Logger.getLogger(ClientUI.class.getName());
    Dimension windowSize = Toolkit.getDefaultToolkit().getScreenSize();
    String username;
    RoomsPanel roomsPanel;
    JMenuBar menu;

    public ClientUI(String title) {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		menu = new JMenuBar();
		JMenu optionsMenu = new JMenu("Options");
		JMenu roomsMenu = new JMenu("Rooms");
		JMenuItem roomsSearch = new JMenuItem("Search");
		JMenuItem exportChatLog = new JMenuItem("Export Chat Log");
		
		roomsSearch.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
				goToPanel("rooms");
		    }
		});
		
		exportChatLog.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {;
		    	Boolean exported = chatLog.exportLog();
		    	if (exported) {
		    		addMessage("Exported Chat Log", true);
		    	} else {
		    		addMessage("Failed to Export Chat Log", true);
		    	}
		    }
		});
		
		roomsMenu.add(roomsSearch);
		optionsMenu.add(exportChatLog);
		menu.add(roomsMenu);
		menu.add(optionsMenu);
		windowSize.width *= .8;
		windowSize.height *= .8;
		setPreferredSize(windowSize);
		setSize(windowSize);// This is needed for setLocationRelativeTo()
		setLocationRelativeTo(null);
		self = this;
		setTitle(title);
		card = new CardLayout();
		setLayout(card);
		createConnectionScreen();
		createUserInputScreen();
	
		createPanelRoom();
		createPanelUserList();
		this.setJMenuBar(menu);
		// TODO remove
		createRoomsPanel();
		showUI();
    }
    void createConnectionScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel hostLabel = new JLabel("Host:");
		JTextField host = new JTextField("127.0.0.1");
		panel.add(hostLabel);
		panel.add(host);
		JLabel portLabel = new JLabel("Port:");
		JTextField port = new JTextField("3099");
		panel.add(portLabel);
		panel.add(port);
		JButton button = new JButton("Next");
		
		ActionListener onEnter = new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
				String _host = host.getText();
				String _port = port.getText();
				if (_host.length() > 0 && _port.length() > 0) {
				    try {
						connect(_host, _port);
						self.next();
				    }
				    catch (IOException e1) {
						e1.printStackTrace();
						// TODO handle error properly
						log.log(Level.SEVERE, "Error connecting");
				    }
				}
		    }
		};
		
		button.addActionListener(onEnter); 
		port.addActionListener(onEnter);
		
		panel.add(button);
		this.add(panel);
	}
	
    void createUserInputScreen() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel userLabel = new JLabel("Username:");
		JTextField username = new JTextField();
		panel.add(userLabel);
		panel.add(username);
		JButton button = new JButton("Join");
		ClientUI self = this;
		
		ActionListener onEnter = new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
				String name = username.getText();
				if (name != null && name.length() > 0) {
				    // need external ref since "this" context is the action event, not ClientUI
				    self.username = name;
				    // this order matters
				    pack();
				    self.setTitle(self.getTitle() + " - " + self.username);
				    SocketClient.setUsername(self.username);
	
				    self.next();
				}
		    }
		};
		button.addActionListener(onEnter);
		username.addActionListener(onEnter);
		
		panel.add(button);
		this.add(panel);
	    }
	
	    void createPanelRoom() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
	
		textArea = new JPanel();
		textArea.setLayout(new BoxLayout(textArea, BoxLayout.Y_AXIS));
		textArea.setAlignmentY(Component.BOTTOM_ALIGNMENT);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		// updated to be 30% of screen width
		Dimension d = new Dimension((int) (windowSize.width * .3), windowSize.height);
		scroll.setPreferredSize(d);
		scroll.setMinimumSize(d);
		scroll.setMaximumSize(d);
		
		panel.add(scroll, BorderLayout.CENTER);
	
		JPanel input = new JPanel();
		input.setLayout(new BoxLayout(input, BoxLayout.X_AXIS));
		JTextField text = new JTextField();
		input.add(text);
		JButton button = new JButton("Send");
		text.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendAction");
		text.getActionMap().put("sendAction", new AbstractAction() {
		    public void actionPerformed(ActionEvent actionEvent) {
				button.doClick();
		    }
		});
	
		button.addActionListener(new ActionListener() {
	
		    @Override
		    public void actionPerformed(ActionEvent e) {
				if (text.getText().length() > 0) {
				    SocketClient.sendMessage(text.getText());
				    text.setText("");
				}
		    }
	
		});
		input.add(button);
		panel.add(input, BorderLayout.SOUTH);
		this.add(panel, "lobby");
    }

	    void createPanelUserList() {
	    	userPanel = new JPanel();
	    	userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
	    	userPanel.setAlignmentY(Component.TOP_ALIGNMENT);

	    	JScrollPane scroll = new JScrollPane(userPanel);
	    	scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    	scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

	    	Dimension d = new Dimension(100, windowSize.height);
	    	scroll.setPreferredSize(d);

	    	textArea.getParent().getParent().getParent().add(scroll, BorderLayout.EAST);
	        }
	    
	void createRoomsPanel() {
		roomsPanel = new RoomsPanel(this);
	    this.add(roomsPanel, "rooms");
	}

    void addClient(String name) {
		User u = new User(name);
		Dimension p = new Dimension(userPanel.getSize().width, 30);
		u.setPreferredSize(p);
		u.setMinimumSize(p);
		u.setMaximumSize(p);
		userPanel.add(u);
		users.add(u);
		pack();
    }

    void removeClient(User client) {
		userPanel.remove(client);
		client.removeAll();
		userPanel.revalidate();
		userPanel.repaint();
    }

    /***
     * Attempts to calculate the necessary dimensions for a potentially wrapped
     * string of text. This isn't perfect and some extra whitespace above or below
     * the text may occur
     * 
     * @param str
     * @return
     */
    int calcHeightForText(String str) {
		FontMetrics metrics = self.getGraphics().getFontMetrics(self.getFont());
		int hgt = metrics.getHeight();
		int adv = metrics.stringWidth(str);
		final int PIXEL_PADDING = 6;
		Dimension size = new Dimension(adv, hgt + PIXEL_PADDING);
		final float PADDING_PERCENT = 1.1f;
		// calculate modifier to line wrapping so we can display the wrapped message
		int mult = (int) Math.floor(size.width / (textArea.getSize().width * PADDING_PERCENT));
		// System.out.println(mult);
		mult++;
		return size.height * mult;
    }

    void addMessage(String str, boolean isCommandOutput) {
		JEditorPane entry = new JEditorPane();
		if (isCommandOutput) {
			entry.setBackground(new Color(235, 235, 235));
			str = "<b><i>" + str + "</b></i>";
		}
		chatLog.log(str);
		entry.setEditable(false);
		entry.setContentType("text/html");
		// entry.setLayout(null);
		entry.setText(str);
		int areaWidth = textArea.getSize().width;
		Dimension d = new Dimension(areaWidth, calcHeightForText(str));
		// attempt to lock all dimensions
		entry.setMinimumSize(d);
		entry.setPreferredSize(d);
		entry.setMaximumSize(d);
		textArea.add(entry);
	
		pack();
		resizeTexts();
		// System.out.println(entry.getSize());
		JScrollBar sb = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
		sb.setValue(sb.getMaximum());
    }
    
    void resizeTexts() {
		// attempts to fix sizing of messages when text area gets resized
		int areaWidth = textArea.getSize().width;
		int cc = textArea.getComponents().length;
		if (cc > 1) {
		    // if we have more than 1 text item
		    Component test = textArea.getComponent(cc - 2);
		    // check if the test width is different than our container
		    if (areaWidth != test.getWidth()) {
				// if so try to resize all the components to be the same width
				for (Component c : textArea.getComponents()) {
				    Dimension current = c.getSize();
				    Dimension updated = new Dimension(areaWidth, current.height);
				    c.setPreferredSize(updated);
				    c.setMinimumSize(updated);
				    c.setMaximumSize(updated);
				}
		    }
		}
    }


    void next() {
		card.next(this.getContentPane());
    }

    void previous() {
		card.previous(this.getContentPane());
    }
    
    void goToPanel(String panel) {
		switch (panel) {
			case "rooms":
			    // TODO get rooms
			    roomsPanel.removeAllRooms();
			    SocketClient.INSTANCE.sendGetRooms(null);
			    break;
			default:
			    // no need to react
			    break;
		}
		card.show(this.getContentPane(), panel);
    }

    void connect(String host, String port) throws IOException {
    	SocketClient.INSTANCE.registerCallbackListener(this);
		SocketClient.INSTANCE.connectAndStart(host, port);
    }

    void showUI() {
		pack();
		Dimension lock = textArea.getSize();
		textArea.setMaximumSize(lock);
		lock = userPanel.getSize();
		userPanel.setMaximumSize(lock);
		setVisible(true);
    }


    @Override
    public void onClientConnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		addClient(clientName);
		if (message != null && !message.isBlank()) {
		    self.addMessage(String.format("%s: %s", clientName, message), false);
		}
    }

    @Override
    public void onClientDisconnect(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
		    User u = iter.next();
		    if (u.getName() == clientName) {
				removeClient(u);
				iter.remove();
				self.addMessage(String.format("%s: %s", clientName, message), false);
				break;
		    }
	
		}
    }

    @Override
    public void onMessageReceive(String clientName, String message) {
		log.log(Level.INFO, String.format("%s: %s", clientName, message));
		self.addMessage(String.format("%s: %s", clientName, message), false);
    }
    
    @Override
    public void onCommandOutputReceive(String message) {
		log.log(Level.INFO, message);
		self.addMessage(message, true);
    }

    @Override
    public void onChangeRoom() {
		Iterator<User> iter = users.iterator();
		while (iter.hasNext()) {
		    User u = iter.next();
		    removeClient(u);
		    iter.remove();
		}
		goToPanel("lobby");
    }

    public static void main(String[] args) {
		ClientUI ui = new ClientUI("My UI");
		if (ui != null) {
		    log.log(Level.FINE, "Started");
		}
    }
    @Override
    public void onSyncDirection(String clientName, Point direction) {
		// TODO Auto-generated method stub
		// no need to sync this for ClientUI
    }

    @Override
    public void onSyncPosition(String clientName, Point position) {
		// TODO Auto-generated method stub
		// no need to sync this for ClientUI
    }

    @Override
    public void onGetRoom(String roomName) {
		// TODO Auto-generated method stub
		if (roomsPanel != null) {
		    roomsPanel.addRoom(roomName);
		    pack();
		}
    }
}