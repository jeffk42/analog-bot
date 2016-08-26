/**
 * 
 */
package jmk.reddit.analogbot;

import java.util.logging.Level;
import java.util.logging.Logger;

import jmk.reddit.util.AnalogBotBase;
import net.dean.jraw.RedditClient;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

/**
 * A concurrently running thread that occasionally requests an updated comment list and looks
 * for users making calls to the bot.
 * 
 * @author jkraus
 *
 */
public class MessageMonitor extends AnalogBotBase implements Runnable {
	
	private RedditClient client = null;
	private AnalogBot parent = null;
	
    private static final Logger LOG = Logger.getLogger(MessageMonitor.class.getName());


	public MessageMonitor(AnalogBot parent, RedditClient client) {
		
		this.client = client;		
		this.parent = parent;
	}
	
	private boolean handleUnreadMessage(Message m)
	{
		try
		{
			parent.handleMessageRequest(m);
		} catch (Exception ex) {
			LOG.log(Level.SEVERE, "Error during handle message request.", ex);
			return false;
		}
		return true;
	}

	
	@Override
	public void run() {
		
		InboxPaginator inboxPaginator = new InboxPaginator(client, "unread");
		
		while (true)
		{
			inboxPaginator.setSorting(Sorting.NEW);
			inboxPaginator.setTimePeriod(TimePeriod.HOUR);
			
			synchronized (AnalogBot.class)
			{
				inboxPaginator.reset();
				
				if (client.isAuthenticated())
				{
					if (inboxPaginator.hasNext())
					{
						Listing<Message> messageList = inboxPaginator.next();
						
						for (Message message : messageList)
						{
							LOG.info("Unread message: "+message.getSubject() + " from: "+message.getAuthor());
							// ignore "post reply from" messages
							if (!message.getSubject().startsWith("post reply") &&
									!message.getSubject().startsWith("comment reply") && 
									!message.getSubject().startsWith("username mention"))
								handleUnreadMessage(message);
							else parent.skipMessage(message);
							
						}
						
					}
				}
				else
				{
					LOG.warning("Skipping a round due to authentication issues.");
					utilities.changeConnectionState(false);
				}
			}
			
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				LOG.log(Level.SEVERE, "Error during thread sleep.", e);
			}
		}
	}

}
