package me.Usoka.markov;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLiteDataHandler implements DataHandler {
	private String sqlDirectory;
	private Connection sqlDatabase;

	/**
	 * @param databaseDirectory directory of the SQLite database
	 */
	public SQLiteDataHandler(String databaseDirectory) {
		sqlDirectory = databaseDirectory;
		sqlDatabase = connect("jdbc:sqlite:"+ databaseDirectory);
	}

	/**
	 * Creates a connection to the SQLite Database
	 * @param url URL/Directory of the database
	 * @return the connection or <i>null</i> if connection fails
	 */
	private Connection connect(String url) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) { System.out.println("SQL Error: "+ e); }
		return conn;
	}

	@Override
	public int getLexiconSizeAll() {
		if (sqlDatabase == null) return 0;
		String query = "SELECT count(DISTINCT word) FROM user_lexicons";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public int getLexiconSizeFor(User user) {
		if (sqlDatabase == null) return 0;
		String blankQuery = "SELECT count(word) FROM user_lexicons WHERE userID = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public int getWordFrequencyAll(String word) {
		if (sqlDatabase == null) return 0;
		String blankQuery = "SELECT sum(frequency) FROM user_lexicons WHERE word = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setString(1, word);
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public int getWordFrequencyFor(String word, User user) {
		if (sqlDatabase == null) return 0;
		String blankQuery = "SELECT frequency FROM user_lexicons WHERE userID = ? AND word = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			prepState.setString(2, word);
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public List<String> getLexiconAll() {
		if (sqlDatabase == null) return new ArrayList<>();
		String query = "SELECT DISTINCT word FROM user_lexicons";

		List<String> lexicon = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) lexicon.add(rs.getString(1));
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return lexicon;
	}

	@Override
	public List<String> getLexiconFor(User user) {
		if (sqlDatabase == null) return new ArrayList<>();
		String query = "SELECT word FROM user_lexicons WHERE userID = ?";

		List<String> lexicon = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			prepState.setLong(1, user.getIdLong());
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) lexicon.add(rs.getString(1));
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return lexicon;
	}

	@Override
	public Map<String, Integer> getLinksAll(String word) {
		if (sqlDatabase == null) return new HashMap<>();
		String blankQuery = "SELECT links.endWord, sum(ALL user_links.frequency) FROM user_links " +
				"LEFT JOIN links ON user_links.linkID = links.linkID " +
				"WHERE links.startWord = ? GROUP BY links.endWord";

		Map<String, Integer> markovLinks = new HashMap<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setString(1, word.toLowerCase());

			ResultSet rs = prepState.executeQuery();
			while (rs.next()) {
				markovLinks.put(rs.getString(1), rs.getInt(2));
			}
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return markovLinks;
	}

	@Override
	public Map<String, Integer> getLinksFor(User user, String word) {
		if (sqlDatabase == null) return new HashMap<>();
		String blankQuery = "SELECT links.endWord, sum(ALL user_links.frequency) FROM users " +
				"LEFT JOIN user_links ON users.userID = user_links.userID " +
				"LEFT JOIN links ON user_links.linkID = links.linkID " +
				"WHERE users.userID = ? AND links.startWord = ? GROUP BY links.endWord";

		Map<String, Integer> markovLinks = new HashMap<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			prepState.setString(2, word.toLowerCase());

			ResultSet rs = prepState.executeQuery();
			while (rs.next()) {
				markovLinks.put(rs.getString(1), rs.getInt(2));
			}
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return markovLinks;
	}
}
