package me.Usoka.markov;

import java.util.regex.Pattern;

public class Message {
	/**
	 * The maximum amount of characters that can be sent in one message. ({@value})
	 * <br>Only applies to the raw content!
	 */
	int MAX_CONTENT_LENGTH = 2000;

	/**
	 * Pattern used to find links in messages.
	 */
	private Pattern LINK_PATTERN = Pattern.compile("((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", Pattern.CASE_INSENSITIVE);

	/**
	 * Pattern used to find escaped links in messages.
	 */
	private Pattern ESCAPED_LINK_PATTERN = Pattern.compile("<((https?|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)>", Pattern.CASE_INSENSITIVE);

	/**
	 * Pattern used to identify commands that should be ignored
	 */
	private Pattern COMMAND_WORDS = Pattern.compile("^!(" +
			"(" + //Commands with parameters that follow it
			"bet|" +										//Blackjack bot
			"playlist|play playlist|" +						//Music bot
			"quote|count|markov|context|source|setuser" +	//Markov bot
			")(\\s+.*|$)|" +
			"(" + //Commands which are only the command
			"start|hit|bet|login|check|logincreation|" +				//Blackjack bot
			"queue|nowplaying|playlists|forceskip|skip|shuffle|stop|" +	//Music bot
			"speak|gethistory|word" +									//Markov bot
			")$)", Pattern.CASE_INSENSITIVE);

	private final long id;
	private final String content;
	private final User author;

	public Message(long id, String content, User author) {
		this.id = id;
		this.content = content;
		this.author = author;
	}

	/**
	 * Get the message ID as a string
	 */
	public String getId() { return Long.toUnsignedString(id); }

	/**
	 * Get the message ID as a long
	 */
	public long getIdLong() { return id; }

	/**
	 * Get the user who wrote this message
	 */
	public User getAuthor() { return author; }

	/**
	 * Get the raw unfiltered content of the message
	 */
	public String getContentRaw() { return content; }

	/**
	 * Returns content but without unwanted links or key-words
	 */
	public String getContentCleaned() {
		if (content == null) return "";
		String cleanContent = content;

		cleanContent = COMMAND_WORDS.matcher(cleanContent).replaceAll(" ");
		cleanContent = ESCAPED_LINK_PATTERN.matcher(cleanContent).replaceAll(" ");
		cleanContent = LINK_PATTERN.matcher(cleanContent).replaceAll("");

		//Remove excess spaces TODO make it not remove newline characters
		cleanContent = cleanContent.replaceAll("\\s+", " ").trim();

		return cleanContent;
	}
}
