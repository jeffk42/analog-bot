package jmk.reddit.analogbot.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import jmk.reddit.analogbot.AnalogBotCommands;
import jmk.reddit.analogbot.RedditConnector;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Submission;

public class WeeklyThreadTimer extends TimerTask {

	AnalogBotCommands commandUtil = null;
	
	protected static final LogManager logManager = LogManager.getLogManager();
	protected static final Logger LOG = Logger.getLogger(WeeklyThreadTimer.class.getName());
	protected static final AnalogBotProperties properties = AnalogBotProperties.getInstance();

	static
    {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException exception) {
            LOG.log(Level.SEVERE, "Error in loading configuration",exception);
        }

    }
	
	public WeeklyThreadTimer(AnalogBotCommands commandUtil) {
		this.commandUtil = commandUtil;
		
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
		RedditClient client = RedditConnector.getInstance().getClient();
		AccountManager account = new AccountManager(client);
		
		try {
			Submission s = account.submit(builder);
			
		} catch (NetworkException e) {
			LOG.log(Level.SEVERE, "NetworkException when posting weekly thread", e);
		} catch (ApiException e) {
			LOG.log(Level.SEVERE, "ApiException when posting weekly thread", e);
		}
		
		
	}
	
	@Override
	public void run() {
		Calendar date1 = Calendar.getInstance();
		date1.add(Calendar.DATE, -1);
		SimpleDateFormat dt1 = new SimpleDateFormat("dd MMM yyyy");
		String postContent = commandUtil.getWeeklyStats(
				properties.getProperty("AnalogBot.WeeklyPost.subreddit"), true);
		long parsedDelay = Long.parseLong(properties.getProperty("AnalogBot.WeeklyPost.postSubmissionOffset"));
		// try sleeping the thread until it's time to post.
		try {
			Thread.sleep(parsedDelay);
		} catch (InterruptedException e) {
			LOG.log(Level.SEVERE, "Exception when waiting to post weekly thread", e);
		}
		LOG.log(Level.INFO, "Posting the Weekly Stats thread now.");
		selfPost("Weekly Stats For /r/"+ properties.getProperty("AnalogBot.WeeklyPost.subreddit") +
				", Week Ending " + dt1.format(date1.getTime()), postContent, 
				properties.getProperty("AnalogBot.WeeklyPost.subreddit"));
		LOG.log(Level.INFO, "Done.");
	}

}
