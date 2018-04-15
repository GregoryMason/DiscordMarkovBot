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
	 * Update multiple messages in the source.
	 * Will add the messages if any don't exist
	 * @param messages List of messages that should be updated
	 * @return if updating messages was successful
	 */
	boolean updateMessages(List<Message> messages);

	/**
	 * Remove a specified message from the source
	 * @param messageID ID of message to delete
	 * @return if it was successfully deleted
	 */
	boolean deleteMessage(String messageID);

	/**
	 * Identify if a message is already saved, based on ID
	 * @param messageID ID of the message
	 * @return if the message exist
	 */
	boolean containsMessageID(String messageID) throws Exception;

	boolean saveUser(User user);


	boolean containsUserID(String userID) throws Exception;

	String getMostRecentMessageID() throws Exception;

	int countMessagesFromUser(User user) throws Exception;
}
