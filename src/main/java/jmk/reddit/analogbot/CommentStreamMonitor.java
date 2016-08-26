/**
 * 
 */
package jmk.reddit.analogbot;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;

import jmk.reddit.util.AnalogBotBase;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.CommentStream;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;

/**
 * A concurrently running thread that occasionally requests an updated comment list and looks
 * for users making calls to the bot.
 * 
 * @author jkraus
 *
 */
public class CommentStreamMonitor extends AnalogBotBase implements Runnable {
	
	RedditClient client = null;
	String subreddit = null;
	CommentStream commentStream = null;
	AnalogBot parent = null;
    private static final Logger LOG = Logger.getLogger(CommentStreamMonitor.class.getName());
    private String[] ignoredUsers = null;


	public CommentStreamMonitor(AnalogBot parent, RedditClient client, String subreddit) {
		
		this.client = client;
		this.subreddit = subreddit;	
		this.commentStream = new CommentStream(client, subreddit);
		this.parent = parent;
		ignoredUsers = parent.getProperties().getProperty("AnalogBot.ignoredUsers").split(",");
	}
	
	private boolean isCommentAuthoredBy(Comment c, String user)
	{
		return c.getAuthor().equalsIgnoreCase(user);
	}
	
	/**
	 * Check the comments replying directly to this one.  If the user already responded, skip it.
	 * @param c
	 * @param user
	 * @return
	 */
	private boolean isCommentRepliedBy(Comment c, String user)
	{
		
		Submission sub = client.getSubmission(c.getSubmissionId().substring(c.getSubmissionId().lastIndexOf("_")+1));
		CommentNode commentNode = sub.getComments();
		Optional<CommentNode> thing = commentNode.findChild(c.getFullName());
		Iterable<CommentNode> iterable = null;
		if (thing != null && thing.isPresent() && thing.get() != null)
			iterable = thing.get().getChildren();
		if (iterable != null)
		{
			for (CommentNode node : iterable) {
			    if (isCommentAuthoredBy(node.getComment(), user))
			    	return true;
			}
		}
		return false;
	}
	
	private boolean isUserIgnored(String user) {
		for (String ig : ignoredUsers)
		{
			if (ig.trim().equals("user"))
					return true;
		}
		return false;
	}
	
	@Override
	public void run() {
		commentStream.setTimePeriod(TimePeriod.HOUR);
		commentStream.setSorting(Sorting.NEW);
		commentStream.setLimit(10);
		Comment firstComment = null;
		String latestId = "";
		
		while (true)
		{
			synchronized (AnalogBot.class)
			{
				if (client.isAuthenticated())
				{
					
					// Get the new comment stream
					commentStream.reset();
					
					if (commentStream.hasNext())
					{
						Listing<Comment> pageList = null;
						
						try {
							pageList = commentStream.next();
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Re-authentication failed. Skipping this round, will try again.", ne);
							utilities.changeConnectionState(false);
							continue;
						}
						
						// set the latest comment; next round we can ignore everything before it.
						if (pageList.size() > 0) 
							firstComment = pageList.get(0);
						
						
						for (Comment c : pageList)
						{
							// if we reach the latest comment of the last round, we're done.
							if (c.getId().equals(latestId))
								break;
							
							
							Pattern r = Pattern.compile(parent.COMMENT_LISTENER_REGEX, Pattern.MULTILINE);
							
							Matcher m = r.matcher(c.getBody());

							// We can ignore our own posts by using the ignored user property.
							if (!isCommentRepliedBy(c, client.getAuthenticatedUser()) && 
									!isUserIgnored(c.getAuthor()))
							{
								
								// if the comment contains "AnalogBot!" then listen to it.
								if (m.find())
								{
									LOG.info("New Request Comment by "+c.getAuthor()+" at "+c.getUrl());
									parent.handleCommentRequest(c, m.group(3).trim());		
								}
							}
							else
								LOG.info("     Skipping comment "+c.getId()+" because "+client.getAuthenticatedUser()+" posted it or already replied to it.");
						}	
					}
					
					// This is where we left off.
					if (firstComment != null)
						latestId = firstComment.getId();
					
				
				}
				else
				{
					LOG.info("Skipping a round due to authentication issues.");
					utilities.changeConnectionState(false);
				}
			
			}
			try {
				Thread.sleep(15000);
			} catch (InterruptedException e) {
				LOG.log(Level.SEVERE, "Error during thread sleep.", e);
			}
		}
	}

}
