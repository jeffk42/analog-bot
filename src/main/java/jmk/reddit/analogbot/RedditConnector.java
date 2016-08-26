/**
 * 
 */
package jmk.reddit.analogbot;

import java.util.logging.Level;
import java.util.logging.Logger;

import jmk.reddit.util.AnalogBotBase;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;

/**
 * @author jkraus
 *
 */
public class RedditConnector extends AnalogBotBase implements Runnable {

	private Credentials credentials = null;
	private UserAgent myUserAgent = null;
	private RedditClient client = null;
	private long lastAuthenticationDate = 0;
	private long authenticationCheckPeriod = 1000 * 60 * 55;
	
	private static RedditConnector instance = null;
    private static final Logger LOG = Logger.getLogger(RedditConnector.class.getName());

	
	private RedditConnector() {

		myUserAgent = UserAgent.of("desktop", "jmk.reddit.AnalogBot", "v0.2", properties.getProperty("AnalogBot.username"));
		client = new RedditClient(myUserAgent);
		credentials = Credentials.script(
				properties.getProperty("AnalogBot.username"), 
				properties.getProperty("AnalogBot.password"),
				properties.getProperty("AnalogBot.appId"),
				properties.getProperty("AnalogBot.secret") );

	}
	
	public static synchronized RedditConnector getInstance() {
		if (instance == null)
		{
			instance = new RedditConnector();	
		}
		
		return instance;
	}
	
	public RedditClient getScriptAppAuthentication() {
		// authenticate with Reddit before doing anything.
		
		OAuthData authData = null;
		try {
			authData = client.getOAuthHelper().easyAuth(credentials);
		} catch (NetworkException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		}
		
		if (authData != null) 
		{
			client.authenticate(authData);
			LOG.info("Authenticated as: "+ client.getAuthenticatedUser());
			updateLastAuthenticationDate();
			utilities.changeConnectionState(true);
		}

		return client;
	}
	
	public void closeConnection() {
		client.getOAuthHelper().revokeAccessToken(credentials);
		client.deauthenticate();
		utilities.changeConnectionState(false);
	}
	
	public void updateLastAuthenticationDate() {
		lastAuthenticationDate = System.currentTimeMillis();
	}
	
	public boolean isTimeToReauthenticate() {
		return (System.currentTimeMillis() > (lastAuthenticationDate + authenticationCheckPeriod));
	}
	
	/**
	 * @return the credentials
	 */
	public Credentials getCredentials() {
		return credentials;
	}

	/**
	 * @param credentials the credentials to set
	 */
	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	/**
	 * @return the myUserAgent
	 */
	public UserAgent getMyUserAgent() {
		return myUserAgent;
	}

	/**
	 * @param myUserAgent the myUserAgent to set
	 */
	public void setMyUserAgent(UserAgent myUserAgent) {
		this.myUserAgent = myUserAgent;
	}

	/**
	 * @return the client
	 */
	public RedditClient getClient() {
		return client;
	}

	/**
	 * @param client the client to set
	 */
	public void setClient(RedditClient client) {
		this.client = client;
	}

	/**
	 * @return the lastAuthenticationDate
	 */
	public long getLastAuthenticationDate() {
		return lastAuthenticationDate;
	}

	/**
	 * @param lastAuthenticationDate the lastAuthenticationDate to set
	 */
	public void setLastAuthenticationDate(long lastAuthenticationDate) {
		this.lastAuthenticationDate = lastAuthenticationDate;
	}

	/**
	 * @return the authenticationCheckPeriod
	 */
	public long getAuthenticationCheckPeriod() {
		return authenticationCheckPeriod;
	}

	/**
	 * @param authenticationCheckPeriod the authenticationCheckPeriod to set
	 */
	public void setAuthenticationCheckPeriod(long authenticationCheckPeriod) {
		this.authenticationCheckPeriod = authenticationCheckPeriod;
	}

	@Override
	public void run() {
		
		while (true) {
			// First, re-authenticate if authentication is expiring.
			if (isTimeToReauthenticate())
			{
				synchronized(AnalogBot.class) {	
					
					if (client.isAuthenticated())
					{
						closeConnection();
						
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							LOG.log(Level.SEVERE, "Error during thread sleep.", e);
						}
						
					}
					else
						LOG.info("No authentication present. Attempting to authenticate.");

					getScriptAppAuthentication();
					
				}
				
				// We just tried, but what if it fails?
				if (!client.isAuthenticated())
				{
					// Wait a bit and try again.
					try {
						Thread.sleep(60000); // 1 minute sleep
					} catch (InterruptedException e) {
						LOG.log(Level.SEVERE, "Error during thread sleep.", e);
					}
				}
			}
			else 
			{ // not time to reauth, but try anyway if there's a problem.
				if (! client.isAuthenticated())
				{
					getScriptAppAuthentication();
				}
				
				try {
					Thread.sleep(60000 * 3); // 3 minute sleep
				} catch (InterruptedException e) {
					LOG.log(Level.SEVERE, "Error during thread sleep.", e);
				}
			}
		}
		
	}

}
