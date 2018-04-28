PRAGMA foreign_keys=OFF;

BEGIN TRANSACTION;

CREATE TABLE users (userID INTEGER PRIMARY KEY, username text NOT NULL);
CREATE TABLE messages (messageID INTEGER PRIMARY KEY, content text NOT NULL);
CREATE TABLE user_messages (userID INTEGER, messageID INTEGER, PRIMARY KEY (userID, messageID), FOREIGN KEY (userID) REFERENCES users (userID), FOREIGN KEY (messageID) REFERENCES messages (messageID));
COMMIT;
