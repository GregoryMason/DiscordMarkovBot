package me.Usoka.markov;

import com.sun.istack.internal.NotNull;

import java.sql.*;
import java.util.*;

public class SQLiteDataHandler implements DataHandler {
	private Connection sqlDatabase;

	/**
	 * @param databaseDirectory directory of the SQLite database
	 * @throws SQLException if a database access error occurs when establishing the connection
	 */
	public SQLiteDataHandler(@NotNull String databaseDirectory) throws SQLException{
		if (databaseDirectory.equals("")) throw new IllegalArgumentException("Database directory cannot be empty String");
		sqlDatabase = DriverManager.getConnection("jdbc:sqlite:"+ databaseDirectory);
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
	public int getLexiconSizeFor(@NotNull User user) {
		if (sqlDatabase == null) return 0;
		String blankQuery = "SELECT count(word) FROM user_lexicons WHERE userID = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public int getWordFrequencyAll(@NotNull String word) {
		if (sqlDatabase == null) return 0;
		String blankQuery = "SELECT sum(frequency) FROM user_lexicons WHERE word = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setString(1, word);
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }
		return 0;
	}

	@Override
	public int getWordFrequencyFor(@NotNull String word, @NotNull User user) {
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
	public Set<String> getLexiconAll() {
		if (sqlDatabase == null) return new HashSet<>();
		String query = "SELECT DISTINCT word FROM user_lexicons";

		Set<String> lexicon = new HashSet<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) lexicon.add(rs.getString(1));
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return lexicon;
	}

	@Override
	public Set<String> getLexiconFor(@NotNull User user) {
		if (sqlDatabase == null) return new HashSet<>();
		String query = "SELECT word FROM user_lexicons WHERE userID = ?";

		Set<String> lexicon = new HashSet<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			prepState.setLong(1, user.getIdLong());
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) lexicon.add(rs.getString(1));
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return lexicon;
	}

	@Override
	public List<String> getAllWordsAll() {
		String query = "SELECT word, frequency FROM user_lexicons";

		List<String> allWords = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) for (int i = 0; i < rs.getInt(2); i++) {
				allWords.add(rs.getString(1));
			}
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return allWords;
	}

	@Override
	public List<String> getAllWordsFor(User user) {
		String blankQuery = "SELECT word, frequency FROM user_lexicons WHERE userID = ?";

		List<String> allWords = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) for (int i = 0; i < rs.getInt(2); i++) {
				allWords.add(rs.getString(1));
			}
			rs.close();
		} catch (SQLException e) { System.out.println("Failed to read from database: "+ e); }

		return allWords;
	}

	@Override
	public Map<String, Integer> getLinksAll(@NotNull String word) {
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
	public Map<String, Integer> getLinksFor(@NotNull User user, @NotNull String word) {
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
