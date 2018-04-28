package me.Usoka.markov;

public class User {
	private final long id;
	private final String name;

	public User(long id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Get the user ID as a string
	 */
	public String getId() { return Long.toUnsignedString(id); }

	/**
	 * Get the user ID as a long
	 */
	public long getIdLong() { return id; }

	/**
	 * Get the name of the user
	 */
	public String getName() { return name; }
}
