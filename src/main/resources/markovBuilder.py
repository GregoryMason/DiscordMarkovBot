import sqlite3

COMMAND_CHARS = "!"

#Make sure everything is stripped down in the same way
def stripWord(word):
	#TODO add a way to strip <>[](){}""'': if only on one side of word
	word = word#.rstrip("!")
	return (word)#.strip("\n\"\'?.;, ()[]{\}:!&+=﻿"))


def getAllWords(messages, allWords = []):
	allWords.clear()

	for msg in messages:
		words = msg.split(" ")
		for word in words:
			word = stripWord(word = word)
			if word == "":
				continue
			allWords.append(word)

	return allWords

def buildChain(allLinks, messages, chain = {}):
	chain.clear()

	for message in messages:
		words = message.split(" ")

		#Nothing to be added to the chain
		if len(words) == 1:
			continue

		index = 1
		for word in words[index:]:
			key = (stripWord(word = words[index -1].lower()), stripWord(word = word))
			if key in allLinks:
				linkID = allLinks[key]
				if linkID in chain:
					chain[linkID] = chain[allLinks[key]] + 1
				else:
					chain[linkID] = 1
			else:
				linkID = len(allLinks)
				allLinks[key] = linkID
				chain[linkID] = 1
			index += 1

	return chain


#Read in all the source messages for a given user
def readSource(sourceDatabase, userID):
	sourceMessages = []

	c = sourceDatabase.cursor()

	userMergePairs = open("bot.config", "r").readlines()[5][13:].split("=")
	if (userID == int(userMergePairs[0])): #Merge messages for specified user accounts
		c.execute("SELECT messages.content FROM users "+
			"LEFT JOIN user_messages ON users.userID = user_messages.userID "+
			"LEFT JOIN messages ON user_messages.messageID = messages.messageID "+
			"WHERE users.userID = ?", (int(userMergePairs[1]),))
		for row in c.fetchall():
			sourceMessages.append(row[0])
		
	c.execute("SELECT messages.content FROM users "+
		"LEFT JOIN user_messages ON users.userID = user_messages.userID "+
		"LEFT JOIN messages ON user_messages.messageID = messages.messageID "+
		"WHERE users.userID = ?", (userID,))

	for row in c.fetchall():
		sourceMessages.append(row[0])

	return sourceMessages


def addUser(markovDatabase, allLinks, user, userMessages, userLexicon = {}, markovChain = {}):
	userLexicon.clear()
	markovChain.clear()

	#build the data for the markov chain
	markovChain = buildChain(allLinks = allLinks, messages = userMessages)

	#Get all words and add them to the lexicon; unique entries with frequency count
	allWords = getAllWords(messages = userMessages)

	for word in allWords:
		if word in userLexicon:
			userLexicon[word] += 1
		else:
			userLexicon[word] = 1

	#Save the lexicon to the markov database
	for word in userLexicon:
		if word == "":
			continue
		markovDatabase.cursor().execute("INSERT INTO user_lexicons (userID, word, frequency) VALUES (?,?,?)", (user, word, userLexicon[word]))
	markovDatabase.commit()

	#Save the markov data to the markov database
	for linkID in markovChain:
		markovDatabase.cursor().execute("INSERT INTO user_links (userID, linkID, frequency) VALUES (?,?,?)", (user, linkID, markovChain[linkID]))
	markovDatabase.commit()





def main():
	targetUsers = []
	allLinks = {}

	#Open the 2 databases
	sourceDatabase = sqlite3.connect("sourceData.db")
	markovDatabase = sqlite3.connect("markovData.db")

	for user in sourceDatabase.cursor().execute("SELECT * FROM users"):
		targetUsers.append(user[0])
		#Ensure all users are in the database, and it's all up to date
		markovDatabase.cursor().execute("REPLACE INTO users (userID, username, discriminator) VALUES (?,?,?)", user)

	#Commit all the queries updating the database
	markovDatabase.commit()

	#Clear previous data to ensure it's accurate to the source
	markovDatabase.cursor().execute("DELETE FROM user_links").execute("DELETE FROM links").execute("DELETE FROM user_lexicons")
	markovDatabase.commit()


	#For each of the users which there is data for, add them to the database
	for user in targetUsers:
		print("Adding user: "+ str(user))
		addUser(markovDatabase = markovDatabase, allLinks = allLinks, user = user, userMessages = readSource(sourceDatabase = sourceDatabase, userID = user))

	for linkTuple in allLinks:
		markovDatabase.cursor().execute("INSERT INTO links (linkID, startWord, endWord) VALUES (?,?,?)", (allLinks[linkTuple], linkTuple[0], linkTuple[1]))
	markovDatabase.commit()

	#Finally, close the databases
	sourceDatabase.close()
	markovDatabase.close()

if __name__ == "__main__":
	main()