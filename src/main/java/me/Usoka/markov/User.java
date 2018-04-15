package me.Usoka.markov;

public class User {
	private final long id;
	private final String name;
	private final short discriminator;

	public User(long id, String name, String discriminator) {
		this.id = id;
		this.name = name;
		this.discriminator = Short.parseShort(discriminator);
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

	/**
	 * Get the discriminator of the user
	 */
	public String getDiscriminator() { return String.format("%04d", discriminator); }
}
