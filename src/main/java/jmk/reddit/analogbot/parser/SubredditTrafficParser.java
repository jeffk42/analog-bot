/**
 * 
 */
package jmk.reddit.analogbot.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import jmk.reddit.analogbot.parser.node.TrafficNode;
import jmk.reddit.util.RedditConnector;

/**
 * @author jkraus
 *
 */
public class SubredditTrafficParser extends SubredditPageParser {

	protected static final Logger LOG = Logger.getLogger(SubredditTrafficParser.class.getName());
    private static SubredditTrafficParser instance = null;
	
	
	protected SubredditTrafficParser() {
		
	
	}
	
	public static synchronized SubredditTrafficParser getInstance() {
		if (instance == null)
		{
			instance = new SubredditTrafficParser();	
		}
		
		return instance;
	}
	
	public ArrayList<TrafficNode> getTrafficNodes(String subreddit)
	{
		
		ArrayList<TrafficNode> trafficNodes = new ArrayList<TrafficNode>();
		
		JsonNode rootNode = null;
		
		String jsonPath = "/r/"+ subreddit + 
				properties.getProperty("SubredditPageParser.subredditTrafficPath");
		//String urlString = "https://www.reddit.com/r/analog/about/traffic.json";
		if ( jsonPath != null && !jsonPath.trim().isEmpty())
		{
			rootNode = getJsonFromUrl(RedditConnector.getInstance().getClient(), jsonPath);
		}
		
		if (rootNode != null)
		{
			JsonNode dayNode = rootNode.get("day");
			
			Iterator<JsonNode> days = dayNode.iterator();
			
			
			
			while (days.hasNext())
			{
				JsonNode node = days.next();
//				System.out.println("Node: " + DateFormat.getInstance().format(
//						new Date(node.get(0).asLong() * 1000)));
				Iterator<JsonNode> iter = node.iterator();
				TrafficNode newNode = new TrafficNode();
				
				if (iter.hasNext())
					newNode.setDate(iter.next().asLong() * 1000);
				if (iter.hasNext())
					newNode.setUniques(iter.next().asText());
				if (iter.hasNext())
					newNode.setPageviews(iter.next().asText());
				if (iter.hasNext())
					newNode.setSubscriptions(iter.next().asText());
				
				trafficNodes.add(newNode);

			}
		}
		else
		{
			LOG.warning("JSON root node is null.");
		}
		
		return trafficNodes;
	}
	
	public String getTrafficStats(String subreddit) {
		ArrayList<TrafficNode> traffic = getTrafficNodes(subreddit);
		String returnString = "";
		
		int newsubs = 0;
		int views = 0;
		// Skip the first day, because that's so far today.
		if (traffic.size() >= 8)
		{
			for (int dayNum = 1; dayNum <= 7; dayNum++)
			{
				newsubs += Integer.parseInt(traffic.get(dayNum).getSubscriptions());
				views += Integer.parseInt(traffic.get(dayNum).getPageviews());
			}
		}
		returnString += "* Number of new subscriptions: **" + newsubs + "**\n";
		returnString += "* Number of pageviews: **" + views + "**\n";
		
		return returnString;
	}

}
