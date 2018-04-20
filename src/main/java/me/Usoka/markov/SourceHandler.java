package me.Usoka.markov;

import java.util.List;

public interface SourceHandler {

	/**
	 * Save a message to the source
	 * @param message message to be saved
	 * @return if it was saved successfully
	 */
	boolean saveMessage(Message message);

	/**
	 * Update a specified message in the source.
	 * Will also add the message if it didn't actually exist
	 * @param message updated version of the message
	 * @return if it was successfully updated
	 */
	boolean updateMessage(Message message);

	/**
	 * Update multiple messages in the source at once.
	 * Will add any messages which didn't previously exist
	 * @param messages List of messages that should be updated
	 * @return if updating messages was successful
	 */
	boolean updateMessages(List<Message> messages);

	/**
	 * Remove a specified message from the source
	 * @param messageID ID (as String) of message to delete
	 * @return if it was deleted successfully
	 */
	boolean deleteMessage(String messageID);

	/**
	 * Identify if a message, by given ID, already existing in the source
	 * @param messageID ID (as String) of message to check for
	 * @return if the message exist
	 * @throws Exception if there is an error reading from the source
	 */
	boolean containsMessageID(String messageID) throws Exception;

	/**
	 * Saves a specified user to the source data.
	 * Will also replace an existing user if ID matches
	 * @param user User to save to source
	 * @return if the user was saved successfully
	 */
	boolean saveUser(User user);

	/**
	 * Checks if a user, by given ID, exists in the source data
	 * @param userID user ID (as String) to check for
	 * @return if the source data contains a user with matching ID
	 * @throws Exception if there is an error reading from the source
	 */
	boolean containsUserID(String userID) throws Exception;

	/**
	 * Gets the String ID of the most recent message in the source data
	 * @return the ID of the most recent message (as String)
	 * @throws Exception if there is an error reading from the source
	 */
	String getMostRecentMessageID() throws Exception;

	/**
	 * Counts the number of messages in the source data for a specified user
	 * @param user specified user to count the messages for
	 * @return number of messages which the user has sent
	 * @throws Exception if there is an error reading from the source
	 */
	int countMessagesFromUser(User user) throws Exception;
}
