package jmk.reddit.weeklystats;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jmk.reddit.analogbot.parser.SubredditBannedParser;
import jmk.reddit.analogbot.parser.SubredditTrafficParser;
import jmk.reddit.util.RedditConnector;
import jmk.reddit.weeklystats.util.WeeklyStatsBase;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubmissionSearchPaginator;
import net.dean.jraw.paginators.SubmissionSearchPaginator.SearchSort;
import net.dean.jraw.paginators.SubmissionSearchPaginator.SearchSyntax;
import net.dean.jraw.paginators.TimePeriod;

public class WeeklyStatisticsGenerator extends WeeklyStatsBase {
	
	protected static final Logger LOG = Logger.getLogger(WeeklyStatisticsGenerator.class.getName());
	private RedditClient client = null;
	
	public static final  String ae1Regex = ".*[Aa][Ee]-?1.*";
	public static final  String portraRegex = ".*[Pp][Oo][Rr][Tt][Rr][Aa].*";
	public static final  String ektarRegex = ".*[Ee][Kk][Tt][Aa][Rr].*";
	public static final  String tmaxRegex = ".*[Tt]-?[Mm][Aa][Xx].*";
	public static final  String ilfordRegex = ".*([Dd][Ee][Ll][Tt][Aa]|[Hh][Pp]5|[Ff][Pp]4|[Ii][Ll][Ff][Oo][Rr][Dd]|[Pp][Aa][Nn][ ]?[Ff]).*";

	private static final long HOUR_SECONDS = 3600;
	private static final long DAY_SECONDS = HOUR_SECONDS * 24;
	private static final long WEEK_SECONDS = DAY_SECONDS * 7;
	
	public DecimalFormat percFormat = new DecimalFormat("#.#");
	
	public WeeklyStatisticsGenerator() {
		long searchTimeOffset = Long.parseLong(properties.getProperty("AnalogBot.WeeklyPost.searchTimeOffset"));
		long toTime = (System.currentTimeMillis() / 1000) + (searchTimeOffset * HOUR_SECONDS);
		long fromTime = toTime - WEEK_SECONDS;
		generateStatsFile(fromTime, toTime);
	}
	
	public WeeklyStatisticsGenerator(String command, long fromTime, long toTime) {
		long searchTimeOffset = Long.parseLong(properties.getProperty("AnalogBot.WeeklyPost.searchTimeOffset"));
		fromTime += (searchTimeOffset * HOUR_SECONDS);
		toTime += (searchTimeOffset * HOUR_SECONDS);
		
		if (command.equalsIgnoreCase("BuildStats"))
			generateStatsFile(fromTime, toTime);
		else if (command.equalsIgnoreCase("PostStats"))
			postStatsFromFile();
		else if (command.equalsIgnoreCase("BuildAndPostStats"))
		{
			generateStatsFile(fromTime, toTime);
			postStatsFromFile();
		}
	}
	
	public void generateStatsFile(long fromTime, long toTime) {
		
		client = RedditConnector.getInstance().getScriptAppAuthentication(false);
		
		if (client.isAuthenticated()) 
		{			
			
			Calendar weekEnding = Calendar.getInstance();
			weekEnding.add(Calendar.DATE, -1);
			SimpleDateFormat dt1 = new SimpleDateFormat("dd MMM yyyy");
			
			// (and timestamp:1373932800..1474019200 title:'something')
			
			String outputFile = properties.getProperty("AnalogBot.WeeklyPost.outputFileName");
			try {
				OutputStream osTime = new BufferedOutputStream(
				        new FileOutputStream(outputFile));

				
				String weeklyPostString = getWeeklyStats(
						properties.getProperty("AnalogBot.WeeklyPost.subreddit"),
						fromTime, toTime);
				osTime.write(weeklyPostString.getBytes());
				
				osTime.flush();
				osTime.close();				
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
//			selfPost("Weekly Stats For /r/"+ properties.getProperty("AnalogBot.WeeklyPost.subreddit") +
//					", Week Ending " + dt1.format(weekEnding.getTime()), postContent, 
//					properties.getProperty("AnalogBot.WeeklyPost.subreddit"));
		}
		else
		{
			LOG.log(Level.SEVERE,"Authentication Failed.");
		}
	}
	
	public void postStatsFromFile()
	{
		String inputFile = properties.getProperty("AnalogBot.WeeklyPost.outputFileName");
		String post = "";
		try {
			FileReader fileReader = new FileReader(inputFile);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                post += line + "\n";
            }   

            // Always close files.
            bufferedReader.close();         

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		System.out.println(post);
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
			account.submit(builder);
			
		} catch (NetworkException e) {
			LOG.log(Level.SEVERE, "NetworkException when posting weekly thread", e);
		} catch (ApiException e) {
			LOG.log(Level.SEVERE, "ApiException when posting weekly thread", e);
		}
		
		
	}
	
	/**
	 * Returns a string formatted for Reddit that contains the subreddit statistics
	 * for the previous seven days. 
	 * @param subreddit the subreddit to gather statistics for
	 * @param weeklyPost false for on-demand commands, true for scheduled weekly posts. 
	 * If true, statistics are included that require additional bandwidth and several minutes
	 * to compile.
	 * @return
	 */
	public String getWeeklyStats(String subreddit, long fromTime, long toTime) {
		String statPost = "";
		
		ArrayList<Submission> textPosts = postSearch(subreddit, SearchSort.NEW, 
				null,"(and timestamp:"+fromTime+".."+toTime+" self:1)");
		ArrayList<Submission> linkPosts = postSearch(subreddit, SearchSort.NEW, 
				null,"(and timestamp:"+fromTime+".."+toTime+" self:0)");
		ArrayList<Submission> allPosts = new ArrayList<Submission>();
		LOG.info("Found "+textPosts.size()+" self posts and "+linkPosts.size()+" link posts. Retrieving link post comments...");
		for (Submission s : linkPosts)
			allPosts.add(RedditConnector.getInstance().getClient().getSubmission(s.getId()));
		LOG.info("Retrieving text post comments...");
		for (Submission s : textPosts)
			allPosts.add(RedditConnector.getInstance().getClient().getSubmission(s.getId()));
		
		
		// Rather than search again, just look at the current results.
		statPost += "#Weekly Statistics For /r/"+subreddit+": \n\n";
		

		statPost += "This post is a collection of stats for /r/"+subreddit+" for the previous week."+
				" It's a new thing, compiled by [AnalogBot](https://www.reddit.com/r/analog/wiki/analogbot), that we're trying out for a couple of weeks. "+
				" We'd love to know what you think. Like it? Hate it? Have a suggestion for new stats? Leave a comment!\n\n&nbsp;\n";

		
		statPost += "###Post Statistics: \n";
		
		statPost += "* Total posts this week: **"+allPosts.size()+"** \n";
		statPost += "* Image posts this week: **"+linkPosts.size() + "** *("+ getPercentageString(linkPosts.size(),allPosts.size()) +" of posts)* \n";
		statPost += "* Text posts this week: **"+textPosts.size() + "** *("+ getPercentageString(textPosts.size(),allPosts.size()) +" of posts)* \n\n";
		
		statPost += "###Photo Detail Statistics: \n";
		
		// Number of photos taken with an AE-1
		ArrayList<Submission> ae1Results = searchResultRegexFilter(linkPosts, ae1Regex);
		int ae1Count = ae1Results.size();
		statPost += "* Photos taken with a Canon AE-1: **"+ ae1Count +"** *("+ getPercentageString(ae1Count,linkPosts.size()) +" of photos)* \n";
		ArrayList<Submission> portraResults = searchResultRegexFilter(linkPosts,portraRegex);
		int portraCount = portraResults.size();
		statPost += "* Photos taken on Portra film: **"+ portraCount +"** *("+ getPercentageString(portraCount,linkPosts.size()) +" of photos)* \n";
		ArrayList<Submission> results = searchResultRegexFilter(ae1Results,portraRegex);
		statPost += "* Photos taken on Portra film with an AE-1: **"+ results.size() +"** *("+ getPercentageString(results.size(),linkPosts.size()) +" of photos)* \n";
		
		results = searchResultRegexFilter(linkPosts,ilfordRegex);
		statPost += "* Photos taken on an Ilford film: **"+ results.size() +"** *("+ getPercentageString(results.size(),linkPosts.size()) +" of photos)* \n\n";
		
		statPost += "###Post Type Statistics: \n";
				
		int flickrCount = searchResultSubmissionFilter(linkPosts,"flickr").size();
		statPost += "* Posted via Flickr: **"+ flickrCount +"** *("+ getPercentageString(flickrCount,linkPosts.size()) +" of photos)* \n";

		int imgurCount = searchResultSubmissionFilter(linkPosts,"imgur").size();
		statPost += "* Posted via Imgur: **"+ imgurCount +"** *("+ getPercentageString(imgurCount,linkPosts.size()) +" of photos)* \n";
		
		int redditCount = searchResultSubmissionFilter(linkPosts,"reddit").size();
		statPost += "* Posted via Reddit: **"+ redditCount +"** *("+ getPercentageString(redditCount,linkPosts.size()) +" of photos)* \n";

		int nsfwCount = searchResultSubmissionFilter(allPosts,"nsfw").size();
		statPost += "* NSFW posts: **"+ nsfwCount +"** *("+ getPercentageString(nsfwCount,allPosts.size()) +" of posts)* \n\n";

		int tScore = 0;
		int tComments = 0;
		int topScore = 0;
		Submission topScoreSubmission = null;
		
		Map<String, Integer> userActivityMap = new HashMap<String, Integer>();
		Map<String, Integer> userCommentMap = new HashMap<String, Integer>();
		
		for (Submission s : allPosts)
		{
			if (!userActivityMap.containsKey(s.getAuthor()))
			{
				userActivityMap.put(s.getAuthor(), new Integer(1));
			}
			else 
			{
				Integer theNum = userActivityMap.remove(s.getAuthor());
				userActivityMap.put(s.getAuthor(), new Integer(theNum.intValue() + 1));
			}
			
			tScore += s.getScore();
			tComments += s.getCommentCount();
			if (s.getScore() > topScore) {
				topScoreSubmission = s;
				topScore = s.getScore();
			}
			
			if (s.getComments() != null)
			{
			
				for (CommentNode c : s.getComments().walkTree())
				{

					if (!userCommentMap.containsKey(c.getComment().getAuthor()))
					{
						userCommentMap.put(c.getComment().getAuthor(), new Integer(1));
					}
					else 
					{
						Integer theNum = userCommentMap.remove(c.getComment().getAuthor());
						userCommentMap.put(c.getComment().getAuthor(), new Integer(theNum.intValue() + 1));
					}
	
				}
			}
		}
		
		if (allPosts.size() > 0)
		{
			ArrayList<String> topPostingUsers = getTopThreeUsers(userActivityMap);
			ArrayList<String> topCommentingUsers = null;
			
			topCommentingUsers = getTopThreeUsers(userCommentMap);
			
			int aboveAverageScore = 0;
			int aboveAverageComments = 0;
			
			double avgScore = (tScore/(double) allPosts.size());
			double avgComments = (tComments/(double) allPosts.size());
			for (Submission s : allPosts)
			{
				if (s.getScore() > avgScore) {
					aboveAverageScore++;
				}
				
				if (s.getCommentCount() > avgComments) {
					aboveAverageComments++;
				}
			}
			
			statPost += "###Vote and Comment Statistics: \n";
			
			statPost += "* Total Upvotes Earned: **"+ tScore + "** \n";
			statPost += "* Average Upvotes Per Post:  **"+ Double.valueOf(new DecimalFormat("#.#").format((tScore/(double) allPosts.size()))) + 
					"** *("+ getPercentageString(aboveAverageScore,allPosts.size()) +" of posts beat the average)* \n";
			
			statPost += "* Total Comments Posted: **"+ tComments + "** \n";
			statPost += "* Average Comments Per Post: **"+ Double.valueOf(new DecimalFormat("#.#").format((tComments/(double) allPosts.size()))) + 
					"** *("+ getPercentageString(aboveAverageComments,allPosts.size()) +" of posts beat the average)* \n";
			
			statPost += "* Highest Scoring Post: "+"["+topScoreSubmission.getTitle().replace("|", ":").replace("[", "(").replace("]", ")")+
					"]("+topScoreSubmission.getShortURL()+") with a score of **"+ topScore + "**. \n\n";
			
			statPost += SubredditTrafficParser.getInstance().getTrafficStats(subreddit);
			statPost += SubredditBannedParser.getInstance().getBannedStats(subreddit);
			statPost += "\n";
			
			statPost += "###Most Active Posters This Week: \n";
			
			boolean testMode = Boolean.parseBoolean(properties.getProperty("AnalogBot.testMode"));
			
			for (int n = 0; n < 3; n++)
			{
				statPost += "* " + (testMode?"":"/u/") + topPostingUsers.get(n) + ", **" + userActivityMap.get(topPostingUsers.get(n)).intValue() + "** posts. \n";
			}
			
	
			statPost += "\n";
			statPost += "###Most Active Commenters This Week: \n";
			for (int n = 0; n < 3; n++)
			{
				if (topCommentingUsers.size() > n)
					statPost += "* " + (testMode?"":"/u/") + topCommentingUsers.get(n) + ", **" + userCommentMap.get(topCommentingUsers.get(n)).intValue() + "** comments. \n";
			}
			
		}
		
		statPost += "\n&nbsp;\n\n*^^bleep, ^^bloop*";
		
		return statPost;
	}
	
	private ArrayList<Submission> searchResultRegexFilter(ArrayList<Submission> resultSet, String regex)
	{
		ArrayList<Submission> filteredResultSet = new ArrayList<Submission>();
		
		for (Submission s : resultSet)
		{
			if (s.getTitle().matches(regex))
			{
				filteredResultSet.add(s);
			}
		}
		
		return filteredResultSet;
	}
	
	private ArrayList<Submission> searchResultSubmissionFilter(ArrayList<Submission> resultSet, String feature)
	{
		ArrayList<Submission> filteredResultSet = new ArrayList<Submission>();
		
		if (feature.equalsIgnoreCase("self:0"))
			for (Submission s : resultSet)
			{
				if (!s.isSelfPost())
				{
					filteredResultSet.add(s);
				}
			}
		else if (feature.equalsIgnoreCase("self:1"))
			for (Submission s : resultSet)
			{
				if (s.isSelfPost())
				{
					filteredResultSet.add(s);
				}
			}
		else if (feature.equalsIgnoreCase("nsfw"))
			for (Submission s : resultSet)
			{
				if (s.isNsfw())
				{
					filteredResultSet.add(s);
				}
			}
		else if (feature.equalsIgnoreCase("flickr"))
			for (Submission s : resultSet)
			{
				if (s.getDomain().toLowerCase().contains("flickr") || 
						s.getDomain().toLowerCase().contains("flic.kr"))
				{
					filteredResultSet.add(s);
				}
			}
		else if (feature.equalsIgnoreCase("imgur"))
			for (Submission s : resultSet)
			{
				if (s.getDomain().toLowerCase().contains("imgur"))
				{
					filteredResultSet.add(s);
				}
			}
		else if (feature.equalsIgnoreCase("reddit"))
			for (Submission s : resultSet)
			{
				if (s.getDomain().toLowerCase().contains("reddit") || 
						s.getDomain().toLowerCase().contains("redd.it"))
				{
					filteredResultSet.add(s);
				}
			}
		
		return filteredResultSet;
	}
	
	private String getPercentageString(int stat, int total)
	{
		if (total > 0)
			return Double.valueOf(percFormat.format(100 * (stat/(double) total)))+"%";
		else return "0.0%";
	}
	
	private boolean isNullOrEmpty(String s)
	{
		if (s == null)
			return true;
		if (s.trim().isEmpty())
			return true;
		
		return false;
	}
	
	private ArrayList<String> getTopThreeUsers(Map<String,Integer> map) {
		
		ArrayList<String> bestUsers = new ArrayList<String>(3);
		
		for (String user : map.keySet())
		{
			if (bestUsers.size() == 0)
			{
				bestUsers.add(0, user);
			}
			else if (bestUsers.size() == 1)
			{
				if (map.get(user).intValue() > 
				map.get(bestUsers.get(0)).intValue())
				{
					bestUsers.add(1, bestUsers.get(0));
					bestUsers.set(0, user);
				}
				else bestUsers.add(1, user);
			}
			else if (bestUsers.size() == 2)
			{
				if (map.get(user).intValue() > 
				map.get(bestUsers.get(0)).intValue())
				{
					bestUsers.add(2, bestUsers.get(1));
					bestUsers.set(1, bestUsers.get(0));
					bestUsers.set(0, user);
				}
				else if (map.get(user).intValue() > 
				map.get(bestUsers.get(1)).intValue())
				{
					bestUsers.add(2, bestUsers.get(1));
					bestUsers.set(1, user);
				}
				else bestUsers.add(2, user);
			}
			else if (bestUsers.size() >= 3)
			{
				if (map.get(user).intValue() > 
				map.get(bestUsers.get(0)).intValue())
				{
					bestUsers.set(2, bestUsers.get(1));
					bestUsers.set(1, bestUsers.get(0));
					bestUsers.set(0, user);
				}
				else if (map.get(user).intValue() > 
				map.get(bestUsers.get(1)).intValue())
				{
					bestUsers.set(2, bestUsers.get(1));
					bestUsers.set(1, user);
				}
				else if (map.get(user).intValue() > 
				map.get(bestUsers.get(2)).intValue())
				{
					bestUsers.set(2, user);
				}
			}
		}
		
		return bestUsers;
	}
	
	public ArrayList<Submission> postSearch(String subreddit, SearchSort sorting, TimePeriod timePeriod, int limit, String query)
	{
		ArrayList<Submission> retList = new ArrayList<Submission>();
		SubmissionSearchPaginator searchPaginator = new SubmissionSearchPaginator(RedditConnector.getInstance().getClient(), query);
		searchPaginator.setSearchSorting(sorting);
		searchPaginator.setSubreddit(subreddit);
		searchPaginator.setLimit(limit);
		
		if (timePeriod != null) searchPaginator.setTimePeriod(timePeriod);
		searchPaginator.setSyntax(SearchSyntax.CLOUDSEARCH);
	
		searchPaginator.reset();
		if (searchPaginator.hasNext())
			retList.addAll(searchPaginator.next());
		else LOG.info("No results.");

		
		return retList;
	}
	
	
	public ArrayList<Submission> postSearch(String subreddit, SearchSort sort, TimePeriod timePeriod, String query)
	{
		// query = "(and (field title 'Portra') self:0 )";
		
		ArrayList<Submission> retList = new ArrayList<Submission>();
		SubmissionSearchPaginator searchPaginator = new SubmissionSearchPaginator(RedditConnector.getInstance().getClient(), query);
		searchPaginator.setSearchSorting(sort);
		searchPaginator.setSubreddit(subreddit);
		searchPaginator.setTimePeriod(timePeriod);
		searchPaginator.setSyntax(SearchSyntax.CLOUDSEARCH);
	
		searchPaginator.reset();

		Listing<Submission> list = null;
		while (searchPaginator.hasNext()){
			list = searchPaginator.next();
			for (Submission sub : list)
			{
				retList.add(sub);
			}
		}
		
		return retList;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("executing with "+args.length + " arguments:");
		
		String command = "";
		long endBound = -1;
		long daysBack = -1;
		
		if (args.length > 0)
			command = args[0];
		if (args.length > 1)
			endBound = Long.parseLong(args[1]);
		if (args.length > 2)
			daysBack = Long.parseLong(args[2]);
		long startBound = -1;
		
		if (daysBack > 0)
			startBound = endBound - (daysBack * DAY_SECONDS);
		
//		if (!command.isEmpty())
//		{
//			
//		}
		
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                LOG.info("Signal Received. Attempting graceful exit.");
                RedditConnector.getInstance().closeConnection(false);
                LOG.info("Connection closed.");
            }
        });
		
		if (args.length == 0)
			new WeeklyStatisticsGenerator();
		else
			new WeeklyStatisticsGenerator(command, startBound, endBound);
		
	}
}
