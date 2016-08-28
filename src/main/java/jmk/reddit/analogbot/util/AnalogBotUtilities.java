package jmk.reddit.analogbot.util;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalogBotUtilities extends AnalogBotBase {

	private static AnalogBotUtilities utils = null;
	protected static final Logger LOG = Logger.getLogger(AnalogBotUtilities.class.getName());
	
	public static synchronized AnalogBotUtilities getInstance() {
		if (utils == null)
			utils = new AnalogBotUtilities();
		
		return utils;
	}
	
	public synchronized void changeConnectionState(boolean running)
	{
		try {
			OutputStream os = new BufferedOutputStream(
			        new FileOutputStream("state"));
			
			if (running)
			{
				os.write("connected".getBytes());
			}
			else
			{
				os.write("disconnected".getBytes());
			}
			
			os.flush();
			
			os.close();
		} catch (FileNotFoundException e) {
			LOG.log(Level.WARNING, "There was a problem accessing the state file.", e);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "There was a problem accessing the state file.", e);
		}
	}
}
