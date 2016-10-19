/**
 * 
 */
package jmk.reddit.analogbot.potw;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jmk.reddit.analogbot.AnalogBot;
import jmk.reddit.util.RedditConnector;
import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

/**
 * @author jkraus
 *
 */
public class PotwMessageHandler {

	private static PotwMessageHandler instance = null;
	public static String CONFIRMATION_SUBJECT_TEXT = "Please Confirm Your Selection:";
	public static String POTW_WINNER_SELECT_SUBJECT_TEXT = "POTW Interview Request:";
	public static synchronized PotwMessageHandler getInstance() {
		if (instance == null)
		{
			instance = new PotwMessageHandler();	
		}
		
		return instance;
	}
	
	public static void handlePotwMessage(Message m, AnalogBot parent)
	{
		String messageId = m.getId();
		String parentId = m.getParentId();
		String postId = null;
		int entryNum = -1;
		String entryUser = null;
		boolean validMessage = false;
		int lastWeek = -1;
				
		if (parentId == null)
		{
			// this is a top-level message.
			// Step 1. Verify this is real.
			if (m.getSubject().contains("Request:"))
				postId = m.getSubject().split("Request:")[1].trim();
			Submission potwPost = RedditConnector.getInstance().getClient().getSubmission(postId);
			
			if (potwPost.getTitle().startsWith("Weekly 'OTW Search Link'"))
			{
				validMessage = true;
				String weekNumStr = null;
				if (potwPost.getTitle().contains("Week "))
					weekNumStr = potwPost.getTitle().split("Week ")[1];
				lastWeek = Integer.parseInt(weekNumStr);
			}
			
			if (validMessage)
			{
				String update = "Update #1: A message has been received from "+m.getAuthor()+" marking the "+
						"following entry for POTW: \n\n";
				
				String [] lines = m.getBody().split("\n");
				for (String line : lines)
					update += "* "+line+"\n";
				
				updateStatus(parent, potwPost, update);
				if (lines.length > 0 && lines[0].contains("Entry "))
				{
					entryNum = Integer.parseInt(lines[0].split("Entry ")[1]);
				}
					
				if (entryNum > 0)
				{
					if (lines.length > 1 && lines[1].trim().startsWith("/u/"))
					{
						entryUser = lines[1].trim();
					}
				}
				
				// only email if information is legit.
				if (entryNum > 0 && !entryUser.isEmpty())
				{
					String subject = CONFIRMATION_SUBJECT_TEXT +" "+ postId +" #" + entryNum;
					String body = "Hello!\n\n";
					
					body += "You have selected the following entry as the winner of the POTW in the Week "+
							lastWeek+" Post:\n\n";
					body += "* Entry #"+entryNum+"\n";
					body += "* User: "+entryUser+"\n\n";
					body += "Please confirm that this is the user that you would like to send the winner notification to by replying to this message with **Y** or **N**.";
					
					try {
						parent.getInbox().compose(m.getAuthor(), subject, body);
						updateStatus(parent,potwPost,"Update #2: Confirmation mail sent to "+m.getAuthor()+".");
					} catch (NetworkException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ApiException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
			}
		}
		else if (m.getSubject().startsWith("re: "+CONFIRMATION_SUBJECT_TEXT))
		{
			boolean sendConfirmed = m.getBody().trim().startsWith("Y") || m.getBody().trim().startsWith("y");
			
			if (sendConfirmed)
			{
				String pid = "";
				int num = -1;
				
				if (m.getSubject().contains(CONFIRMATION_SUBJECT_TEXT))
				{
					Pattern r = Pattern.compile(CONFIRMATION_SUBJECT_TEXT+"[\\s]*([a-zA-Z0-9]+)\\s#([\\d]+)");
					Matcher mtc = r.matcher(m.getSubject());
					
					if (mtc.find())
					{
						pid = mtc.group(1);
						num = Integer.parseInt(mtc.group(2));
					}
				}
				if (!pid.isEmpty() && num > 0)
				{
					Submission potwPost = RedditConnector.getInstance().getClient().getSubmission(pid);
					Pattern r = Pattern.compile("^"+num+"\\s\\|\\s[\\d]+\\s\\|\\s\\[([\\w\\-]+)\\]\\(.*?\\)\\s\\|\\s\\[(.*?)\\]\\((.*?)\\)", Pattern.MULTILINE);
					Matcher mtc = r.matcher(potwPost.getSelftext());
					
					if (mtc.find())
					{
						String user = mtc.group(1);
						String link = mtc.group(3);
						int searchWeek = -1;
						r = Pattern.compile("^Use this.*?Week\\s(\\d+)",Pattern.MULTILINE);
						mtc = r.matcher(potwPost.getSelftext());
						if (mtc.find())
						{
							searchWeek = Integer.parseInt(mtc.group(1));
						}
						
						String update = "Update 3: Sending interview template to "+user+" for POTW Week "+searchWeek+"."
								+ " If I did it for real, it would look like this:\n\n\n-----\n\n";
						String template = readFileAsString("stats/potwTemplate.txt");
						template = template.replace("**link**", link);
						template = template.replace("**mod**", "/u/"+m.getAuthor());
						
						update += template;
						updateStatus(parent, potwPost, update);
						
						
					}
				}
			}
			//else updateStatus(parent,potwPost,"Update #3: Selected entry was not ")
		}
	}
	private static String readFileAsString(String filePath) {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader;
		try {
			reader = new BufferedReader(
			        new FileReader(filePath));
			char[] buf = new char[1024];
	        int numRead=0;
	        while((numRead=reader.read(buf)) != -1){
	            String readData = String.valueOf(buf, 0, numRead);
	            fileData.append(readData);
	        }
	        reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return fileData.toString();
    }
	private static void updateStatus(AnalogBot parent, Submission potwPost, String update)
	{
		try {
			parent.getAccount().reply(potwPost, update);
		} catch (NetworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
