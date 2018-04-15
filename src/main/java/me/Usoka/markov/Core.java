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
	 * @param selectedUser specified user for the markov data. <code>null</code> can
	 *                     be used to specify targeting all users
	 */
	public Core(String sqlSourceDatabaseDir, String sqlMarkovDatabaseDir, User selectedUser) {
		this.markovSource = new SQLiteSourceHandler(sqlSourceDatabaseDir);
		this.markovData = new SQLiteDataHandler(sqlMarkovDatabaseDir);
		this.currentUser = selectedUser;
		getLexiconFromSource();
	}

	/**
	 * Sets the target user for the Markov chain to build sentences and get data for
	 * @param targetUser specified target user. <code>null</code> to set to all users
	 */
	public void setTargetUser(User targetUser) {
		this.currentUser = targetUser;
		getLexiconFromSource();
	}

	private void getLexiconFromSource() {
		if (currentUser == null) lexicon = markovData.getLexiconAll();
		else lexicon = markovData.getLexiconFor(currentUser);
	}

	/**
	 * @return how many words are in the lexicon for everyone
	 */
	public int getAllLexiconSize() { return markovData.getLexiconSize(); }

	/**
	 * @return get the size of the lexicon for the current user
	 */
	public int getLexiconSize() {
		if (currentUser == null) return getAllLexiconSize();
		return markovData.getLexiconSizeFor(currentUser);
	}

	/**
	 * Gets the size of the lexicon for a specified user
	 * @param user specified user to get the lexicon size for
	 * @return how many words are in the lexicon associated with that user
	 */
	public int getLexiconSizeFor(User user) { return markovData.getLexiconSizeFor(user); }

	/**
	 * Gets the frequency of a specified word for all users
	 * @param word word to check frequency of
	 * @return sum frequency of specified word in source
	 */
	public int getAllFrequencyOf(String word) { return markovData.getFrequencyAllOf(word); }

	/**
	 * Finds the frequency of a specified word for the current user
	 * @param word word to check frequency of
	 * @return frequency of specified word in source
	 */
	public int getFrequencyOf(String word) {
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
	 */
	public List<String> getMarkovList(String word) {
		if (currentUser == null) return markovData.getLinksAll(word);
		return markovData.getLinksFor(currentUser, word);
	}

	/**
	 * Returns the markov data (potential next words) for a specified word as a String
	 * @param word word to find markov data for
	 * @return what could follow that word
	 */
	public String getMarkovString(String word) {
		List<String> linkEnds = getMarkovList(word);

		//Build a hashMap for displaying the information
		Map<String, Integer> wordFreq = new HashMap<>();
		for (String endWord : linkEnds) if (wordFreq.containsKey(endWord)) wordFreq.put(endWord, wordFreq.get(endWord) +1); else wordFreq.put(endWord, 1);

		StringBuilder collatedString = new StringBuilder();
		for (String endWord : wordFreq.keySet()) {
			int freq = wordFreq.get(endWord);
			collatedString.append(freq).append((freq > 10)? ((freq > 100)? "": " "):"  ")	//Add the frequency
					.append(endWord).append("\r\n");										//And the word itself
		}

		return collatedString.toString();
	}

	/**
	 * Picks a random word from the lexicon and returns it
	 * @return the chosen word
	 */
	public String getRandomWord() {
		if (lexicon == null || lexicon.size() == 0) return "";
		//TODO adjust for different frequencies of words
		return lexicon.get((int)(Math.random() * lexicon.size()));
	}

	private String getNextWord(String precedingWord) {
		String returnWord = getRandomWord();

		List<String> linkedWords = getMarkovList(precedingWord);
		if (linkedWords.size() == 0) return "";

		//If it's not going to remain a random word, return a random one from the links
		if ((int)(Math.random() * 50) != 1) return linkedWords.get((int)(Math.random() * linkedWords.size()));

		return returnWord;
	}

	/**
	 * Build a sentence from a provided word using markov chains
	 * @param startWord word to start the sentence with
	 * @return the completed sentence
	 */
	private String buildSentence(String startWord) {
		StringBuilder sentence = new StringBuilder();
		String precedingWord = startWord; String nextWord;

		sentence.append(precedingWord).append(" ");

		int repeatCount = 0;

		while (sentence.length() < 500) { //Make sure sentences can't become too long
			nextWord = getNextWord(precedingWord);
			if (nextWord.equals("")) break; //try/catch block

			sentence.append(nextWord).append(" ");

			//Check if it's repeated. If so, increase repeat count, else reset it
			if (precedingWord.equals(nextWord)) repeatCount++; else repeatCount = 0;
			precedingWord = nextWord;

			//Random chance of stopping the sentence, influenced by how many words follow the new word, how common it is, and how many times it's repeated
			if ((int) (Math.random() * (10 + getMarkovList(nextWord).size() + getFrequencyOf(nextWord) - repeatCount + 2)) <= 2) break;
		}
		return sentence.toString();
	}

	/**
	 * Get a sentence starting with a random word
	 * @return the sentence generated
	 */
	public String getSentence() {
		return buildSentence(getRandomWord());
	}

	/**
	 * Get a sentence starting with a specified word
	 * @param startWord word to start sentence with
	 * @return completed sentence
	 */
	public String getSentence(String startWord) {
		if (!lexicon.contains(startWord)) return "\""+ startWord +"\" not found in lexicon";
		return buildSentence(startWord);
	}

	/**
	 * Get the ID of the most recent message
	 * @return ID
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
		//TODO
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
	 * @param message message to update
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
	public boolean deleteMessage(String messageID) { return markovSource.deleteMessage(messageID); }

	/**
	 * Counts the number of messages in the file used for Markov source
	 * @return number of lines/messages
	 */
	public int getSourceCountOf(User targetUser) throws Exception{
		if (markovSource == null) throw new Exception("Markov Source not configured");

		return markovSource.countMessagesFromUser(targetUser);
	}
}
