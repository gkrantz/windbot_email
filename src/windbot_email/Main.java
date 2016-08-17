package windbot_email;

import java.nio.file.Path;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;

public class Main {
	static String filepath;
	private static void window(){
		JFrame frame = new JFrame("Windbot Emailer");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemResource("windbot_email/Dolmero.png")));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel clientInfo = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(10, 10, 10, 10);
        
        constraints.gridx = 0;
        constraints.gridy = 0;
		JLabel sendtoText = new JLabel("Email to");
	    JTextField sendto = new JTextField(24);
	    sendto.setText("");
	    clientInfo.add(sendtoText, constraints);
	    constraints.gridx = 1;
	    clientInfo.add(sendto, constraints);
	    
	    constraints.gridx = 0;
        constraints.gridy = 1;
        JLabel sendfromText = new JLabel("Email from");
        JLabel sendfromGmail = new JLabel("@gmail.com");
	    JTextField sendfrom = new JTextField(17);
	    sendfrom.setText("");
	    clientInfo.add(sendfromText, constraints);
	    constraints.gridx = 1;
	    clientInfo.add(sendfrom, constraints);
	    constraints.anchor = GridBagConstraints.EAST;
	    clientInfo.add(sendfromGmail, constraints);
	    constraints.anchor = GridBagConstraints.WEST;
	    
	    constraints.gridx = 0;
        constraints.gridy = 2;
        JLabel passwordText = new JLabel("Password");
	    JPasswordField password = new JPasswordField(24);
	    clientInfo.add(passwordText, constraints);
	    constraints.gridx = 1;
	    clientInfo.add(password, constraints);
	      
	    frame.getContentPane().add(clientInfo, BorderLayout.CENTER);
	    JButton send = new JButton();
	    send.setText("Send");
	    frame.getContentPane().add(send, BorderLayout.SOUTH);
	      
	    send.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					send.setText("Waiting...");
					send.setEnabled(false);
					sendto.setEnabled(false);
					Path path;
					File dir = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
					filepath = dir.toString();
					path = dir.toPath();
					ListenThread emailThread = new ListenThread(path, sendto.getText(), sendfrom.getText(), password.getPassword());
					emailThread.start();
				}
			});
	 
	      //Display the window.
	      frame.pack();
	      frame.setVisible(true);
	}

	
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable (){
			public void run() {
				window();
			}
		});
	}
}