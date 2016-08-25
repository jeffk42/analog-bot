package jmk.reddit.analogbot.parser.node;

public class TrafficNode {

	private long date = 0;
	private String uniques = null;
	private String pageviews = null;
	private String subscriptions = null;
	
	public TrafficNode(long date, String uniques, String pageviews, String subscriptions)
	{
		this.date = date;
		this.uniques = uniques;
		this.pageviews = pageviews;
		this.subscriptions = subscriptions;
		
	}
	
	public TrafficNode() {
		
	}
	
	
	/**
	 * @return the date
	 */
	public long getDate() {
		return date;
	}
	/**
	 * @param date the date to set
	 */
	public void setDate(long date) {
		this.date = date;
	}
	/**
	 * @return the uniques
	 */
	public String getUniques() {
		return uniques;
	}
	/**
	 * @param uniques the uniques to set
	 */
	public void setUniques(String uniques) {
		this.uniques = uniques;
	}
	/**
	 * @return the pageviews
	 */
	public String getPageviews() {
		return pageviews;
	}
	/**
	 * @param pageviews the pageviews to set
	 */
	public void setPageviews(String pageviews) {
		this.pageviews = pageviews;
	}
	/**
	 * @return the subscriptions
	 */
	public String getSubscriptions() {
		return subscriptions;
	}
	/**
	 * @param subscriptions the subscriptions to set
	 */
	public void setSubscriptions(String subscriptions) {
		this.subscriptions = subscriptions;
	}
}
