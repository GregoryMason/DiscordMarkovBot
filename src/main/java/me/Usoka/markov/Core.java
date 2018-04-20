package me.Usoka.markov;

import java.util.*;

public class Core {
	private SourceHandler markovSource;
	private DataHandler markovData;

	private User currentUser;
	private List<String> lexicon;

	/**
	 * Create an instance of <code>Core</code> without a specific user to target from initialisation
	 * @param sqlSourceDatabaseDir directory for the SQLite Source database
	 * @param sqlMarkovDatabaseDir directory for the SQLire Markov database
	 */
	public Core(String sqlSourceDatabaseDir, String sqlMarkovDatabaseDir) {
		this(sqlSourceDatabaseDir,sqlMarkovDatabaseDir,null);
	}

	/**
	 * Create an instance of <code>Core</code> with a specified user to targer from initialisation
	 * @param sqlSourceDatabaseDir directory for the SQLite Source database
	 * @param sqlMarkovDatabaseDir directory for the SQLire Markov database
	 * @param selectedUser specified user for the markov data. <code>null</code>
	 *                     to target all users
	 */
	public Core(String sqlSourceDatabaseDir, String sqlMarkovDatabaseDir, User selectedUser) {
		//TODO Ensure parameters are valid arguments (ie. check sqlSourceDatabaseDir and sqlMarkovDatabaseDir not null)
		this.markovSource = new SQLiteSourceHandler(sqlSourceDatabaseDir);
		this.markovData = new SQLiteDataHandler(sqlMarkovDatabaseDir);
		this.currentUser = selectedUser;
		getLexiconFromData();
	}

	/**
	 * Sets the target user for the Markov chain. Sentences and data is based on <code>targetUser</code>
	 * @param targetUser specified target user. <code>null</code> to set to all users
	 */
	public void setTargetUser(User targetUser) {
		this.currentUser = targetUser;
		getLexiconFromData();
	}

	/**
	 * Builds the lexicon (collection of all unique words) for the current target user.
	 */
	private void getLexiconFromData() {
		if (currentUser == null) lexicon = markovData.getLexiconAll();
		else lexicon = markovData.getLexiconFor(currentUser);
	}

	/**
	 * @return how many unique words are in the lexicon for everyone
	 */
	public int getAllLexiconSize() { return markovData.getLexiconSize(); }

	/**
	 * Get the size of the lexicon for the current target user
	 * @return size of lexicon for the current target user
	 */
	public int getLexiconSize() {
		//TODO confirm, is this just the same as lexicon.size()?
		if (currentUser == null) return getAllLexiconSize();
		return markovData.getLexiconSizeFor(currentUser);
	}

	/**
	 * Gets the size of the lexicon for a specified user
	 * @param user specified user to get the lexicon size for
	 * @return how many words are in the lexicon associated with that user
	 */
	public int getLexiconSizeFor(User user) {
		//TODO reject null
		return markovData.getLexiconSizeFor(user);
	}

	/**
	 * Gets the frequency of a specified word for all users
	 * @param word word to check frequency of
	 * @return frequency of the specified word in the markov data
	 */
	public int getAllFrequencyOf(String word) {
		//TODO reject null
		return markovData.getFrequencyAllOf(word);
	}

	/**
	 * Finds the frequency of a specified word for the current target user
	 * @param word word to check frequency of
	 * @return frequency of specified word in the markov data, for current user
	 */
	public int getFrequencyOf(String word) {
		//TODO reject null
		if (currentUser == null) return getAllFrequencyOf(word);
		return markovData.getFrequencyOf(word, currentUser);
	}

	/**
	 * @return how many words (counting multiple appearances) are in the source material
	 */
	public int getSumAllWords() {
		if (currentUser == null) return 0;
		return markovData.getFrequencyAllWordsFor(currentUser);
	}

	/**
	 * Returns the markov links (potential next words) for a specified word
	 * @param word word to find markov links for
	 * @return <code>List</code> of words that could follow that word
	 * TODO change this to return a Map of word->frequency (this list can easily be made from one
	 */
	public List<String> getMarkovList(String word) {
		if (currentUser == null) return markovData.getLinksAll(word);
		return markovData.getLinksFor(currentUser, word);
	}

	/**
	 * Builds a String of the markov data for a specified word, including frequencies of each.
	 * Uses the markov data of the current target user
	 * @param word word to find the markov links for
	 * @return all words that can follow that word, and their frequencies
	 */
	public String getMarkovString(String word) {
		List<String> linkEnds = getMarkovList(word);

		//Build a hashMap for displaying the information
		Map<String, Integer> wordFreq = new HashMap<>();
		for (String endWord : linkEnds) if (wordFreq.containsKey(endWord)) wordFreq.put(endWord, wordFreq.get(endWord) +1); else wordFreq.put(endWord, 1);

		//Build the string, each word a new line formatted [freq] [word]
		StringBuilder collatedString = new StringBuilder();
		for (String endWord : wordFreq.keySet()) {
			int freq = wordFreq.get(endWord);
			collatedString.append(freq).append((freq > 10)? ((freq > 100)? "": " "):"  ")	//Add the frequency
					.append(endWord).append("\r\n");										//And the word itself
		}

		return collatedString.toString();
	}

	/**
	 * Gets a random word from the current target user's lexicon
	 * @return the chosen word
	 */
	public String getRandomWord() {
		if (lexicon == null || lexicon.size() == 0) return "";
		//TODO adjust for different frequencies of words
		return lexicon.get((int)(Math.random() * lexicon.size()));
	}

	/**
	 * Picks a next word to use in a markov chain, based on the links from the current word
	 * <br/> Note: Has a 1 in 50 chance of picking a completely random word instead
	 * @param precedingWord the word which this one will follow
	 * @return the chosen next word
	 */
	private String getNextWord(String precedingWord) {
		String returnWord = getRandomWord();

		//Get all the words which are linked in the markov data from precedingWord
		List<String> linkedWords = getMarkovList(precedingWord);
		if (linkedWords.size() == 0) return "";

		//If it's not going to remain a random word, return a random one from the links
		if ((int)(Math.random() * 50) != 1) return linkedWords.get((int)(Math.random() * linkedWords.size()));

		return returnWord;
	}

	/**
	 * Build a sentence from a provided word using the markov data of the current target user
	 * @param startWord specified word to start building the sentence from
	 * @return the completed sentence
	 */
	private String buildSentence(String startWord) {
		StringBuilder sentence = new StringBuilder();
		String precedingWord = startWord, nextWord;

		//Begin the sentence with the provided word
		sentence.append(precedingWord).append(" ");

		int repeatCount = 0;

		while (sentence.length() < 500) { //Make sure sentences can't become too long
			nextWord = getNextWord(precedingWord);
			if (nextWord.equals("")) break;

			sentence.append(nextWord).append(" ");

			//Check if it's repeated. If so, increase repeat count, else reset it
			if (precedingWord.equals(nextWord)) repeatCount++; else repeatCount = 0;
			precedingWord = nextWord;

			//Random chance of stopping the sentence
			//Less likely to stop if the last word has more words that follow it
				//or if the word occurs more frequently
			//More likely to stop the sentence if the word has repeated multiple times
			if ((int) (Math.random() * (10 + getMarkovList(nextWord).size() + getFrequencyOf(nextWord) - repeatCount + 2)) <= 2) break;
		}
		return sentence.toString();
	}

	/**
	 * Get a markov chain sentence starting with a random word
	 * @return the sentence generated
	 */
	public String getSentence() { return buildSentence(getRandomWord()); }

	/**
	 * Get a markov chain sentence starting with a specified word
	 * @param startWord word to start sentence with
	 * @return the sentence generated from the word
	 */
	public String getSentence(String startWord) {
		//Ensure the user has said that word before
		if (!lexicon.contains(startWord)) return "\""+ startWord +"\" not found in lexicon";
		return buildSentence(startWord);
	}

	/**
	 * Get the ID of the most recent message
	 * @return message ID as String
	 */
	public String getMostRecentID() {
		try {
			return markovSource.getMostRecentMessageID();
		} catch (Exception e) { System.out.println("Source read error: "+ e); }
		return "";
	}

	/**
	 * Ensures there is a matching user in the source. Otherwise updates source to match provided user
	 * @param user user that should be matched in the source
	 */
	public void ensureUser(User user) {
		//TODO Call relevant method in SourceHandler when created
	}

	/**
	 * Saves a specified message to the source list
	 * @param message message to save
	 */
	public void saveMaterial(Message message) {
		if (markovSource == null) return;
		String content = message.getContentCleaned();

		//Don't save empty messages
		if (content.equals("")) return;

		try {
			//If it exists (for some reason), update it instead of saving as new message
			if (markovSource.containsMessageID(message.getId())) {
				updateMaterial(message);
				return;
			}

			//Ensure the author of the message exists in the source
			if (!markovSource.containsUserID(message.getAuthor().getId())) {
				//Add the user to the source
				if (!markovSource.saveUser(message.getAuthor())) {
					throw new Exception("Adding user ("+ message.getAuthor().getId() +"|"+ message.getAuthor().getName() +") failed");
				}
			}
		} catch (Exception e) {
			System.out.println("Failed to save message "+ message.getId() +": "+ e);
			return;
		}

		markovSource.saveMessage(message);
	}

	/**
	 * Updates an existing message in the source material
	 * @param message updated version of the message
	 */
	public void updateMaterial(Message message) {
		if (markovSource == null) return;
		//Check if it's already in source data, otherwise add it as a new message
		try {
			if (!markovSource.containsMessageID(message.getId())) {
				saveMaterial(message);
				return;
			}
		} catch (Exception e) {
			System.out.println("Update Message Error: "+ e);
			return;
		}

		String cleanedContent = message.getContentCleaned();
		if (cleanedContent.equals("")) {
			//Since the message now has no relevant content, it should just be deleted from the source
			deleteMessage(message.getId());
			return;
		}

		markovSource.updateMessage(message);
	}

	/**
	 * Updates a List of messages in bulk rather than one at a time
	 * @param messages <code>List</code> of messages
	 * @return how many messages were actually updated/saved
	 */
	public int updateMaterial(List<Message> messages) {
		if (markovSource == null) return -1;
		int numSaved = 0;

		//Track which users are in this bulk update (using userID)
		HashSet<String> users = new HashSet<>();

		for (Message message : messages) {
			//Take out messages which shouldn't be saved
			if (message.getContentCleaned().equals("")) { continue; }

			try { //Ensure all users are in the source
				if (!users.contains(message.getAuthor().getId())) {
					//If they aren't, add them
					if (!markovSource.containsUserID(message.getAuthor().getId())) {
						if (!markovSource.saveUser(message.getAuthor())) {
							throw new Exception("Adding user ("+ message.getAuthor().getId() +
									"|"+ message.getAuthor().getName() +") failed");
						}
					}

					//Add user to the Set tracking users in this bulk update
					users.add(message.getAuthor().getId());
				}
			} catch (Exception e) { System.out.println("Failed to save message "+ message.getId() +": "+ e); }
			numSaved++;
		}

		//Save the complete list of messages now
		markovSource.updateMessages(messages);

		return numSaved;
	}

	/**
	 * Deletes a specified message from the source by given messageID
	 * @param messageID ID of message to remove
	 */
	public void deleteMessage(String messageID) { markovSource.deleteMessage(messageID); }

	/**
	 * Counts the number of messages in the file used for Markov source
	 * @return number of lines/messages
	 */
	public int getSourceCountOf(User targetUser) throws Exception{
		if (markovSource == null) throw new Exception("Markov Source not configured");

		return markovSource.countMessagesFromUser(targetUser);
	}
}
