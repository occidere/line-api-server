package org.occidere.lineapiserver;

import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.event.source.Source;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
@Slf4j
@LineMessageHandler
@RestController
public class LineApiServerApplication {

	@EventMapping
	public Message handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
		String replyToken = event.getReplyToken();
		TextMessageContent textMessageContent = event.getMessage();
		String originMsgText = event.getMessage().getText();

		Source source = event.getSource();
		String senderId = source.getSenderId();
		String userId = source.getUserId();

		System.out.println(event);
		System.out.println("replyToken: " + replyToken);
		System.out.println("textMessageContent: " + textMessageContent);
		System.out.println("text: " + originMsgText);
		System.out.println("senderId: " + senderId);
		System.out.println("userId: " + userId);

		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

		return new TextMessage(date + " - " + originMsgText);
	}

	@EventMapping
	public void handleDefaultMessageEvent(Event event) {
		System.out.println("event: " + event);
	}


	public static void main(String[] args) {
		SpringApplication.run(LineApiServerApplication.class, args);
	}
}
