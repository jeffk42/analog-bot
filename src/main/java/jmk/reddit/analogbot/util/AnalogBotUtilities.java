package jmk.reddit.analogbot.util;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AnalogBotUtilities {

	private static AnalogBotUtilities utils = null;
	
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
