/**
 * 
 */
package jmk.reddit.analogbot;

import java.util.logging.Level;
import java.util.logging.Logger;

import jmk.reddit.analogbot.util.AnalogBotBase;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;

/**
 * A concurrently running thread that monitors a subreddit for new posts.
 * 
 * @author jkraus
 *
 */
public abstract class PostStreamMonitor extends AnalogBotBase implements Runnable {
	
	protected RedditClient client = null;
	protected String subreddit = null;
	protected SubredditPaginator subPaginator = null;
	protected AnalogBot parent = null;
    private static final Logger LOG = Logger.getLogger(PostStreamMonitor.class.getName());
    protected String LISTENER_REGEX = "^Weekly[\\s]{1}[']OTW[\\s]{1}Search[\\s]{1}Link['][\\s]{1}\\-[\\s]{1}Week[\\s]{1}(\\d+)";


	public PostStreamMonitor(AnalogBot parent, RedditClient client, String subreddit) {
		
		this.client = client;
		this.subreddit = subreddit;	
		this.parent = parent;
		subPaginator = new SubredditPaginator(client, subreddit);
	}
	
	/**
	 * This method is called whenever a new post is found by the monitor thread.
	 * @param post
	 */
	protected abstract void handleIncomingPost(Submission post);
	
	/**
	 * Subclasses override this method to perform tasks before the thread
	 * starts monitoring.
	 */
	protected abstract void performStartupTasks();
	
	@Override
	public void run() {
		performStartupTasks();
		subPaginator.setSubreddit(subreddit);
		subPaginator.setTimePeriod(TimePeriod.DAY);
		subPaginator.setSorting(Sorting.NEW);
		subPaginator.setLimit(5);
		Submission firstPost = null;
		String latestId = "";
		while (true)
		{
			synchronized (AnalogBot.class)
			{
				if (client.isAuthenticated())
				{
					// Get the new comment stream
					subPaginator.reset();
					
					if (subPaginator.hasNext())
					{
						Listing<Submission> pageList = null;
						
						try {
							pageList = subPaginator.next();
						} catch (NetworkException ne) {
							LOG.log(Level.SEVERE, "Comment Monitor thread reports connection problem. Skipping this round, will try again.", ne);
							utilities.changeConnectionState(false);
							continue;
						}
						
						// set the latest post; next round we can ignore everything before it.
						if (pageList.size() > 0) 
						{
							firstPost = pageList.get(0);
						}
						
						
						for (Submission post : pageList)
						{
							// if we reach the latest comment of the last round, we're done.
							if (post.getId().equals(latestId))
								break;
							
							handleIncomingPost(post);
							
						}	
					}
					
					// This is where we left off.
					if (firstPost != null)
						latestId = firstPost.getId();
					
				
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
