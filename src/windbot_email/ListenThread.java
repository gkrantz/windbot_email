package windbot_email;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.imageio.*;

public class ListenThread extends Thread
{
	Path listenpath;
	String recipientEmail;
	String senderEmail;
	String senderPassword;
	public ListenThread(Path listenpath, String recipientEmail, String senderEmail, char[] senderPassword)
	{
		this.listenpath = listenpath;
		this.recipientEmail = recipientEmail;
		this.senderEmail = senderEmail + "@gmail.com";
		this.senderPassword = new String(senderPassword);
	}

	@Override
	public void run()
	{
		FileSystem fs = listenpath.getFileSystem();
		String ignorepath = null;
		try {
			try(WatchService service = fs.newWatchService()){
				listenpath.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE); 
				// Start the infinite polling loop
				WatchKey key = null;
				while (true) {
					key = service.take();

					// Dequeueing events
					Kind<?> kind = null;
					for (WatchEvent<?> watchEvent : key.pollEvents()) {
						// Get the type of the event
						kind = watchEvent.kind();
						if (OVERFLOW == kind) {
							continue; // loop
						} else if (ENTRY_CREATE == kind) {
							Main.updateProgress(1);
							// A new Path was created
							Path newPath = ((WatchEvent<Path>) watchEvent)
									.context();
							// Output
							System.out.println("New path created: " + newPath);
							if (!newPath.toString().equals(ignorepath)){

								Properties props = System.getProperties();
								props.put("mail.smtp.starttls.enable", true);
								props.put("mail.smtp.host", "smtp.gmail.com");
								props.put("mail.smtp.user", senderEmail);
								props.put("mail.smtp.password", senderPassword);
								props.put("mail.smtp.port", "587");
								props.put("mail.smtp.auth", true);



								Session session = Session.getInstance(props,null);
								MimeMessage message = new MimeMessage(session);

								System.out.println("Port: "+session.getProperty("mail.smtp.port"));

								try {
									InternetAddress from = new InternetAddress(senderEmail);
									message.setSubject("Windbot alert");
									message.setFrom(from);
									message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));

									Multipart multipart = new MimeMultipart("alternative");

									BodyPart messageBodyPart = new MimeBodyPart();
									messageBodyPart.setText("Sent by Dolmero.");

									multipart.addBodyPart(messageBodyPart);


									messageBodyPart = new MimeBodyPart();

									String filename = listenpath + "\\" + newPath;
									System.out.println("2: " + filename);

									// Read file
									BufferedImage input_image = null; 
									input_image = ImageIO.read(new File(filename));
									// Convert to png if it's a bmp
									if (filename.substring(filename.length()-3).equals("bmp")){
										filename = filename.substring(0, filename.length()-3) + "png";
										File outputfile = new File(filename);
										ImageIO.write(input_image, "PNG", outputfile);
										ignorepath = newPath.toString();
										ignorepath = ignorepath.substring(0, ignorepath.length()-3) + "png";
									}
									
									//Add png to message
									DataSource source = new FileDataSource(filename);
									messageBodyPart.setDataHandler(new DataHandler(source));
									messageBodyPart.setFileName(filename);
									multipart.addBodyPart(messageBodyPart);
									
									message.setContent(multipart);
									Main.updateProgress(2);
									// Send message
									Transport transport = session.getTransport("smtp");
									transport.connect("smtp.gmail.com", senderEmail, senderPassword);
									System.out.println("Transport: "+transport.toString());
									transport.sendMessage(message, message.getAllRecipients());
									
									// Delete png file
									if (ignorepath.substring(ignorepath.length()-3).equals("png")){
									try {
									    	Files.delete(Paths.get(listenpath + "\\" + ignorepath));
										} catch (NoSuchFileException x) {
									    	System.err.format("%s: no such" + " file or directory%n", ignorepath);
										} catch (DirectoryNotEmptyException x) {
									    	System.err.format("%s not empty%n", ignorepath);
										} catch (IOException x) {
									    	// File permission problems are caught here.
									    	System.err.println(x);
										}
									}
									
								} catch (AddressException e3) {
									e3.printStackTrace();
								} catch (MessagingException e2) {
									e2.printStackTrace();
								}
							}
						} else if (ENTRY_MODIFY == kind) {
							// modified
							Path newPath = ((WatchEvent<Path>) watchEvent)
									.context();
							// Output
							System.out.println("New path modified: " + newPath);
							Main.updateProgress(3);
						}
					}

					if (!key.reset()) {
						break; // loop
					}
				}

			}  catch (InterruptedException ie) {
				ie.printStackTrace();
			}

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
}