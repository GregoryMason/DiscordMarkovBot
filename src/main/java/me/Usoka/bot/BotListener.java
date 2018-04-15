package me.Usoka.bot;

import me.Usoka.markov.Core;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.collections4.ListUtils;

import java.util.*;

public class BotListener extends ListenerAdapter {

	/**
	 * Character that is used for initialising commands for this bot ({@value})
	 */
	private static final String COMMAND_INITIALIZER = "!";

	/**
	 * Path to resource folder from where the program runs
	 */
	private static final String RESOURCES_PATH = "src\\main\\resources\\";

	/**
	 * The default nickname which the bot uses for when it's running with data from all users
	 */
	private static final String DEFAULT_NICKNAME = "Markov Bot";

	private String adminUserID;

	//Guild/channel ids which the bot works in
	private final String homeGuildID;
	private final String botChannelID;

	/**
	 * Used to identify if the bot is fully running.
	 * <code>false</code> when bot is starting up
	 */
	private boolean botRunning = false;

	/**
	 * Queue of all the events which get captured during the bot starting up
	 */
	private Queue<GenericGuildMessageEvent> queuedEvents = new LinkedList<>();

	/**
	 * How many messages should be in each batch for bulk-messages ({@value})
	 */
	private static final int BATCH_SIZE = 200;

	private Core markovCore;

	/**
	 * Create an instance of <code>BotListener</code>
	 * @param homeGuildID The ID of the guild which the bot is based in
	 * @param targetChannelID Channel which the bot uses for sending messages
	 */
	public BotListener(String homeGuildID, String targetChannelID) {
		if (homeGuildID == null || targetChannelID == null) throw new IllegalArgumentException();

		this.homeGuildID = homeGuildID;
		this.botChannelID = targetChannelID;
		markovCore = new Core(
				RESOURCES_PATH +"sourceData.db",
				RESOURCES_PATH +"markovData.db");
	}

	/**
	 * Set the userID related to the administrative account for this bot
	 * @param userID
	 */
	public void setAdmin(String userID) {
		this.adminUserID = userID;
	}

	/**
	 * Event listener for when the bot has successfully connect to the API and is ready to run
	 * @param event The event which queues this
	 */
	@Override
	public void onReady(ReadyEvent event) { startBot(event.getJDA()); }

	/**
	 * Start processes for the bot to complete before processing any messages
	 * @param api api used to get the Guilds and Channels
	 */
	private void startBot(JDA api) {
		Guild homeGuild = api.getGuildById(homeGuildID);
		homeGuild.getController().setNickname(homeGuild.getSelfMember(), DEFAULT_NICKNAME).queue();

		//Ensure the username and discriminator for all users are up to date
		for (Member member : api.getGuildById(homeGuildID).getMembers()) {
			markovCore.ensureUser(convertUserClass(member.getUser()));
		}

		//Fetch all message history from between the last time bot was started and now
		getAllChannelHistory(homeGuild, api.getTextChannelById(botChannelID),
				"Starting bot", Long.parseUnsignedLong(markovCore.getMostRecentID()));

		System.out.println("Resolving "+ queuedEvents.size() +" queued events");
		//Resolve all queued events from when bot was starting up
		for (GenericGuildMessageEvent queuedEvent : queuedEvents) {
			if (queuedEvent instanceof GuildMessageReceivedEvent) messageReceived((GuildMessageReceivedEvent) queuedEvent);
			if (queuedEvent instanceof GuildMessageUpdateEvent) messageUpdated((GuildMessageUpdateEvent) queuedEvent);
			if (queuedEvent instanceof GuildMessageDeleteEvent) messageDelete((GuildMessageDeleteEvent) queuedEvent);
		}

		System.out.println("Startup complete");
		botRunning = true;
	}

	/**
	 * Takes passed arguments and interprets what commands they call
	 * @param event MessageReceivedEvent for the message with give command
	 * @param command command called in that message
	 * @param content everything else in that message
	 */
	private void interpretCommand(GuildMessageReceivedEvent event, String command, String content) {
		MessageChannel channel = event.getChannel();

		//TODO Set up something better for the commands

		//There is nothing current set up for the markov
		if (markovCore == null) return;

		if (command.equals("gethistory") && event.getAuthor().getId().equals(adminUserID)) {
			getAllChannelHistory(event.getGuild(), event.getChannel(), "Rediscovering the past...", 0);
		}

		if (command.equals("context")) channel.sendMessage("Feature missing \uD83D\uDE22").queue();

		if (command.equals("count")) {
			if (content.equals("")) {
				channel.sendMessage("Lexicon contains "+ markovCore.getLexiconSize() +" unique words").queue();
				return;
			}
			int count = markovCore.getFrequencyOf(content);
			channel.sendMessage("\""+ content +"\" said "+ count +" time"+ ((count == 1)? "":"s")).queue();
		}

		if (command.equals("markov")) {
			String markovData = markovCore.getMarkovString(content);
			//TODO ensure that it will not print if there's too many following words that it exceeds the char limit
			channel.sendMessage((markovData.equals(""))? "No data found" : "```"+ markovData +"```").queue();
		}


		if (command.equals("source")) {
			if (content.matches("^<@!?[0-9]*>$")) {

				User targetUser = event.getJDA().getUserById(content.replaceFirst("^<@!?", "").replaceFirst(">$", ""));
				int messageCount = 0;

				if (targetUser != null) try { messageCount = markovCore.getSourceCountOf(convertUserClass(targetUser)); } catch (Exception ignored) {}
				if (messageCount <= 0) event.getChannel().sendMessage("No data found for "+ content).queue();
				else event.getChannel().sendMessage(messageCount +" messages in source from "+ content).queue();
			}
		}

		if (command.equals("setuser")) {
			if (content.matches("^<@!?[0-9]*>$")) {
				User targetUser = event.getJDA().getUserById(content.replaceFirst("^<@!?", "").replaceFirst(">$", ""));
				if (targetUser != null) try {
					//Set up the nickname for the bot, ensuring it's not over the character limit
					String botNickname = targetUser.getName();
					if (botNickname.length() > 28) botNickname = botNickname.substring(0,28);
					botNickname += " Bot";

					event.getGuild().getController().setNickname(event.getGuild().getSelfMember(), botNickname).reason("Auto set nickname for changing source").queue();
					markovCore.setTargetUser(convertUserClass(targetUser));
				} catch (InsufficientPermissionException ignore) {} finally {
					event.getMessage().addReaction("\uD83C\uDD97").queue();
				}
			}
			if (content.toLowerCase().equals("all")) {
				markovCore.setTargetUser(null);
				event.getGuild().getController().setNickname(event.getGuild().getSelfMember(), DEFAULT_NICKNAME).reason("Auto reset nickname for all users").queue();
				event.getMessage().addReaction("\uD83C\uDD97").queue();
			}
		}

		if (!(event.getChannel().getId().equals(botChannelID)
				|| event.getChannel().getId().equals(botChannelID))) return;

		//For building Markov sentences
		if (command.equals("quote")) sendMarkovSentence(channel, content, false);
		if (command.equals("speak")) sendMarkovSentence(channel, content, true);

		if (command.equals("word") && content.equals("")) channel.sendMessage(markovCore.getRandomWord()).queue();
	}

	private void sendMarkovSentence(MessageChannel channel, String startingWord, boolean isTTS) {
		channel.sendTyping().queue();

		//Get the constructed sentence
		String sentence = (startingWord.equals(""))? markovCore.getSentence() : markovCore.getSentence(startingWord);
		//Send the message to the channel (or specify that it couldn't build the sentence for current user)
		channel.sendMessage((sentence.matches("^(\\s+)?$"))? "No source data for current user" : sentence).tts(isTTS).queue();
	}


	/**
	 * Converts a User from an instance of {@link net.dv8tion.jda.core.entities.User} (for the API) to
	 * {@link me.Usoka.markov.User} for the libraries used for the Markov
	 */
	private me.Usoka.markov.User convertUserClass(User u) {
		return new me.Usoka.markov.User(u.getIdLong(), u.getName(), u.getDiscriminator());
	}

	/**
	 * Converts a Message from an instance of {@link net.dv8tion.jda.core.entities.Message} (for the API) to
	 * {@link me.Usoka.markov.Message} for the libraries used for the Markov
	 */
	private me.Usoka.markov.Message convertMessageClass(Message m) {
		return new me.Usoka.markov.Message(m.getIdLong(), m.getContentRaw(), convertUserClass(m.getAuthor()));
	}




	/**
	 * Gets all the messages in a channel, to a maximum of 1000 messages
	 * @param channel channel to get messages from
	 * @return List of the messages
	 */
	private List<me.Usoka.markov.Message> getChannelMessages(TextChannel channel, Message tracker,
															 EmbedBuilder baseEmbed, List<MessageEmbed.Field> postFields,
															 long earliestMessageID) {
		int count = 0;
		List<me.Usoka.markov.Message> collectedMessages = new ArrayList<>();

		for (Message message : channel.getIterableHistory()) {
			if (message.getAuthor().isBot() || message.getContentRaw().equals("")) continue;
			if (message.getIdLong() < earliestMessageID) break; //Stop collecting messages
			collectedMessages.add(convertMessageClass(message));

			if ((++count) % BATCH_SIZE == 0) {
				//Create an updated version of the embed
				EmbedBuilder updatedEmbed = new EmbedBuilder(baseEmbed);
				updatedEmbed.addField(channel.getName() +" [In Progress]","Messages: "+ count,false);
				for (MessageEmbed.Field f : postFields) updatedEmbed.addField(f);

				//Update the message on the tracker
				tracker.editMessage(updatedEmbed.build()).queue();

				//Hard limit to avoid abusing API
				if (count >= 1000) break;
			}
		}

		return collectedMessages;
	}

	/**
	 * Collects all the messages from history for all channels and saves them to the source
	 * @param progressMessage message with the embed that gets updated
	 * @param earliestMessageID ID of the earliest message that the bot should look for
	 */
	private void saveChannelHistory(Message progressMessage, long earliestMessageID) {
		//Get the embed which needs to be updated
		MessageEmbed targetEmbed = progressMessage.getEmbeds().get(0);

		//Create the base for the updated embed
		EmbedBuilder embedBuilder = new EmbedBuilder();

		embedBuilder.setTitle(targetEmbed.getTitle(),targetEmbed.getUrl())
				.setThumbnail(targetEmbed.getThumbnail().getUrl()); //Set title/image
		embedBuilder.setFooter(targetEmbed.getFooter().getText(), targetEmbed.getFooter().getIconUrl()); //Set footer
		embedBuilder.setTimestamp(targetEmbed.getTimestamp()); //Set Timestamp

		//Track the fields after the current channel's
		List<MessageEmbed.Field> fieldsAfter = new LinkedList<>(targetEmbed.getFields());


		//Go through all of the text channels
		for (TextChannel channel : progressMessage.getGuild().getTextChannels()) {
			fieldsAfter.remove(0);

			//Get the messages
			List<me.Usoka.markov.Message> channelMessages = getChannelMessages(channel, progressMessage, embedBuilder, fieldsAfter, earliestMessageID);

			//Partition channelMessages up into batches of 200 for saving, to better track progress
			List<List<me.Usoka.markov.Message>> batches = ListUtils.partition(channelMessages, BATCH_SIZE);

			//Add each messages to the source material
			int numSaved = 0;
			for (List<me.Usoka.markov.Message> batch : batches) {
				//Update the embed
				EmbedBuilder updatedEmbed = new EmbedBuilder(embedBuilder);
				updatedEmbed.addField(channel.getName() +" [Saving Messages]",
						"Messages: "+ Integer.toString(channelMessages.size()) +"\r\nSaved: "+ numSaved,
						false);
				for (MessageEmbed.Field f : fieldsAfter) updatedEmbed.addField(f);
				progressMessage.editMessage(updatedEmbed.build()).queue();

				//Save the messages
				numSaved += markovCore.updateMaterial(new ArrayList<>(batch));
			}

			embedBuilder.addField(channel.getName() +" [Complete]",
					"Messages: "+ Integer.toString(channelMessages.size()) +"\r\nSaved: "+ numSaved,
					false);
		}
		progressMessage.editMessage(embedBuilder.build()).queue();
	}

	/**
	 * Go through the history of a given guild and save all messages from all TextChannels. <br/>
	 * Logs progress with a message embed in the channel specified
	 * @param guild Guild from which to get all the history
	 * @param logChannel <code>TextChannel</code> to make the embed logging the progress
	 */
	private void getAllChannelHistory(Guild guild, TextChannel logChannel, String messageContent, long earliestMessageID) {
		EmbedBuilder embedBuilder = new EmbedBuilder();
		//Set the embed Title to the guild name and the thumbnail the guild icon
		embedBuilder.setTitle(guild.getName(),"https://i.imgur.com/NqbaLqs.mp4").setThumbnail(guild.getIconUrl());
		embedBuilder.setFooter("Started", null).setTimestamp(java.time.OffsetDateTime.now());

		guild.getTextChannels().forEach((channel) -> embedBuilder.addField(channel.getName() +" [Queued]","Messages: 0",false));

		logChannel.sendMessage(messageContent).embed(embedBuilder.build()).queue((message) -> saveChannelHistory(message, earliestMessageID));
	}

	/**
	 * Handles when a new message is received in a TextChannel
	 * @param event event associated with the received message
	 */
	private void messageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot()) return; // ignore bot messages

		Message message = event.getMessage();
		String content = message.getContentRaw();

		// It's a command! Parse the command and it's arguments
		if (content.startsWith(COMMAND_INITIALIZER) && !content.startsWith(COMMAND_INITIALIZER+" ")) {
			//Read the command (from command init to first space)
			String command = content.split("\\s+")[0].substring(1);
			interpretCommand(event,
					command.toLowerCase(),
					(content.length() > command.length()+2)? content.substring(2 + command.length()) : "");
		}

		markovCore.saveMaterial(convertMessageClass(event.getMessage()));
	}

	/**
	 * Handles when a message has been update/edited in a TextChannel
	 * @param event event associated with the message update
	 */
	private void messageUpdated(GuildMessageUpdateEvent event) {
		if (event.getAuthor().isBot()) return; // ignore bot messages

		Message message = event.getMessage();
		markovCore.updateMaterial(convertMessageClass(message));
	}

	/**
	 * Handles when a message has been deleted in a TextChannel
	 * @param event event associated with the Delete action
	 */
	private void messageDelete(GuildMessageDeleteEvent event) {
		markovCore.deleteMessage(event.getMessageId());
	}



	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (!botRunning) {
			//Add the event to a queue, where it'll get resolved when the bot has started
			queuedEvents.add(event);
			return;
		}
		messageReceived(event);
	}

	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		if (!botRunning) {
			//Add the event to a queue, where it'll get resolved when the bot has started
			queuedEvents.add(event);
			return;
		}
		messageUpdated(event);
	}

	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		if (!botRunning) {
			//Add the event to a queue, where it'll get resolved when the bot has started
			queuedEvents.add(event);
			return;
		}
		messageDelete(event);
	}
}
