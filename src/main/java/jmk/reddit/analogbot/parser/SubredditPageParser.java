package jmk.reddit.analogbot.parser;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import jmk.reddit.analogbot.util.AnalogBotBase;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.HttpRequest;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.RestResponse;


public class SubredditPageParser extends AnalogBotBase {
	
	protected static final Logger LOG = Logger.getLogger(SubredditPageParser.class.getName());
	
	public JsonNode getJsonFromUrl(RedditClient client, String path)
	{
		
		HttpRequest request = client.request().host(
				properties.getProperty("SubredditPageParser.host"))
                .path(path)
                .build();
		LOG.log(Level.INFO, "making request: "+ path);
		
		RestResponse response = null;
		try
		{
			response = client.execute(request);
		} catch (NetworkException e)
		{
			LOG.log(Level.WARNING, "A problem occurred getting the JSON response from "+path, e);
			response = e.getResponse();
		}

		if (response.getStatusCode() == 200 && response.getJson() != null)
		{
			return response.getJson();
		}
		
		return null;
	}

}
