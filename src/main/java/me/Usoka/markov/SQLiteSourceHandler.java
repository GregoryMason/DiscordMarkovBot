package me.Usoka.markov;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteSourceHandler implements SourceHandler {
	private Connection sqlDatabase;

	/**
	 * @param databaseDirectory directory for the SQLite database
	 * @throws SQLException if a database access error occurs when establishing the connection
	 */
	public SQLiteSourceHandler(@NotNull String databaseDirectory) throws SQLException{
		if (databaseDirectory.equals("")) throw new IllegalArgumentException("Database directory cannot be empty String");
		sqlDatabase = DriverManager.getConnection("jdbc:sqlite:"+ databaseDirectory);
		if (sqlDatabase == null) throw new IllegalArgumentException("Could not open connection: connection was null");
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
			System.err.println("SQLException in saveMessage: "+ e);
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
			System.err.println("SQLException in updateMessage: "+ e);
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
			System.err.println("SQLException in updateMessages: "+ e);
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
			System.err.println("SQLException in deleteMessage: "+ e);
			return false;
		}

		return false;
	}

	@Override
	public boolean containsMessageID(String messageID) throws IOException {
		if (messageID == null) return false;
		String blankQuery = "SELECT EXISTS ( SELECT * FROM messages WHERE messageID = ?)";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery)) {
			prepStatement.setLong(1, Long.parseLong(messageID));

			return prepStatement.executeQuery().getBoolean(1);
		} catch (SQLException e) {
			throw new IOException("Database read failed", e);
		}
	}

	@Override
	public boolean saveUser(User user) {
		if (user == null) return false;
		String blankQuery = "REPLACE INTO users (userID, username) VALUES (?,?)";

		try {
			PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery);

			prepStatement.setLong(1, user.getIdLong());
			prepStatement.setString(2, user.getName());

			prepStatement.executeUpdate();
		} catch (SQLException e) {
			System.err.println("SQLException in saveUser: "+ e);
			return false;
		}

		return true;
	}

	@Override
	public boolean containsUserByID(String userID) throws IOException {
		if (userID == null) return false;
		String blankQuery = "SELECT EXISTS ( SELECT * FROM users WHERE userID = ?)";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery)) {
			prepStatement.setLong(1, Long.parseLong(userID));

			return prepStatement.executeQuery().getBoolean(1);
		} catch (SQLException e) {
			throw new IOException("Database read failed", e);
		}
	}

	@Override
	public String getMostRecentMessageID() throws IOException {
		String query = "SELECT max(messageID) FROM messages";
		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(query)) {
			return prepStatement.executeQuery().getString(1);
		} catch (SQLException e) {
			throw new IOException("Database read failed", e);
		}
	}

	@Override
	public List<Message> getMessagesContaining(String subString) throws IOException{
		String queryBlank = "SELECT users.userID, users.username, messages.messageID, messages.content " +
				"FROM users LEFT JOIN user_messages ON users.userID = user_messages.userID " +
				"LEFT JOIN messages ON user_messages.messageID = messages.messageID " +
				"WHERE content LIKE ? ESCAPE '\\' ORDER BY messages.messageID";

		List<Message> compiledList = new ArrayList<>();
		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(queryBlank)) {
			//Escape wild-card characters from the substring
			subString = subString.replaceAll("\\\\", "\\\\\\\\");
			subString = subString.replaceAll("%", "\\\\%").replaceAll("_", "\\\\_");
			prepStatement.setString(1,"%"+ subString +"%");

			ResultSet rs = prepStatement.executeQuery();
			while (rs.next()) {
				compiledList.add(new Message(
						rs.getLong(3),
						rs.getString(4),
						new User(rs.getLong(1), rs.getString(2))
				));
			}
		} catch (SQLException e) {
			throw new IOException("Database read failed", e);
		}

		return compiledList;
	}

	@Override
	public int countMessagesFrom(@NotNull User user) throws IOException{
		String blankQuery = "SELECT count(*) FROM users " +
				"LEFT JOIN user_messages ON users.userID = user_messages.userID " +
				"LEFT JOIN messages ON user_messages.messageID = messages.messageID " +
				"WHERE users.userID = ?";

		try (PreparedStatement prepStatement = sqlDatabase.prepareStatement(blankQuery)) {
			prepStatement.setLong(1, user.getIdLong());
			return prepStatement.executeQuery().getInt(1);
		} catch (SQLException e) {
			throw new IOException("Database read failed", e);
		}
	}
}
