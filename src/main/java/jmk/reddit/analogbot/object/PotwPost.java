/**
 * 
 */
package jmk.reddit.analogbot.object;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jkraus
 *
 */
public class PotwPost implements Serializable {

	private int weekNumber = -1;
	private int postWeekNumber = -1;
	private String title = null;
	private String body = null;
	private String author = null;
	private Date postDate = null;
	private String postId = null;
	private int potwSelection = -1;
	private String promptId = null;
	private String postUrl = null;

	private CurrentState state = null;
	
	/**
	 * 
	 */
	public PotwPost() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @return the state
	 */
	public CurrentState getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(CurrentState state) {
		this.state = state;
	}

	/**
	 * @return the weekNumber
	 */
	public int getWeekNumber() {
		return weekNumber;
	}

	/**
	 * @param weekNumber the weekNumber to set
	 */
	public void setWeekNumber(int weekNumber) {
		this.weekNumber = weekNumber;
	}

	public int getPostWeekNumber() {
		return postWeekNumber;
	}

	public void setPostWeekNumber(int postWeekNumber) {
		this.postWeekNumber = postWeekNumber;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @param body the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the postDate
	 */
	public Date getPostDate() {
		return postDate;
	}

	/**
	 * @param postDate the postDate to set
	 */
	public void setPostDate(Date postDate) {
		this.postDate = postDate;
	}

	/**
	 * @return the postId
	 */
	public String getPostId() {
		return postId;
	}

	/**
	 * @param postId the postId to set
	 */
	public void setPostId(String postId) {
		this.postId = postId;
	}

	/**
	 * @return the potwSelection
	 */
	public int getPotwSelection() {
		return potwSelection;
	}

	/**
	 * @param potwSelection the potwSelection to set
	 */
	public void setPotwSelection(int potwSelection) {
		this.potwSelection = potwSelection;
	}

	/**
	 * @return the promptId
	 */
	public String getPromptId() {
		return promptId;
	}

	/**
	 * @param promptId the promptId to set
	 */
	public void setPromptId(String promptId) {
		this.promptId = promptId;
	}

	/**
	 * @return the postUrl
	 */
	public String getPostUrl() {
		return postUrl;
	}

	/**
	 * @param postUrl the postUrl to set
	 */
	public void setPostUrl(String postUrl) {
		this.postUrl = postUrl;
	}
	
	

}
