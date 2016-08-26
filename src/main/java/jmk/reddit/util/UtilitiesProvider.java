package jmk.reddit.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public abstract class UtilitiesProvider {
	protected static final LogManager logManager = LogManager.getLogManager();
	protected static final Logger LOG = Logger.getLogger(UtilitiesProvider.class.getName());

	static
    {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException exception) {
            LOG.log(Level.SEVERE, "Error in loading configuration",exception);
        }

    }
    
    public UtilitiesProvider() {

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
	
	public abstract Properties getProperties();
	
}
