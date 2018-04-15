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
import java.util.Scanner;

public class Bot {
	private static final String configFile = "src\\main\\resources\\bot.config";

	public static void main(String[] args) throws LoginException, RateLimitedException{
		String token;
		String adminUserID;
		String homeGuildID;
		String homeGuildTargetChannel;
		String[] homeGuildIgnoredChannels;

		try (Scanner config = new Scanner(new File(configFile))) {
			//TODO ensure file is in correct formatting
			token = config.nextLine().substring(7);									//Read in bot token
			adminUserID = config.nextLine().substring(7);							//Read in admin user ID
			homeGuildID = config.nextLine().substring(12);							//Read in home guild
			homeGuildTargetChannel = config.nextLine().substring(14);				//Read in bot channel
			homeGuildIgnoredChannels = config.nextLine().split(",(\\s+)?");	//Read in ignored channels
		} catch (FileNotFoundException e) {
			Scanner console = new Scanner(System.in);

			//TODO input validation for IDs

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

			//TODO Handle user which should have accounts' markov data merged


			try (FileWriter config = new FileWriter(new File(configFile))) {
				config.write("Token: "+ token +"\r\n");
				config.write("Admin: "+"\r\n");
				config.write("Home Guild: "+ homeGuildID +"\r\n");
				config.write("Home Channel: "+ homeGuildTargetChannel +"\r\n");
				config.write("Ignored: "+ ignoredChannels +"\r\n");
				config.write("Merge Users: "); //TODO

			} catch (IOException e2) {
				System.err.println("Could not save config to file: "+ e2);
			}
		}

		BotListener botListener = new BotListener(homeGuildID, homeGuildTargetChannel);
		botListener.setAdmin(adminUserID);

		JDA api = new JDABuilder(AccountType.BOT).setToken(token).buildAsync();
		api.addEventListener(botListener);
	}
}
