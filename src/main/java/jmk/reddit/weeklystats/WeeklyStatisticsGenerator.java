package jmk.reddit.weeklystats;

import jmk.reddit.util.RedditConnector;
import jmk.reddit.weeklystats.util.WeeklyStatsBase;

public class WeeklyStatisticsGenerator extends WeeklyStatsBase {
	
	public WeeklyStatisticsGenerator() {
		
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
                RedditConnector.getInstance().closeConnection();
                LOG.info("Connection closed.");
            }
        });
		new WeeklyStatisticsGenerator();
	}
}
