package jmk.reddit.weeklystats.util;

import java.util.logging.Logger;

import jmk.reddit.util.UtilitiesProvider;

public class WeeklyStatsBase extends UtilitiesProvider {
	protected static final Logger LOG = Logger.getLogger(WeeklyStatsBase.class.getName());
	protected static final WeeklyStatsProperties properties = WeeklyStatsProperties.getInstance();
    
    public WeeklyStatsBase() {

    }
    
    /**
	 * @return the properties
	 */
    @Override
	public WeeklyStatsProperties getProperties() {
		return properties;
	}
	
}
