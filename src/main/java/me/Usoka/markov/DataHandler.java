package me.Usoka.markov;

import java.util.List;

public interface DataHandler {

	/**
	 * @return how many unique words are in the lexicon built from all users
	 */
	int getLexiconSize();

	/**
	 * @param user specified user
	 * @return how many unique words are in the lexicon for the specified user
	 */
	int getLexiconSizeFor(User user);

	int getFrequencyOf(String word, User user);

	int getFrequencyAllOf(String word);

	List<String> getLexiconAll();

	List<String> getLexiconFor(User user);

	List<String> getLinksFor(User user, String word);

	List <String> getLinksAll(String word);
}
