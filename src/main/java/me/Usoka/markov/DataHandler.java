package me.Usoka.markov;

import java.util.List;

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
	int getLexiconSizeFor(User user);

	/**
	 * Gets how many times any use has said a specified word
	 * @param word specified word to find the frequency of
	 * @return total number of times any user has said the word
	 */
	int getWordFrequencyAll(String word);

	/**
	 * Gets the frequency for a specified word, and a specified user. (how
	 * many times the user has said that specific word)
	 * @param word specified word to find frequency of
	 * @param user specified user to find the frequency for
	 * @return number of times the user has said the word
	 */
	int getWordFrequencyFor(String word, User user);

	/**
	 * Gets the complete list of unique words said by all users
	 * @return <code>List</code> containing all words
	 */
	List<String> getLexiconAll();

	/**
	 * Gets the complete list of unique words said for a specific user
	 * @param user specified user to get the lexicon for
	 * @return <code>List</code> containing all words from that user
	 */
	List<String> getLexiconFor(User user);

	/**
	 * Gets the words that are linked to from a given one, for all
	 * users in the markov data
	 * @param word specified word to get links from
	 * @return <code>List</code> containing all linked words
	 * TODO update this to a HashMap of linkedWord->frequency
	 */
	List <String> getLinksAll(String word);

	/**
	 * Gets the words that are linked to from a given word, for a specified
	 * user, in the markov data
	 * @param user specified user to get the markov links for
	 * @param word specified word to get links from
	 * @return <code>List</code> containing all linked words
	 * TODO update this to a HashMap of linkedWord->frequency
	 */
	List<String> getLinksFor(User user, String word);
}
