/**
 * 
 */
package jmk.reddit.analogbot.parser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import jmk.reddit.analogbot.RedditConnector;
import jmk.reddit.analogbot.parser.node.SpamNode;

/**
 * @author jkraus
 *
 */
public class SubredditSpamParser extends SubredditPageParser {

	protected static final Logger LOG = Logger.getLogger(SubredditSpamParser.class.getName());
    private static SubredditSpamParser instance = null;
	
	
	protected SubredditSpamParser() {
		
	
	}
	
	public static synchronized SubredditSpamParser getInstance() {
		if (instance == null)
		{
			instance = new SubredditSpamParser();	
		}
		
		return instance;
	}
	
	public ArrayList<SpamNode> getSpamNodes(String subreddit)
	{
		
		ArrayList<SpamNode> spamNodes = new ArrayList<SpamNode>();
		
		JsonNode rootNode = null;
		
		String jsonPath = "/r/"+ subreddit + 
				properties.getProperty("SubredditPageParser.subredditSpamPath")+"?limit=100";
		//String urlString = "https://www.reddit.com/r/analog/about/traffic.json";
		if ( jsonPath != null && !jsonPath.trim().isEmpty())
		{
			rootNode = getJsonFromUrl(RedditConnector.getInstance().getClient(), jsonPath);
		}
		
		if (rootNode != null)
		{
			JsonNode dataNode = rootNode.get("data").get("children");
			
			Iterator<JsonNode> dataPieces = dataNode.iterator();
			
			
			
			while (dataPieces.hasNext())
			{
				JsonNode node = dataPieces.next().get("data");
//				System.out.println("Node: " + DateFormat.getInstance().format(
//						new Date(node.get("created").asLong() * 1000))+ " "+node.get("author").asText());
				SpamNode newNode = new SpamNode();
				
				newNode.setCreated(node.get("created").asLong() * 1000);
				newNode.setBannedBy(node.get("banned_by").asText());
				newNode.setAuthor(node.get("author").asText());
				spamNodes.add(newNode);
			}
		}
		else
		{
			LOG.warning("JSON root node is null.");
		}
		
		return spamNodes;
	}
	
	public String getSpamStats(String subreddit) {
		ArrayList<SpamNode> spam = getSpamNodes(subreddit);
		String returnString = "";
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		int bannedAccts = 0;
//		System.out.println("Lower Bound Date: "+DateFormat.getInstance().format(
//						new Date(cal.getTimeInMillis()))+"\nUpper: "+Calendar.getInstance().getTimeInMillis());
		// Skip the first day, because that's so far today.
		for (int num = 0; num < spam.size(); num++)
		{
			if (spam.get(num).getCreated() > cal.getTimeInMillis() &&
				!spam.get(num).getBannedBy().isEmpty())
			{
				bannedAccts++;
				System.out.println(num+": "+spam.get(num).getAuthor());
			}
				
		}
		
		returnString += "* Number of spam accounts banned by the mods: **" + bannedAccts + "**\n";
		//returnString += "* Number of pageviews: **" + views + "**\n";
		
		return returnString;
	}

}
