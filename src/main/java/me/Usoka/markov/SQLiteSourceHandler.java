package me.Usoka.markov;

import com.sun.istack.internal.NotNull;

import java.sql.*;
import java.util.List;

public class SQLiteSourceHandler implements SourceHandler {
	private Connection sqlDatabase;

	/**
	 * @param databaseDirectory directory for the SQLite database
	 * @throws SQLException if a database access error occurs when establishing the connection
	 */
	public SQLiteSourceHandler(@NotNull String databaseDirectory) throws SQLException{
		if (databaseDirectory.equals("")) throw new IllegalArgumentException("Database directory cannot be empty String");
		sqlDatabase = DriverManager.getConnection(databaseDirectory);
	}

	@Override
	public boolean saveMessage(Message message) {
		if (message == null) return false;
		String blankMessagesQuery = "INSERT OR IGNORE INTO messages (messageID, content) VALUES (?,?)";
		String blankLinkQuery = "INSERT OR IGNORE INTO user_messages (userID, messageID) VALUES (?,?)";

		try {
			PreparedStatement messagesPrepState = sqlDatabase.prepareStatement(blankMessagesQuery);
			PreparedStatement linkPrepState = sqlDatabase.prepareStatement(blankLinkQuery);

			//Set values in query
			messagesPrepState.setLong(1, message.getIdLong());
			messagesPrepState.setString(2, message.getContentCleaned());
			linkPrepState.setLong(1, message.getAuthor().getIdLong());
			linkPrepState.setLong(2, message.getIdLong());

			//Execute the queries
			messagesPrepState.executeUpdate();
			linkPrepState.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQL Error: "+ e);
			return false;
		}

		return true;
	}

	@Override
	public boolean updateMessage(Message message) {
		if (message == null) return false;
		String blankQuery = "REPLACE INTO messages (messageID, content) VALUES (?,?)";

		try {
			PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery);
			prepStatement.setLong(1, message.getIdLong());
			prepStatement.setString(2, message.getContentCleaned());
			prepStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQL Error: "+ e);
			return false;
		}

		return true;
	}

	@Override
	public boolean updateMessages(List<Message> messages) {
		if (messages == null) return false;
		String messagesBlank = "REPLACE INTO messages (messageID, content) VALUES (?,?)";
		String user_messagesBlank = "REPLACE INTO user_messages (userID, messageID) VALUES (?,?)";

		try {
			sqlDatabase.setAutoCommit(false);

			//Create the prepared statements for each of the messages
			for (Message message : messages) {
				if (message == null || message.getContentCleaned().equals("")) continue;
				PreparedStatement messagesPrep = sqlDatabase.prepareStatement(messagesBlank);
				messagesPrep.setLong(1, message.getIdLong());
				messagesPrep.setString(2, message.getContentCleaned());

				messagesPrep.executeUpdate();

				PreparedStatement user_messagesPrep = sqlDatabase.prepareStatement(user_messagesBlank);
				user_messagesPrep.setLong(1, message.getAuthor().getIdLong());
				user_messagesPrep.setLong(2, message.getIdLong());

				user_messagesPrep.executeUpdate();
			}

			sqlDatabase.commit();
			sqlDatabase.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println("SQL Error: "+ e);
			return false;
		}

		return true;
	}

	@Override
	public boolean deleteMessage(String messageID) {
		if (messageID == null) return false;
		String messagesBlank = "DELETE FROM messages WHERE messageID = ?";
		String user_messagesBlank = "DELETE FROM user_messages WHERE messageID = ?";

		try {
			sqlDatabase.setAutoCommit(false);

			//Create the prep statements
			PreparedStatement messagesPrep = sqlDatabase.prepareStatement(messagesBlank);
			PreparedStatement user_messagesPrep = sqlDatabase.prepareStatement(user_messagesBlank);

			messagesPrep.setLong(1, Long.parseUnsignedLong(messageID));
			user_messagesPrep.setLong(1, Long.parseUnsignedLong(messageID));

			messagesPrep.executeUpdate();
			user_messagesPrep.executeUpdate();

			sqlDatabase.commit();
			sqlDatabase.setAutoCommit(true);
		} catch (SQLException e) {
			System.out.println("SQL Error: "+ e);
			return false;
		}

		return false;
	}

	@Override
	public boolean containsMessageID(String messageID) throws SQLException {
		if (messageID == null) return false;
		String blankQuery = "SELECT EXISTS ( SELECT * FROM messages WHERE messageID = ?)";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery)) {
			prepStatement.setLong(1, Long.parseLong(messageID));

			return prepStatement.executeQuery().getBoolean(1);
		} catch (SQLException e) {System.out.println("SQL Error: "+ e);}
		throw new SQLException("failed to read from database");
	}

	@Override
	public boolean saveUser(User user) {
		if (user == null) return false;
		String blankQuery = "REPLACE INTO users (userID, username, discriminator) VALUES (?,?,?)";

		try {
			PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery);

			prepStatement.setLong(1, user.getIdLong());
			prepStatement.setString(2, user.getName());
			prepStatement.setInt(3, Integer.parseInt(user.getDiscriminator()));

			prepStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQL Error: "+ e);
			return false;
		}

		return true;
	}

	@Override
	public boolean containsUserByID(String userID) throws SQLException {
		if (userID == null) return false;
		String blankQuery = "SELECT EXISTS ( SELECT * FROM users WHERE userID = ?)";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery)) {
			prepStatement.setLong(1, Long.parseLong(userID));

			return prepStatement.executeQuery().getBoolean(1);
		} catch (SQLException e) {System.out.println("SQL Error: "+ e);}
		throw new SQLException("failed to read from database");
	}

	@Override
	public String getMostRecentMessageID() throws SQLException {
		String query = "SELECT max(messageID) FROM messages";
		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(query)) {
			return prepStatement.executeQuery().getString(1);
		} catch (SQLException e) {System.out.println("SQL Error: "+ e);}
		throw new SQLException("failed to read from database");
	}

	@Override
	public int countMessagesFrom(@NotNull User user) throws SQLException{
		String blankQuerry = "SELECT count(*) FROM users " +
				"LEFT JOIN user_messages ON users.userID = user_messages.userID " +
				"LEFT JOIN messages ON user_messages.messageID = messages.messageID " +
				"WHERE users.userID = ?";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuerry)) {
			prepStatement.setLong(1, user.getIdLong());
			return prepStatement.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("SQL Error: "+ e); }

		throw new SQLException("Failed to read from database");
	}
}
