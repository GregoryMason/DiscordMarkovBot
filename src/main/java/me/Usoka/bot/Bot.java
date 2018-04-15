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
		if (!pairedUserString.matches("^[0-9]*(\\s+)?=(\\s+)?[0-9]*$")) {
			throw new IllegalArgumentException("Invalid ignore user pair: "+ pairedUserString);
		}

		return pairedUserString.split("(\\s+)?=(\\s+)?");
	}

	public static void main(String[] args) throws LoginException, RateLimitedException{
		String token, adminUserID, homeGuildID, homeGuildTargetChannel;
		String[] homeGuildIgnoredChannels;
		Map<String, String> mergeUsers = new HashMap<>();

		//Try reading in from an existing config file
		try (Scanner config = new Scanner(new File(configFile))) {
			//TODO ensure file is in correct formatting FIXME potential errors
			token = config.nextLine().substring(7);												//Read in bot token
			adminUserID = config.nextLine().substring(7);										//Read in admin user ID
			homeGuildID = config.nextLine().substring(12);										//Read in home guild
			homeGuildTargetChannel = config.nextLine().substring(14);							//Read in bot channel
			homeGuildIgnoredChannels = config.nextLine().substring(9).split(",(\\s+)?");	//Read in ignored channels

			//Read in pairs of users to merge for markov
			for (String userPair : config.nextLine().substring(13).split(",(\\s+)?")) {
				try {
					String[] splitPair = getPair(userPair);
					mergeUsers.put(splitPair[0], splitPair[1]);
				} catch (IllegalArgumentException invalidPair) {
					System.err.println(invalidPair.getMessage());
				}
			}

		} catch (FileNotFoundException fileNotFound) { //File doesn't exist, so prompt user in order to create a config file
			Scanner console = new Scanner(System.in);

			//TODO input validation

			System.out.print("Bot Token: ");
			token = console.nextLine();

			System.out.print("Admin User ID: ");
			adminUserID = console.nextLine();

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
				config.write("Token: "+ token +"\r\n");							//Bot token
				config.write("Admin: "+"\r\n");									//Admin user ID
				config.write("Home Guild: "+ homeGuildID +"\r\n");				//Home guild
				config.write("Home Channel: "+ homeGuildTargetChannel +"\r\n");	//Bot channel
				config.write("Ignored: "+ ignoredChannels +"\r\n");				//Ignored channels
				config.write("Merge Users: "+ mergedUsers);						//Users to merge

			} catch (IOException fileSaveFail) {
				System.err.println("Could not save config to file: "+ fileSaveFail);
			}
		}

		BotListener botListener = new BotListener(homeGuildID, homeGuildTargetChannel);
		botListener.setAdmin(adminUserID);
		//TODO add ignored channels to botListener
		//TODO add users to merge to botListener

		JDA api = new JDABuilder(AccountType.BOT).setToken(token).buildAsync();
		api.addEventListener(botListener);
	}
}
