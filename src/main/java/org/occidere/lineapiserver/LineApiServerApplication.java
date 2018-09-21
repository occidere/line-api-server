package org.occidere.lineapiserver;

import com.google.common.io.ByteStreams;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.client.MessageContentResponse;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.ImageMessageContent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
@LineMessageHandler
@RestController
public class LineApiServerApplication {

	@Autowired
	private LineMessagingClient lineMessagingClient;
	private static Path downloadedContentDir;

	@EventMapping
	public Message handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
		String replyToken = event.getReplyToken();
		TextMessageContent textMessageContent = event.getMessage();
		String originMsgText = event.getMessage().getText();

		Source source = event.getSource();
		String senderId = source.getSenderId();
		String userId = source.getUserId();

		log.info("event: {}", event);
		log.info("replyToken: " + replyToken);
		log.info("textMessageContent: " + textMessageContent);
		log.info("text: " + originMsgText);
		log.info("senderId: " + senderId);
		log.info("userId: " + userId);


		return new TextMessage(getDate() + " - " + originMsgText);
	}

	@EventMapping
	public void handleImageMessageEvent(MessageEvent<ImageMessageContent> event) throws IOException {
		// You need to install ImageMagick
		handleHeavyContent(
				event.getReplyToken(),
				event.getMessage().getId(),
				responseBody -> {
					DownloadedContent jpg = saveContent("jpg", responseBody);
					DownloadedContent previewImg = createTempFile("jpg");
					system(
							"convert",
							"-resize", "240x",
							jpg.path.toString(),
							previewImg.path.toString());
					reply(event.getReplyToken(),
							new ImageMessage(jpg.getUri(), jpg.getUri()));
				});
	}

	private String getDate() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}

	private void handleHeavyContent(String replyToken, String messageId, Consumer<MessageContentResponse> messageConsumer) {
		MessageContentResponse response;
		try {
			response = lineMessagingClient.getMessageContent(messageId).get();
		} catch (Exception e) {
			reply(replyToken, new TextMessage("Can't get image: " + e.getMessage()));
			throw new RuntimeException(e);
		}
		messageConsumer.accept(response);
	}

	private void reply(String replyToken, Message message) {
		reply(replyToken, Collections.singletonList(message));
	}

	private void reply(String replyToken, List<Message> messages) {
		try {
			BotApiResponse apiResponse = lineMessagingClient
					.replyMessage(new ReplyMessage(replyToken, messages))
					.get();
			log.info("Sent Messages: {}", apiResponse);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String createUri(String path) {
		return ServletUriComponentsBuilder.fromCurrentContextPath()
				.path(path).build()
				.toUriString();
	}

	private void system(String... args) {
		ProcessBuilder processBuilder = new ProcessBuilder(args);
		try {
			Process start = processBuilder.start();
			int i = start.waitFor();
			log.info("result: {} =>  {}", Arrays.toString(args), i);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InterruptedException e) {
			log.info("Interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	private static DownloadedContent saveContent(String ext, MessageContentResponse responseBody) {
		log.info("Got content-type: {}", responseBody);

		DownloadedContent tempFile = createTempFile(ext);
		try (OutputStream outputStream = Files.newOutputStream(tempFile.path)) {
			ByteStreams.copy(responseBody.getStream(), outputStream);
			log.info("Saved {}: {}", ext, tempFile);
			return tempFile;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static DownloadedContent createTempFile(String ext) {
		String fileName = LocalDateTime.now().toString() + '-' + UUID.randomUUID().toString() + '.' + ext;
		Path tempFile = LineApiServerApplication.downloadedContentDir.resolve(fileName);
		tempFile.toFile().deleteOnExit();
		return new DownloadedContent(
				tempFile,
				createUri("/downloaded/" + tempFile.getFileName()));
	}

	@Value
	public static class DownloadedContent {
		Path path;
		String uri;
	}


	public static void main(String[] args) throws Exception {
		downloadedContentDir = Files.createTempDirectory("line-bot");
		SpringApplication.run(LineApiServerApplication.class, args);
	}
}
