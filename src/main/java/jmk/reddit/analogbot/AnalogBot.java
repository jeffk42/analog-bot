/**
 * 
 */
package jmk.reddit.analogbot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

import jmk.reddit.analogbot.util.AnalogBotBase;
import jmk.reddit.analogbot.util.WeeklyThreadTimer;
import jmk.reddit.util.RedditConnector;
import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.managers.InboxManager;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Message;

/**
 * The executable class that starts up AnalogBot.
 * @author jkraus
 *
 */
public class AnalogBot extends AnalogBotBase {
	
    private static final Logger LOG = Logger.getLogger(AnalogBot.class.getName());

    // Default regex for the call command
	public String COMMENT_LISTENER_REGEX = "(^|.*[ ]+)(AnalogBot!)\\W*(.*)$.*";
	
	private AccountManager account = null;
	private InboxManager inbox = null;
	private AnalogBotCommands commandUtil = null;

	
	public AnalogBot() {
		
		// Check to see if a custom call command has been defined.
		String callProp = properties.getProperty("AnalogBot.CommentMonitor.callString");
		if (callProp != null && !callProp.trim().isEmpty())
			COMMENT_LISTENER_REGEX = "(^|.*[ ]+)("+ callProp +")\\W*(.*)$.*";
		
		// initial authentication is outside of the thread to avoid concurrency issues.
		RedditConnector.getInstance().getScriptAppAuthentication(true);
		
		// now start the thread to reauthorize as necessary.
		startAuthenticationListenerThread();
		

		account = new AccountManager(RedditConnector.getInstance().getClient());
		inbox = new InboxManager(RedditConnector.getInstance().getClient());
		commandUtil = AnalogBotCommands.getInstance();
	
		// spawn threads for each of the primary functions, if enabled.
		startCommentListenerThreads();
		startMessageListenerThread();
		startWeeklyPostThread();
		
		
		
		LOG.info("Process started"+(Boolean.getBoolean(this.getProperties().getProperty("AnalogBot.testMode"))?" in test mode.":"."));
	}
	
	/** Test method to auto-post the weekly stats on startup. */
	private void postWeeklyNow() {
		Calendar date1 = Calendar.getInstance();
		date1.add(Calendar.DATE, -1);
		SimpleDateFormat dt1 = new SimpleDateFormat("dd MMM yyyy");
		String postContent = commandUtil.getWeeklyStats(
				properties.getProperty("AnalogBot.WeeklyPost.subreddit"), true);
		selfPost("Weekly Stats For /r/"+ properties.getProperty("AnalogBot.WeeklyPost.subreddit") +
				", Week Ending " + dt1.format(date1.getTime()), postContent, 
				properties.getProperty("AnalogBot.WeeklyPost.subreddit"));
	}
	
	/**
	 * This method creates a thread for each subreddit that we're listening to, for the purpose
	 * of monitoring incoming comments.
	 */
	private void startCommentListenerThreads() 
	{	
		if (Boolean.parseBoolean(properties.getProperty("AnalogBot.enableCommentMonitor")))
		{
			String listenSubs = getProperties().getProperty("AnalogBot.CommentMonitor.subredditsToMonitor");
			if (listenSubs != null && !listenSubs.trim().isEmpty())
			{
				String[] subList = listenSubs.split(",");
				for (String subreddit : subList)
				{
					Thread commentStreamThread = new Thread(new CommentStreamMonitor(this, RedditConnector.getInstance().getClient(), subreddit.trim()), "CommentMonitor_"+subreddit.trim());
					LOG.info("Firing up the Comment Monitor thread for /r/"+subreddit.trim());
					commentStreamThread.start();
				}
			}
			else LOG.info("No subreddits defined for comment listening; Bot will operate in PM-only mode.");
		}
		else
		{
			LOG.info("Comment Monitor threads have been disabled in preferences.");
		}
	}
	
	/**
	 * Starts a thread for monitoring the bot's inbox for new requests.
	 */
	private void startMessageListenerThread() 
	{	
		if (Boolean.parseBoolean(properties.getProperty("AnalogBot.enableMessageMonitor")))
		{
			Thread messageStreamThread = new Thread(new MessageMonitor(this, RedditConnector.getInstance().getClient()), "MessageMonitor");
			LOG.info("Firing up the Message Monitor thread.");
			messageStreamThread.start();
		}
		else
		{
			LOG.info("Message Monitor thread has been disabled in preferences.");
		}
	}
	
	/**
	 * Creates a thread that is used to keep the bot authenticated.
	 */
	private void startAuthenticationListenerThread() 
	{	
		Thread authStreamThread = new Thread(RedditConnector.getInstance());
		authStreamThread.start();
	}
	
	/**
	 * Starts the thread that generates a weekly stats post.
	 * @deprecated Use WeeklyStatisticsGenerator in a cron job
	 */
	@Deprecated
	private void startWeeklyPostThread() {
		if (Boolean.parseBoolean(properties.getProperty("AnalogBot.enableWeeklyPost"))) {
			Calendar date1 = Calendar.getInstance();
			
			
			SimpleDateFormat dt1 = new SimpleDateFormat("dd MMM yyyy");
			
			
			Date prefDate = null;
			try {
				prefDate = dt1.parse(properties.getProperty("AnalogBot.WeeklyPost.nextWeeklyPost"));
			} catch (ParseException e) {
				LOG.log(Level.WARNING, "Problem parsing weekly post start date. Defaulting to upcoming Monday.");
			} catch (NullPointerException e) {
				LOG.log(Level.WARNING, "Problem getting weekly post start date. Defaulting to upcoming Monday.");
			}
			
			// if using preferences date, don't change it.
			if (prefDate != null)
				date1.setTime(prefDate);
			else {
				// if it's already Monday, let's start on the next one.
				if (date1.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
					date1.add(Calendar.DATE, 7);
				
				while (date1.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
				    date1.add(Calendar.DATE, 1);
				}
			}
			
			
			// TODO: allow prefs to define day of week to post.
			//int day = date1.get(Calendar.DAY_OF_WEEK);
			String prefTime = properties.getProperty("AnalogBot.WeeklyPost.dataGatheringTime");
	
			int hour = Integer.parseInt(prefTime.substring(0,2));
			int min = Integer.parseInt(prefTime.substring(2,4));
			date1.set(Calendar.HOUR_OF_DAY, hour);
			date1.set(Calendar.MINUTE, min);
			date1.set(Calendar.SECOND, 0);
			date1.set(Calendar.MILLISECOND, 0);
			
			LOG.info("Will attempt to compile the next weekly email on "+
					DateFormat.getInstance().format(date1.getTime())+" and post it after the SubmissionOffset.");
			
			Timer timer = new Timer("schedule");
			timer.scheduleAtFixedRate(new WeeklyThreadTimer(commandUtil), date1.getTime(), 1000 * 60 * 60 * 24 * 7);

		}
		else LOG.info("Weekly Post thread has been disabled.");
	}
	
	/**
	 * Create a new post.
	 * 
	 * @param title
	 * @param postContent
	 * @param subreddit
	 */
	public void selfPost(String title, String postContent, String subreddit) {
		AccountManager.SubmissionBuilder builder = new AccountManager.SubmissionBuilder(postContent, subreddit, title);
		
		try {
			account.submit(builder);
		} catch (NetworkException e) {
			e.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reply to an existing comment.
	 * 
	 * @param comment
	 * @param reply
	 */
	public void replyToComment(Comment comment, String reply)
	{
		try {
			account.reply(comment, reply);
			LOG.info(" - Request Complete.");
		} catch (NetworkException e) {
			e.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Reply to a private message.
	 * 
	 * @param message
	 * @param response
	 */
	public void replyToMessage(Message message, String response)
	{
		try {
			inbox.compose(message.getAuthor(), "AnalogBot Request: "+message.getSubject(), response);
			inbox.setRead(true, message);
			LOG.info(" - Request Complete.");
		} catch (NetworkException e) {
			e.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Skip a private message.
	 * 
	 * @param message
	 * @param response
	 */
	public void skipMessage(Message message)
	{
		try {
			inbox.setRead(true, message);
			LOG.info(" - Message skipped.");
		} catch (NetworkException e) {
			e.printStackTrace();
		} 
	}
	
	public void handleCommentRequest(Comment comment, String requestString)
	{
		LOG.info(" - Request Made: "+requestString);
		replyToComment(comment, commandUtil.parseCommand(comment, requestString));
	}
	
	public void handleMessageRequest(Message message)
	{
		LOG.info(" - PM Request Made: "+ message.getBody());
		replyToMessage(message, commandUtil.parseMessage(message));
	}


	/**
	 * @return the account
	 */
	public AccountManager getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(AccountManager account) {
		this.account = account;
	}

	/**
	 * @return the inbox
	 */
	public InboxManager getInbox() {
		return inbox;
	}

	/**
	 * @param inbox the inbox to set
	 */
	public void setInbox(InboxManager inbox) {
		this.inbox = inbox;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                LOG.info("Signal Received. Attempting graceful exit.");
                RedditConnector.getInstance().closeConnection(true);
                LOG.info("Connection closed.");
            }
        });
		new AnalogBot();
	}

}
