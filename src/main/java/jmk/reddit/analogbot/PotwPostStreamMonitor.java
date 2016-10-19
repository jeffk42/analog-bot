/**
 * 
 */
package jmk.reddit.analogbot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jmk.reddit.analogbot.object.CurrentState;
import jmk.reddit.analogbot.object.PotwPost;
import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;

/**
 * @author jkraus
 *
 */
public class PotwPostStreamMonitor extends PostStreamMonitor {	

	private static final Logger LOG = Logger.getLogger(PotwPostStreamMonitor.class.getName());
	protected String LISTENER_REGEX = "^Weekly[\\s]{1}[']OTW[\\s]{1}Search[\\s]{1}Link['].*Week[\\s]{1}(\\d+)";
	
	public PotwPostStreamMonitor(AnalogBot parent, RedditClient client, String subreddit) {
		super(parent, client, subreddit);
	}
	
	@Override
	protected void handleIncomingPost(Submission post) {
		LOG.log(Level.INFO, "Found new post: "+post.getTitle());
		Pattern r = Pattern.compile(LISTENER_REGEX, Pattern.MULTILINE);
		Matcher m = r.matcher(post.getTitle());
		int weekNum = 0;
		
		if (m.find())
		{
			weekNum = Integer.parseInt(m.group(1));
			LOG.log(Level.INFO, "Post is for week "+weekNum);
			PotwPost potwPost = null;
			
			if (!serializedVersionExists(post, weekNum))
			{
				potwPost = createPotwPost(weekNum, post);
				potwPost.setPromptId(replyToPost(post, buildSelectorResponse(potwPost.getBody())));
				savePotwPost(potwPost);
			}
			else 
			{
				potwPost = restorePotwPost(post, weekNum);
			}
		}
		
		
	}
	
	protected String buildSelectorResponse(String body) {
		String response = "Please";
		Pattern r = Pattern.compile("^POTW rota \\- this week: (/u/[\\w]+)", Pattern.MULTILINE);
		Matcher m = r.matcher(body);
		String user = "";
		if (m.find())
		{
			
		}
		return response;
	}
	
	protected void savePotwPost(PotwPost post) {
		FileOutputStream fout;
		ObjectOutputStream oos;
		try {
			fout = new FileOutputStream(buildSerializedFilename(post));
			oos = new ObjectOutputStream(fout);
			oos.writeObject(post);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected PotwPost restorePotwPost(Submission s, int num)
	{
		PotwPost post = null;
		
		if (serializedVersionExists(s, num))
		{
			FileInputStream fin;
			ObjectInputStream ois;
			
			try {
				fin = new FileInputStream(buildSerializedFilename(s, num));
				ois = new ObjectInputStream(fin);
				post = (PotwPost) ois.readObject();
			}
			catch (FileNotFoundException fnfe) {
				// TODO Auto-generated catch block
				fnfe.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return post;
	}
	
	protected String buildSerializedFilename(Submission post, int weekNum)
	{
		return "temp/potw_"+weekNum+"_"+post.getCreated().getTime()+".ser";
	}
	
	protected String buildSerializedFilename(PotwPost post)
	{
		return "temp/potw_"+post.getPostWeekNumber()+"_"+post.getPostDate().getTime()+".ser";
	}
	
	protected boolean serializedVersionExists(Submission post, int weekNum) {
		File postFile = new File(buildSerializedFilename(post, weekNum));
		return postFile.exists();
	}
	
	protected PotwPost createPotwPost(int weekNum, Submission s) {
		
		PotwPost post = new PotwPost();
//		LOG.info("Post has "+s.getCommentCount()+" comments.");
//		
//		if (s.getCommentCount() > 0)
//		{
//			LOG.info(s.getDataNode().toString());
//			CommentNode topNode = client.getSubmission(s.getId()).getComments();
//			
//			if (topNode != null)
//			{
//				
//				FluentIterable<CommentNode> topNodeIterator = topNode.walkTree();
//				
//				if (topNodeIterator != null)
//				{
//					LOG.info("Post has comments.");
//		
//					//Iterator<CommentNode> comments = topCommentNode.iterator();
//					
//					for (CommentNode comment : topNodeIterator)
//					{
//						LOG.info("Found comment: "+comment.getComment().getBody());
//						if (comment.getComment().getAuthor().equals(client.getAuthenticatedUser()))
//						{
//							String body = comment.getComment().getBody().trim();
//							Pattern r = Pattern.compile("^([\\S]+).*");
//							Matcher m = r.matcher(body);
//							if (m.find())
//							{
//								LOG.log(Level.INFO, "Message found with state "+m.group(1));
//							}
//						}
//					}
//				}
//				else 
//				{	
//					post.setState(CurrentState.new_post);
//					LOG.info("topNodeIterator is null");
//				}
//			}
//			else LOG.info("topNode is null.");
//		}
//		else 
//		{
//			post.setState(CurrentState.new_post);
//			LOG.info("Found new post.");
//			
//		}
		post.setState(CurrentState.new_post);
		post.setPostId(s.getId());
		post.setPostUrl(s.getUrl());
		post.setWeekNumber(weekNum - 1);
		post.setPostWeekNumber(weekNum);
		post.setTitle(s.getTitle());
		post.setPostDate(s.getCreated());
		post.setAuthor(s.getAuthor());
		post.setBody(s.getSelftext());
		return post;
	}
	
	/**
	 * Reply to an existing post.
	 * 
	 * @param comment
	 * @param reply
	 */
	public String replyToPost(Submission post, String reply)
	{
		try {
			return parent.getAccount().reply(post, reply);
		} catch (NetworkException e) {
			e.printStackTrace();
		} catch (ApiException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	protected void performStartupTasks() {
		
		/* The thread was just started, so we need to determine if there is
		 * a POTW that's currently in progress.
		 */
		
	}
}
