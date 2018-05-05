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
		if (sqlDatabase == null) throw new IllegalArgumentException("Could not open connection: connection was null");
	}

	@Override
	public int getLexiconSizeAll() {
		String query = "SELECT count(DISTINCT word) FROM user_lexicons";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) {
			System.err.println("SQLException in getLexiconSizeAll: "+ e);
		}
		return 0;
	}

	@Override
	public int getLexiconSizeFor(@NotNull User user) {
		String blankQuery = "SELECT count(word) FROM user_lexicons WHERE userID = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) {
			System.err.println("SQLException in getLexiconSizeFor: "+ e);
		}
		return 0;
	}

	@Override
	public int getWordFrequencyAll(@NotNull String word) {
		String blankQuery = "SELECT sum(frequency) FROM user_lexicons WHERE word = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setString(1, word);
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) {
			System.err.println("SQLException in getWordFrequencyAll: "+ e);
		}
		return 0;
	}

	@Override
	public int getWordFrequencyFor(@NotNull String word, @NotNull User user) {
		String blankQuery = "SELECT frequency FROM user_lexicons WHERE userID = ? AND word = ?";

		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			prepState.setString(2, word);
			return prepState.executeQuery().getInt(1);
		} catch (SQLException e) {
			System.err.println("SQLException in getWordFrequencyFor: "+ e);
		}
		return 0;
	}

	@Override
	public Set<String> getLexiconAll() {
		String query = "SELECT DISTINCT word FROM user_lexicons";

		Set<String> lexicon = new HashSet<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) {
				String item = rs.getString(1);
				//Make sure to not add empty strings
				if (!item.matches("^(\\s+)?$")) lexicon.add(item);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("SQLException in getLexiconAll: "+ e);
		}

		return lexicon;
	}

	@Override
	public Set<String> getLexiconFor(@NotNull User user) {
		String query = "SELECT word FROM user_lexicons WHERE userID = ?";

		Set<String> lexicon = new HashSet<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			prepState.setLong(1, user.getIdLong());
			ResultSet rs = prepState.executeQuery();
			while (rs.next()) {
				String item = rs.getString(1);
				//Make sure to not add empty strings
				if (!item.matches("^(\\s+)?$")) lexicon.add(item);
			}
			rs.close();
		} catch (SQLException e) {
			System.err.println("SQLException in getLexiconFor: "+ e);
		}

		return lexicon;
	}

	@Override
	public List<String> getAllWordsAll() {
		String query = "SELECT word, frequency FROM user_lexicons";

		List<String> allWords = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(query)) {
			allWords = getListFromResultSet(prepState.executeQuery());
		} catch (SQLException e) {
			System.err.println("SQLException in getAllWordsAll: "+ e);
		}

		return allWords;
	}

	@Override
	public List<String> getAllWordsFor(User user) {
		String blankQuery = "SELECT word, frequency FROM user_lexicons WHERE userID = ?";

		List<String> allWords = new ArrayList<>();
		try (PreparedStatement prepState = sqlDatabase.prepareStatement(blankQuery)) {
			prepState.setLong(1, user.getIdLong());
			allWords = getListFromResultSet(prepState.executeQuery());
		} catch (SQLException e) {
			System.err.println("SQLException in getAllWordsFor: "+ e);
		}

		return allWords;
	}

	/**
	 * Gets a <code>String</code> {@link List} from a given <code>ResultSet</code>. Adds each given string to
	 * the list as many times as is specified.
	 * @param rs The result set which the list should be built from. <br/>
	 *           <code>ResultSet</code> should be in the format:<ul>
	 *           <li>Column 1: <code>String</code> to be added to list</li>
	 *           <li>Column 2: <code>int</code> representing how many times it should be added</li></ul>
	 *           Any other columns will be ignored.
	 * @return <code>List</code> built from the result set
	 * @throws SQLException if an exception is thrown by a method called on the result set
	 */
	private List<String> getListFromResultSet(@NotNull ResultSet rs) throws SQLException {
		List<String> returnList = new ArrayList<>();

		while (rs.next()) for (int i = 0; i < rs.getInt(2); i++) {
			String item = rs.getString(1);
			//Make sure to not add empty strings
			if (!item.matches("^(\\s+)?$")) returnList.add(item);
		}
		rs.close();
		return returnList;
	}

	@Override
	public Map<String, Integer> getLinksAll(@NotNull String word) {
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
		} catch (SQLException e) {
			System.err.println("SQLException in getLinksAll: "+ e);
		}

		return markovLinks;
	}

	@Override
	public Map<String, Integer> getLinksFor(@NotNull User user, @NotNull String word) {
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
		} catch (SQLException e) {
			System.err.println("SQLException in getLinksFor: "+ e);
		}

		return markovLinks;
	}
}
