package org.occidere.lineapiserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linecorp.bot.client.LineMessagingClient;
import com.linecorp.bot.model.PushMessage;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

		LinkedHashMap<String, String> titleImageMap = new ObjectMapper().readValue(jsonMsg, LinkedHashMap.class);

		Map.Entry<String, String> entry = titleImageMap.entrySet().iterator().next();

		String title = entry.getKey();
		String url = entry.getValue();

		log.info("title: {}", title);
		log.info("url: {}", url);
		log.info("event: {}", event);

		Message message;
		ReplyMessage replyMessage;

//		message = new ImageMessage(url, url);
//		replyMessage = new ReplyMessage(replyToken, message);
//		BotApiResponse response = lineMessagingClient
//				.replyMessage(replyMessage)
//				.get();
//		log.info("Response : " + response);

		return new TextMessage(title);
	}

	private void pushImage(LinkedHashMap<String, String> titleImageMap) throws Exception {
		Map.Entry<String, String> entry = titleImageMap.entrySet().iterator().next();

		String title = entry.getKey();
		String url = entry.getValue();
		String to = "U6f1932eaf6267ca62d11f797a22db6d4";

		log.info("title: {}", title);
		log.info("url: {}", url);

		Message message = new ImageMessage(url, url);
		PushMessage pushMessage = new PushMessage(to, message);

		BotApiResponse response = lineMessagingClient
				.pushMessage(pushMessage)
				.get();

		log.info("{}", response.toString());
	}

	@RequestMapping(value = "/push/image", method = RequestMethod.POST)
	public String pushImageEvent(@RequestBody List<LinkedHashMap<String, String>> body) throws Exception {
		log.info("body: {}", body);

		for(LinkedHashMap<String, String> titleImageMap : body) {
			pushImage(titleImageMap);
		}

		return "done";
	}

	public static void main(String[] args) {
		SpringApplication.run(LineApiServerApplication.class, args);
	}
}
