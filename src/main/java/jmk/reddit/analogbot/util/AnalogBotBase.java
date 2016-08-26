package jmk.reddit.analogbot.util;

import java.util.logging.Logger;

import jmk.reddit.util.UtilitiesProvider;

public class AnalogBotBase extends UtilitiesProvider {

	protected static final AnalogBotProperties properties = AnalogBotProperties.getInstance();
	protected static final AnalogBotUtilities utilities = AnalogBotUtilities.getInstance();
	protected static final Logger LOG = Logger.getLogger(AnalogBotBase.class.getName());
    
    public AnalogBotBase() {

    }
    
    /**
	 * @return the properties
	 */
    @Override
	public AnalogBotProperties getProperties() {
		return properties;
	}
	
}
