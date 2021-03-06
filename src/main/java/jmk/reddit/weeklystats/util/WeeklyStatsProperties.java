package jmk.reddit.weeklystats.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class WeeklyStatsProperties extends Properties {
	
	private static final long serialVersionUID = 1L;
	private static WeeklyStatsProperties props = null;

	protected static final LogManager logManager = LogManager.getLogManager();
	protected static final Logger LOG = Logger.getLogger(WeeklyStatsProperties.class.getName());

	static
    {
        try {
            logManager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException exception) {
            LOG.log(Level.SEVERE, "Error in loading configuration",exception);
        }
    }
	
	protected WeeklyStatsProperties() {
		try {
			this.load(new FileInputStream(new File("analogbot.properties")));
		} catch (FileNotFoundException e) {
			LOG.log(Level.SEVERE, "File Not Found while loading properties file.", e);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "IO Error loading properties file", e);
		}
	}
	public static synchronized WeeklyStatsProperties getInstance() {
		if (props == null)
			props = new WeeklyStatsProperties();
		
		return props;
	}
}

