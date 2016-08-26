package jmk.reddit.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class AnalogBotBase {
	protected static final LogManager logManager = LogManager.getLogManager();
	protected static final Logger LOG = Logger.getLogger(AnalogBotBase.class.getName());
	protected static final AnalogBotProperties properties = AnalogBotProperties.getInstance();
	protected static final AnalogBotUtilities utilities = AnalogBotUtilities.getInstance();

	static
    {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException exception) {
            LOG.log(Level.SEVERE, "Error in loading configuration",exception);
        }

    }
    
    public AnalogBotBase() {

    }
    
    /**
	 * @return the properties
	 */
	public AnalogBotProperties getProperties() {
		return properties;
	}

	/**
	 * @return the logmanager
	 */
	public static LogManager getLogmanager() {
		return logManager;
	}

	/**
	 * @return the log
	 */
	public static Logger getLog() {
		return LOG;
	}
	
}
