package jmk.reddit.analogbot.parser.node;

public class SpamNode {

	private long created = 0;
	private String bannedBy = "";
	private String author = "";
	
	public SpamNode()
	{
		
	}
	
	
	/**
	 * @return the created
	 */
	public long getCreated() {
		return created;
	}
	/**
	 * @param created the created to set
	 */
	public void setCreated(long created) {
		this.created = created;
	}
	/**
	 * @return the bannedBy
	 */
	public String getBannedBy() {
		return bannedBy;
	}
	/**
	 * @param bannedBy the bannedBy to set
	 */
	public void setBannedBy(String bannedBy) {
		this.bannedBy = bannedBy;
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
	
}
