package org.occidere.lineapiserver.controller;

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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class LineApiController {
	@Autowired
	private LineMessagingClient lineMessagingClient;

	@Value("${line.bot.id}")
	private String dailyOmgId;

	@EventMapping
	public Message handleRequestImageEvent(MessageEvent<TextMessageContent> event) {
		String replyToken = event.getReplyToken();
		String text = event.getMessage().getText();

		log.info("text: {}", text);

		try {
			if (StringUtils.isNumeric(text) == false) {
				throw new RuntimeException("Not Numeric: " + text);
			}

			int range = Integer.parseInt(text);
			List<LinkedHashMap<String, String>> titleImageList = requestTitleImageList(range);

			for (LinkedHashMap<String, String> titleImageMap : titleImageList) {
				replyImage(replyToken, titleImageMap);
			}

			return new TextMessage("Done!");
		} catch (Exception e) {
			return new TextMessage(e.getMessage());
		}
	}

	private List<LinkedHashMap<String, String>> requestTitleImageList(int range) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject("http://49.236.134.11:8080/request/ohmygirl/image?range=" + range, List.class);
	}

	/**
	 * DailyOMG에서 호출하는 주소
	 *
	 * @param body
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/push/image", method = RequestMethod.POST)
	public String pushImageEvent(@RequestBody List<LinkedHashMap<String, String>> body) throws Exception {
		log.info("body: {}", body);

		for (LinkedHashMap<String, String> titleImageMap : body) {
			pushImage(titleImageMap);
		}

		return "done";
	}

	private void pushImage(LinkedHashMap<String, String> titleImageMap) throws Exception {
		Map.Entry<String, String> entry = titleImageMap.entrySet().iterator().next();

		String title = entry.getKey();
		String url = entry.getValue();

		log.info("title: {}", title);
		log.info("url: {}", url);

		Message message = new ImageMessage(url, url);
		PushMessage pushMessage = new PushMessage(dailyOmgId, message);

		BotApiResponse response = lineMessagingClient
				.pushMessage(pushMessage)
				.get();

		log.info("{}", response.toString());
	}

	private void replyImage(String reployToken, LinkedHashMap<String, String> titleImageMap) throws Exception {
		Map.Entry<String, String> entry = titleImageMap.entrySet().iterator().next();

		String title = entry.getKey();
		String url = entry.getValue();

		log.info("title: {}", title);
		log.info("url: {}", url);

		Message message = new ImageMessage(url, url);
		ReplyMessage replyMessage = new ReplyMessage(reployToken, message);

		BotApiResponse response = lineMessagingClient
				.replyMessage(replyMessage)
				.get();

		log.info("{}", response.toString());
	}

}
