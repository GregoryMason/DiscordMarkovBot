package me.Usoka.bot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Bot {
	private static final String configFile = "src\\main\\resources\\bot.config";

	/**
	 * Get the pair of user IDs as Strings from a single string containing both
	 * @param pairedUserString String which contains the user pair
	 * @return The pair split into a 2 length array
	 */
	private static String[] getPair(String pairedUserString) {
		//Enforce that pairs must be valid format
		if (!pairedUserString.matches("^[0-9]+(\\s+)?=(\\s+)?[0-9]+$")) {
			throw new IllegalArgumentException("Invalid ignore user pair: "+ pairedUserString);
		}

		return pairedUserString.split("(\\s+)?=(\\s+)?");
	}

	/**
	 * Checks the formatting for the config file. Throws exception if anything's invalid
	 * @param configFile the <code>File</code> that is the configFile
	 */
	private static void checkConfigFormat(File configFile) {
		try (Scanner s = new Scanner(configFile)) {
			if (!s.nextLine().matches("^Token: .+")) throw new InvalidFormatException("Invalid token format");
			if (!s.nextLine().matches("^Admin: [0-9]+$")) throw new InvalidFormatException("Invalid admin id format");
			if (!s.nextLine().matches("^Home Guild: [0-9]+$")) throw new InvalidFormatException("Invalid home guild format");
			if (!s.nextLine().matches("^Home Channel: [0-9]+$")) throw new InvalidFormatException("Invalid home channel format");
			if (!s.nextLine().matches("^Ignored: ?((,\\s+)?([0-9]+)?)*")) throw new InvalidFormatException("Invalid ignored channel(s) format");
			if (!s.nextLine().matches("^Merge Users: ((,\\s+)?[0-9]+(\\s+)?=(\\s+)?[0-9]+)*")) throw new InvalidFormatException("Invalid merged users format");
		} catch (IOException e) {
			System.err.println("Error in format check despite successful initial file opening: "+ e);
		}
	}

	public static void main(String[] args) throws LoginException, RateLimitedException{
		Config botConfig;
		String homeGuildID, homeGuildTargetChannel;
		String[] homeGuildIgnoredChannels;
		Map<String, String> mergeUsers = new HashMap<>();

		//Try reading in from an existing config file
		try (Scanner config = new Scanner(new File(configFile))) {
			//Check the file formatting
			checkConfigFormat(new File(configFile));

			botConfig = new Config(config.nextLine().substring(7));						//Read in bot token
			botConfig.setAdminID(config.nextLine().substring(7));				//Read in admin user ID
			homeGuildID = config.nextLine().substring(12);				//Read in home guild
			homeGuildTargetChannel = config.nextLine().substring(14);	//Read in bot channel

			String line = config.nextLine();							//Read in ignored channels
			if (!line.matches("^Ignored: ?$")) { //Only try reading in ignore channels if any are specified
				homeGuildIgnoredChannels = line.substring(9).split(",(\\s+)?");
			}

			//Read in pairs of users to merge for markov
			for (String userPair : config.nextLine().substring(13).split(",(\\s+)?")) {
				try {
					String[] splitPair = getPair(userPair);
					mergeUsers.put(splitPair[0], splitPair[1]);
				} catch (IllegalArgumentException invalidPair) {
					System.err.println(invalidPair.getMessage());
				}
			}

		} catch (FileNotFoundException e) {
			//File doesn't exist, or is in incorrect format, so prompt user in order to create a config file
			Scanner console = new Scanner(System.in);

			//TODO input validation

			System.out.print("Bot Token: ");
			botConfig = new Config(console.nextLine());

			System.out.print("Admin User ID: ");
			botConfig.setAdminID(console.nextLine());

			System.out.print("Home Guild ID: ");
			homeGuildID = console.nextLine();

			System.out.print("Target Channel ID: ");
			homeGuildTargetChannel = console.nextLine();

			//Channels that should be ignored
			System.out.print("Ignored Channels (separated by ,): ");
			String ignoredChannels = console.nextLine();
			homeGuildIgnoredChannels = ignoredChannels.split(",(\\s+)?");


			//Get which user accounts' should have their markov data merged
			System.out.print("Users to merge (Format <ID>=<ID>[,<ID>=<ID>[...]]): ");
			String mergedUsers = console.nextLine();

			//Read in pairs of users to merge for markov
			for (String userPair : mergedUsers.split(",(\\s+)?")) {
				try {
					String[] splitPair = getPair(userPair);
					mergeUsers.put(splitPair[0], splitPair[1]);
				} catch (IllegalArgumentException invalidPair) {
					System.err.println(invalidPair.getMessage());
				}
			}

			//Save the collected input to a new config file
			try (FileWriter config = new FileWriter(new File(configFile))) {
				config.write("Token: "+ botConfig.getToken() +"\r\n");							//Bot token
				config.write("Admin: "+ botConfig.getAdminID() +"\r\n");					//Admin user ID
				config.write("Home Guild: "+ homeGuildID +"\r\n");				//Home guild
				config.write("Home Channel: "+ homeGuildTargetChannel +"\r\n");	//Bot channel
				config.write("Ignored: "+ ignoredChannels +"\r\n");				//Ignored channels
				config.write("Merge Users: "+ mergedUsers);						//Users to merge

			} catch (IOException fileSaveFail) {
				System.err.println("Could not save config to file: "+ fileSaveFail);
			}
		}

		BotListener botListener = new BotListener(homeGuildID, homeGuildTargetChannel);
		botListener.setAdmin(botConfig.getAdminID());
		//TODO add ignored channels to botListener
		//TODO add users to merge to botListener

		JDA api = new JDABuilder(AccountType.BOT).setToken(botConfig.getToken()).buildAsync();
		api.addEventListener(botListener);
	}

	public static class Config {
		private final String token;
		private String adminID;
		private String homeID;

		public Config(String token) {
			if (token == null || token.isEmpty()) throw new IllegalArgumentException("Invalid bot token (null or empty)");
			//TODO formatting checks for token
			this.token = token;
		}

		public String getToken() { return token; }

		public void setAdminID(String adminUserID) {
			if (adminUserID != null) {
				if (adminUserID.isEmpty()) throw new IllegalArgumentException("Admin user ID cannot be empty");
				if (adminUserID.matches("^\\d+$")) throw new IllegalArgumentException("Invalid admin user ID format ("+ adminUserID +")");
			}
			this.adminID = adminUserID;
		}
		public String getAdminID() { return adminID; }

		/**
		 * Checks if the given user's ID matches that of the stored Admin User ID
		 */
		public boolean isAdminUser(String userID) { return userID.equals(adminID); }

		public void setHomeGuildID(String homeGuildID) {
			if (homeGuildID != null) {
				if (homeGuildID.isEmpty()) throw new IllegalArgumentException("Guild ID cannot be empty");
				if (homeGuildID.matches("^\\d+$")) throw new IllegalArgumentException("Invalid Guild ID format ("+ homeGuildID +")");
			}
			this.homeID = homeGuildID;
		}
		public String getHomeGuildID() { return homeID; }

		/**
		 * Checks if the given Guild's ID matches that of the stored Home Guild ID
		 */
		public boolean isHomeGuid(String guildID) { return guildID.equals(homeID); }
	}
}
