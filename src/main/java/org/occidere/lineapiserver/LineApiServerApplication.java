package org.occidere.lineapiserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.ImageMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.response.BotApiResponse;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SpringBootApplication
@Slf4j
@LineMessageHandler
@RestController
public class LineApiServerApplication {

	@Autowired
	private LineMessagingClient lineMessagingClient;

	@EventMapping
	public Message handleImageRequestEvent(MessageEvent<TextMessageContent> event) throws Exception {
		String replyToken = event.getReplyToken();
		String jsonMsg = event.getMessage().getText();

		Map<String, String> jsonMap = new ObjectMapper().readValue(jsonMsg, HashMap.class);

		String title = jsonMap.get("title");
		String url = jsonMap.get("url");

		log.info("event: {}", event);

		Message message;
		ReplyMessage replyMessage;

		message = new ImageMessage(url, url);
		replyMessage = new ReplyMessage(replyToken, message);
		BotApiResponse response;

		response = lineMessagingClient.replyMessage(replyMessage).get();
		log.info("Response : " + response);

		return new TextMessage(title);
	}


	public static void main(String[] args) {
		SpringApplication.run(LineApiServerApplication.class, args);
	}
}
