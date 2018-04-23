package me.Usoka.markov;

import com.sun.istack.internal.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataHandler {

	/**
	 * Gets how many unique words are in the complete lexicon from all users
	 * @return number of unique words
	 */
	int getLexiconSizeAll();

	/**
	 * Gets how many unique words are in the lexicon for a specified user
	 * @param user specified user for lexicon
	 * @return number of unique words
	 */
	int getLexiconSizeFor(@NotNull User user);

	/**
	 * Gets how many times any use has said a specified word
	 * @param word specified word to find the frequency of
	 * @return total number of times any user has said the word
	 */
	int getWordFrequencyAll(@NotNull String word);

	/**
	 * Gets the frequency for a specified word, and a specified user. (how
	 * many times the user has said that specific word)
	 * @param word specified word to find frequency of
	 * @param user specified user to find the frequency for
	 * @return number of times the user has said the word
	 */
	int getWordFrequencyFor(@NotNull String word, @NotNull User user);

	/**
	 * Gets a complete set of unique words said by all users
	 * @return <code>Set</code> containing all unique words
	 */
	Set<String> getLexiconAll();

	/**
	 * Gets a complete set of unique words said for a specific user
	 * @param user specified user to get the lexicon for
	 * @return <code>Set</code> containing all unique words from that user
	 */
	Set<String> getLexiconFor(@NotNull User user);

	/**
	 * Gets a complete list of all words said by all users.
	 * Each word is repeated in the list the number of times it's been said
	 * @return <code>List</code> containing all words
	 */
	List<String> getAllWordsAll();

	/**
	 * Gets a complete list of all words said by a specified user.
	 * Each word is repeated in the list the number of times it's been said
	 * @param user specified user to get the list for
	 * @return <code>List</code> containing all words from that user
	 */
	List<String> getAllWordsFor(@NotNull User user);

	/**
	 * Gets the words that are linked to from a given one, for all
	 * users in the markov data. Words are mapped to how often they have occurred
	 * @param word specified word to get links from
	 * @return <code>Map</code> of the linked words to their frequencies
	 */
	Map <String, Integer> getLinksAll(@NotNull String word);

	/**
	 * Gets the words that are linked to from a given word, for a specified
	 * user, in the markov data. Words are mapped to how often they have occurred
	 * @param user specified user to get the markov links for
	 * @param word specified word to get links from
	 * @return <code>Map</code> of the linked words to their frequencies
	 */
	Map<String, Integer> getLinksFor(@NotNull User user, @NotNull String word);
}
