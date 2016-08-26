package jmk.reddit.weeklystats;

import java.util.logging.Logger;

import jmk.reddit.util.UtilitiesProvider;
import jmk.reddit.weeklystats.util.WeeklyStatsProperties;

public class StatUtilsProvider extends UtilitiesProvider {
	protected static final Logger LOG = Logger.getLogger(StatUtilsProvider.class.getName());
	protected static final WeeklyStatsProperties properties = WeeklyStatsProperties.getInstance();
    
    public StatUtilsProvider() {

    }
    
    /**
	 * @return the properties
	 */
    @Override
	public WeeklyStatsProperties getProperties() {
		return properties;
	}
	
}
