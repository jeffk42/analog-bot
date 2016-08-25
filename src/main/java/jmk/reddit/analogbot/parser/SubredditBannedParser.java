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
import jmk.reddit.analogbot.parser.node.BannedNode;

/**
 * @author jkraus
 *
 */
public class SubredditBannedParser extends SubredditPageParser {

	protected static final Logger LOG = Logger.getLogger(SubredditSpamParser.class.getName());
    private static SubredditBannedParser instance = null;
	
	
	protected SubredditBannedParser() {
		
	
	}
	
	public static synchronized SubredditBannedParser getInstance() {
		if (instance == null)
		{
			instance = new SubredditBannedParser();	
		}
		
		return instance;
	}
	
	public ArrayList<BannedNode> getBannedNodes(String subreddit)
	{
		
		ArrayList<BannedNode> bannedNodes = new ArrayList<BannedNode>();
		
		JsonNode rootNode = null;
		
		String jsonPath = "/r/"+ subreddit + 
				properties.getProperty("SubredditPageParser.subredditBannedPath")+"?limit=100";
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
				JsonNode node = dataPieces.next();
				BannedNode newNode = new BannedNode();
				
				newNode.setBanned(node.get("date").asLong() * 1000);
				newNode.setName(node.get("name").asText());
				bannedNodes.add(newNode);
			}
		}
		else
		{
			LOG.warning("JSON root node is null.");
		}
		
		return bannedNodes;
	}
	
	public String getBannedStats(String subreddit) {
		ArrayList<BannedNode> spam = getBannedNodes(subreddit);
		String returnString = "";
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		int bannedAccts = 0;

		for (int num = 0; num < spam.size(); num++)
		{
			if (spam.get(num).getBanned() > cal.getTimeInMillis())
			{
				bannedAccts++;
//				System.out.println(num+": "+spam.get(num).getName());
			}
				
		}
		
		returnString += "* Number of spam accounts banned by the mods: **" + bannedAccts + "**\n";
		//returnString += "* Number of pageviews: **" + views + "**\n";
		
		return returnString;
	}

}
