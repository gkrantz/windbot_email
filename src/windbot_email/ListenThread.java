package windbot_email;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.nio.file.FileSystem;
import java.nio.file.Path;
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
	                        // A new Path was created
	                        Path newPath = ((WatchEvent<Path>) watchEvent)
	                                .context();
	                        // Output
	                        System.out.println("New path created: " + newPath);
	                        
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
	                            DataSource source = new FileDataSource(filename);
	                            messageBodyPart.setDataHandler(new DataHandler(source));
	                            messageBodyPart.setFileName(filename);
	                            multipart.addBodyPart(messageBodyPart);

	                            message.setContent(multipart);

	                            // Send message
	                            Transport transport = session.getTransport("smtp");
	                            transport.connect("smtp.gmail.com", senderEmail, senderPassword);
	                            System.out.println("Transport: "+transport.toString());
	                            transport.sendMessage(message, message.getAllRecipients());
	                        } catch (AddressException e3) {
	                            e3.printStackTrace();
	                        } catch (MessagingException e2) {
	                            e2.printStackTrace();
	                        }
	                        
	                    } else if (ENTRY_MODIFY == kind) {
	                        // modified
	                        Path newPath = ((WatchEvent<Path>) watchEvent)
	                                .context();
	                        // Output
	                        System.out.println("New path modified: " + newPath);
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