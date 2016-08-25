package jmk.reddit.analogbot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jmk.reddit.analogbot.parser.SubredditBannedParser;
import jmk.reddit.analogbot.parser.SubredditTrafficParser;
import jmk.reddit.util.AnalogBotBase;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubmissionSearchPaginator;
import net.dean.jraw.paginators.SubmissionSearchPaginator.SearchSort;
import net.dean.jraw.paginators.SubmissionSearchPaginator.SearchSyntax;
import net.dean.jraw.paginators.TimePeriod;

public class AnalogBotCommands extends AnalogBotBase {
	
	private String GET_PHOTOS_REGEX = "^((?:get|give|list|show|display|find)[\\s]*(?:me)?[\\s]*(?:the)?)[\\s]*([\\d]*)[\\s]*([a-z]*[\\s]*[a-z]*)[\\s]*([\\d]*)[\\s]*photos.*(/u/[\\w-]+).*$";
	private String GET_PHOTOS_MSG_REGEX = ".*((?:get|give|list|show|display|find)[\\s]*(?:me)?[\\s]*(?:the)?)[\\s]*([\\d]*)[\\s]*([a-z]*[\\s]*[a-z]*)[\\s]*([\\d]*)[\\s]*photos.*(/u/[\\w-]+).*$";
	private String WEEKLY_STATS_REGEX = "^.*weekly[ ]?(stats|statistics).*$";
	private String PERSONAL_STATS_REGEX = "^.*personal[ ]?(stats|statistics).*$";
	
	private String ae1Regex = ".*[Aa][Ee]-?1.*";
	private String portraRegex = ".*[Pp][Oo][Rr][Tt][Rr][Aa].*";
	private String ektarRegex = ".*[Ee][Kk][Tt][Aa][Rr].*";
	private String tmaxRegex = ".*[Tt]-?[Mm][Aa][Xx].*";
	private String ilfordRegex = ".*([Dd][Ee][Ll][Tt][Aa]|[Hh][Pp]5|[Ff][Pp]4|[Ii][Ll][Ff][Oo][Rr][Dd]|[Pp][Aa][Nn][ ]?[Ff]).*";

	private DecimalFormat percFormat = new DecimalFormat("#.#");
	
	//private RedditClient client = null;
	private static AnalogBotCommands instance = null;
	
    private static final Logger LOG = Logger.getLogger(AnalogBotCommands.class.getName());
    
	
	protected AnalogBotCommands()
	{
		
	}
	
	public static synchronized AnalogBotCommands getInstance() {
		if (instance == null)
		{
			instance = new AnalogBotCommands();	
		}
		
		return instance;
	}
	
	
	public String parseCommand(Comment comment, String command)
	{
		String botCommand = command.toLowerCase().trim();
		
		if (botCommand.matches(GET_PHOTOS_REGEX))
		{
			return parseGetPhotoRequest(botCommand, GET_PHOTOS_REGEX);			
		}
		else if (botCommand.matches(WEEKLY_STATS_REGEX))
		{
			return getWeeklyStats(comment.getSubredditName(), false);
		}
		else if (botCommand.matches(PERSONAL_STATS_REGEX))
		{
			return getPersonalStats(comment.getAuthor());
		}

		
		String com = command.toLowerCase().trim();
		
		if (com.startsWith("hey") || com.startsWith("hello") || com.startsWith("sup") || 
				com.startsWith("what's up") || com.startsWith("whats up"))
			return sayHello();

		return "Hello, fellow human. I'm afraid I don't understand your human words.";
	}
	
	public String parseMessage(Message message)
	{
		String botCommand = message.getBody().toLowerCase().trim();
		
		if (botCommand.matches(GET_PHOTOS_MSG_REGEX))
		{
			return parseGetPhotoRequest(botCommand, GET_PHOTOS_MSG_REGEX);			
		}
		else if (botCommand.matches(WEEKLY_STATS_REGEX))
		{
			return getWeeklyStats(properties.getProperty("analog"), false);
		}
		else if (botCommand.matches(PERSONAL_STATS_REGEX))
		{
			return getPersonalStats(message.getAuthor());
		}
		else if (botCommand.matches(".*[ ]?help.*"))
		{
			return displayHelp(message);
		}
		else if (botCommand.contains("hey") || botCommand.contains("hello") || botCommand.contains("sup") || 
				botCommand.contains("what's up") || botCommand.contains("whats up"))
			return sayHello();

		return "Hello, fellow human. I'm afraid I don't understand your human words.";
	}
	
	private String sayHello() {
		LOG.info("  saying hello.");
		return "Greetings fellow human of which I am definitely also one!\n\n&nbsp;\n\n*^^bleep, ^^bloop*";
	}
	
	private String displayHelp(Message m) {
		return "Hi there! Feel free to [check out my wiki page](https://www.reddit.com/r/analog/wiki/analogbot) for information!";
	}
	
	/**
	 * Parse out the command to build a photo search.
	 * @param botCommand
	 */
	private String parseGetPhotoRequest(String botCommand, String regex)
	{
		int defaultResultSize = Integer.parseInt(properties.getProperty("AnalogBot.defaultSearchResults"));
		
		int resultSize = defaultResultSize; // the default.
		Pattern r = Pattern.compile(regex);
		Matcher m = r.matcher(botCommand);
		boolean validResultSizeFound = false;
		String username = "";
		SearchSort sortType = SearchSort.NEW;
		
		
		if (m.find())
		{
			// The size of the result set can be located in group 2 or 4.
			if (!isNullOrEmpty(m.group(2)))
			{
				try {
					resultSize = Integer.parseInt(m.group(2));
					if (resultSize < 1) 
						throw new NumberFormatException();
					else validResultSizeFound = true;
					
				} catch (NumberFormatException nfe) {
					LOG.warning("invalid result size, defaulting.");
					resultSize = defaultResultSize;
				}
			}
			
			if (!isNullOrEmpty(m.group(4)) && !validResultSizeFound)
			{
				try {
					resultSize = Integer.parseInt(m.group(4));
					if (resultSize < 1) 
						throw new NumberFormatException();
					else validResultSizeFound = true;
					
				} catch (NumberFormatException nfe) {
					LOG.warning("invalid result size, defaulting.");
					resultSize = defaultResultSize;
				}
			}
			
			if (!isNullOrEmpty(m.group(5)) && m.group(5).startsWith("/u/"))
			{
				username = m.group(5).substring(3); // Cut off the "/u/" for search
			}
			
			if (m.group(3) == null || m.group(3).trim().isEmpty())
			{
				sortType = SearchSort.NEW;
			}
			else
			{
				String sort = m.group(3).toLowerCase().trim();
				
				if (sort.contains("new"))
					sortType = SearchSort.NEW;
				else if (sort.contains("hot"))
					sortType = SearchSort.HOT;
				else if (sort.contains("top"))
					sortType = SearchSort.TOP;
				else if (sort.contains("relevan"))
					sortType = SearchSort.RELEVANCE;
				else if (sort.contains("comment"))
					sortType = SearchSort.COMMENTS;
				else if (sort.contains("best"))
					sortType = SearchSort.TOP;
				else if (sort.contains("highest rat"))
					sortType = SearchSort.TOP;
				else if (sort.contains("most upvoted"))
					sortType = SearchSort.TOP;
				else if (sort.contains("most recent"))
					sortType = SearchSort.NEW;
				else
					sortType = SearchSort.NEW;

			}
			
		}
		
		return getPhotosFromUserSearch(username, sortType, resultSize);
	}
	
	private String getPhotosFromUserSearch(String username, SearchSort sortType, int limit) {
		int maxResultSize = Integer.parseInt(properties.getProperty("AnalogBot.maxSearchResults"));
		String resultString = "";
		
		ArrayList<Submission> results = postSearch("analog",sortType, null, 
				limit, "(and (field author '"+username+"') self:0 )");
		
		resultString += "Getting "+limit+" photos for /u/"+username+", sorted by "+
				sortType.name()+":\n\n";
		
		if (results.size() > 0)
		{
			resultString += "Post|Link|Date|Score|Comments\n:--|:--|:--|:--:|:--:\n";
			int resultNum = 0;
			for (Submission line : results)
			{
				if (resultNum >= maxResultSize)
					break;
				TimeZone timeZone = TimeZone.getTimeZone("GMT");
				Calendar calendar = Calendar.getInstance(timeZone);
				calendar.setTime(line.getCreated());
				SimpleDateFormat simpleDateFormat = 
				       new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy", Locale.US);
				simpleDateFormat.setTimeZone(timeZone);
				
				resultString += "["+line.getTitle().replace("|", ":").replace("[", "(").replace("]", ")")+
						"]("+line.getShortURL()+")|[Direct&nbsp;Link]("+line.getUrl()+")|`"+simpleDateFormat.format(calendar.getTime()) +
						"`|`"+line.getScore()+ "`|`"+line.getCommentCount()+"`\n";
				resultNum++;
			}
			resultString += "\n\n*^^bleep, ^^bloop*";
		}
		else {
			resultString += "*Hmmmm....* It looks as if "+username+" doesn't have any photos shared with /r/analog. "+
					"They clearly can't be trusted. But you can trust AnalogBot.\n\n&nbsp;\n\n*^^bleep, ^^bloop*\n\n";
		}
		
		
		return resultString;
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
	 * Returns a string formatted for Reddit that contains the subreddit statistics
	 * for the previous seven days. 
	 * @param subreddit the subreddit to gather statistics for
	 * @param weeklyPost false for on-demand commands, true for scheduled weekly posts. 
	 * If true, statistics are included that require additional bandwidth and several minutes
	 * to compile.
	 * @return
	 */
	public String getWeeklyStats(String subreddit, boolean weeklyPost) {
		String statPost = "";
		
		ArrayList<Submission> textPosts = postSearch(subreddit, SearchSort.NEW, 
				TimePeriod.WEEK,"self:1");
		ArrayList<Submission> linkPosts = postSearch(subreddit, SearchSort.NEW, 
				TimePeriod.WEEK,"self:0");
		ArrayList<Submission> allPosts = new ArrayList<Submission>();
		if (weeklyPost)
		{
			for (Submission s : linkPosts)
				allPosts.add(RedditConnector.getInstance().getClient().getSubmission(s.getId()));
			for (Submission s : textPosts)
				allPosts.add(RedditConnector.getInstance().getClient().getSubmission(s.getId()));
		}
		else
		{
			allPosts.addAll(linkPosts);
			allPosts.addAll(textPosts);
		}
		
		// Rather than search again, just look at the current results.
		statPost += "#Weekly Statistics For /r/"+subreddit+": \n\n";
		
		if (weeklyPost)
			statPost += "This post is a collection of stats for /r/"+subreddit+" for the previous week."+
				" It's a new thing, compiled by [AnalogBot](https://www.reddit.com/r/analog/wiki/analogbot), that we're trying out for a couple of weeks. "+
				" We'd love to know what you think. Like it? Hate it? Have a suggestion for new stats? Leave a comment!\n\n&nbsp;\n";

		
		statPost += "###Post Statistics: \n";
		
		statPost += "* Total posts this week: **"+allPosts.size()+"** \n";
		statPost += "* Image posts this week: **"+linkPosts.size() + "** *("+ getPercentageString(linkPosts.size(),allPosts.size()) +" of posts)* \n";
		statPost += "* Text posts this week: **"+textPosts.size() + "** *("+ getPercentageString(textPosts.size(),allPosts.size()) +" of posts)* \n\n";
		
		statPost += "###Photo Detail Statistics: \n";
		
		// Number of photos taken with an AE-1
		ArrayList<Submission> ae1Results = searchResultRegexFilter(linkPosts,ae1Regex);
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
			
			if (weeklyPost && (s.getComments() != null))
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
		
		ArrayList<String> topPostingUsers = getTopThreeUsers(userActivityMap);
		ArrayList<String> topCommentingUsers = null;
		
		if (weeklyPost) topCommentingUsers = getTopThreeUsers(userCommentMap);
		
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
		
		if (weeklyPost) {
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
	
	
	private String getPersonalStats(String author) 
	{
		
		String[] movies = properties.getProperty("AnalogBot.movies").split(",");
		int movieNum = Math.round((int) (Math.random() * (movies.length-1)));
		String statPost = "*It's always about you, isn't it? Okay, here are some stats.*\n\n";
		
		ArrayList<Submission> textPosts = postSearch("analog", SearchSort.NEW, 
				TimePeriod.ALL,"(and (field author '"+author+"') self:1 )");
		ArrayList<Submission> linkPosts = postSearch("analog", SearchSort.NEW, 
				TimePeriod.ALL,"(and (field author '"+author+"') self:0 )");
		ArrayList<Submission> allPosts = new ArrayList<Submission>();
		allPosts.addAll(linkPosts);
		allPosts.addAll(textPosts);
		
		if (allPosts.size() != 0)
		{
			// Rather than search again, just look at the current results.
			statPost += "#Personal Statistics For "+author+": \n";
			
			statPost += "###Post Statistics: \n";
			
			statPost += "* Total posts in /r/analog: **"+allPosts.size()+"** \n";
	
			statPost += "* Image posts: **"+linkPosts.size() + "** *("+ getPercentageString(linkPosts.size(),allPosts.size()) +" of your total)* \n";
			statPost += "* Text posts: **"+textPosts.size() + "** *("+ getPercentageString(textPosts.size(),allPosts.size()) +" of your total)* \n\n";
		
			statPost += "###Photo Detail Statistics: \n";
			
			ArrayList<Submission> portraResults = searchResultRegexFilter(linkPosts,portraRegex);
			int portraCount = portraResults.size();
			statPost += "* Photos taken on Portra film: **"+ portraCount +"** *("+ getPercentageString(portraCount,linkPosts.size()) +" of photos)* \n";		
			
			ArrayList<Submission> results = searchResultRegexFilter(linkPosts,ektarRegex);
			int count = results.size();
			statPost += "* Photos taken on Ektar film: **"+ count +"** *("+ getPercentageString(count,linkPosts.size()) +" of photos)* \n";
			
			results = searchResultRegexFilter(linkPosts,tmaxRegex);
			count = results.size();
			statPost += "* Photos taken on T-Max film: **"+ count +"** *("+ getPercentageString(count,linkPosts.size()) +" of photos)* \n";
	
			results = searchResultRegexFilter(linkPosts,ilfordRegex);
			statPost += "* Photos taken on an Ilford film: **"+ results.size() +"** *("+ getPercentageString(results.size(),linkPosts.size()) +" of photos)* \n\n";
	
			int nsfwCount = searchResultSubmissionFilter(allPosts,"nsfw").size();
			statPost += "* NSFW posts: **"+ nsfwCount +"** *("+ getPercentageString(nsfwCount,allPosts.size()) +" of posts)* \n\n";
	
			int tScore = 0;
			int tComments = 0;
			int topScore = 0;
			Submission topScoreSubmission = null;
			
			for (Submission s : allPosts)
			{
				tScore += s.getScore();
				tComments += s.getCommentCount();
				if (s.getScore() > topScore) {
					topScoreSubmission = s;
					topScore = s.getScore();
				}
			}
			
			int aboveAverageScore = 0;
			double avgScore = (tScore/(double) allPosts.size());
			for (Submission s : allPosts)
			{
				if (s.getScore() > avgScore) {
					aboveAverageScore++;
				}
			}
			
			statPost += "###Vote and Comment Statistics: \n";
			
			statPost += "* Total Upvotes Earned:  **"+ tScore + "** \n";
			statPost += "* Average Upvotes Per Post:  **"+ Double.valueOf(new DecimalFormat("#.#").format((tScore/(double) allPosts.size()))) + 
					"** *("+ getPercentageString(aboveAverageScore,allPosts.size()) +" of your posts beat this average)* \n";
			
			statPost += "* Total Comments On Your Posts:  **"+ tComments + "** \n";
			statPost += "* Average Comments Made On Your Posts:  **"+ Double.valueOf(new DecimalFormat("#.#").format((tComments/(double) allPosts.size()))) + "** \n";
			
			statPost += "* Highest Scoring Post: "+"["+topScoreSubmission.getTitle().replace("|", ":").replace("[", "(").replace("]", ")")+
					"]("+topScoreSubmission.getShortURL()+") with a score of **"+ topScore + "**. \n";
			statPost += "* Favorite Movie:  *"+ movies[movieNum] + "*\n\n";
			statPost += "^**Note:** *^Statistics ^will ^only ^be ^accurate ^up ^to ^Reddit's ^maximum ^allowed ^1000 ^search ^results.*\n\n";
		}
		else
		{
			statPost += "&nbsp;\n\n Oh... wait. I can't give you stats on your posts when you've never posted to the sub! "+
					"Show off some of your stuff and get back to me!\n";
		}
		
		statPost += "\n&nbsp;\n\n*^^bleep, ^^bloop*";
		
		return statPost;
	}
	
//	private String potwTasks() {
//		String returnString = "";
//		Subreddit s;
//		WikiManager wm = new WikiManager(client);
//		WikiPage wp = wm.get(subreddit, page);
//		
//		return returnString;
//	}
	
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
	
}
