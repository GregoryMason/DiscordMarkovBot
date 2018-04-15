PRAGMA foreign_keys=OFF;

BEGIN TRANSACTION;

CREATE TABLE users (userID INTEGER PRIMARY KEY, username text NOT NULL, discriminator INTEGER NOT NULL);

CREATE TABLE links (linkID INTEGER PRIMARY KEY, startWord text NOT NULL, endWord text);

CREATE TABLE user_links (userID INTEGER, linkID INTEGER, frequency INTEGER, PRIMARY KEY (userID, linkID), FOREIGN KEY (userID) REFERENCES users (userID), FOREIGN KEY (linkID) REFERENCES links (linkID));

CREATE TABLE user_lexicons (userID INTEGER, word NOT NULL, frequency INTEGER, PRIMARY KEY (userID, word), FOREIGN KEY (userID) REFERENCES users (userID));

COMMIT;
