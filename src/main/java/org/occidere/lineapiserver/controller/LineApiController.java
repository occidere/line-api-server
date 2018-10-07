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
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
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
@LineMessageHandler
public class LineApiController {
	@Autowired
	private LineMessagingClient lineMessagingClient;

	@Value("${line.bot.id}")
	private String dailyOmgId;

	@Value("${dailyomg.url}")
	private String dailyOmgUrl;

	@EventMapping
	public Message handleRequestImageEvent(MessageEvent<TextMessageContent> event) {
		String replyToken = event.getReplyToken();
		String text = event.getMessage().getText();

		log.info("replyToken: {}", replyToken);
		log.info("text: {}", text);

		try {
			int range = checkAndGetRange(text); // 잘못된 형식이면 IllegalArgumentException 발생
			List<LinkedHashMap<String, String>> titleImageList = requestTitleImageList(range);

			log.info("list size: {}", titleImageList.size());

			for (LinkedHashMap<String, String> titleImageMap : titleImageList) {
				replyImage(replyToken, titleImageMap); // 요청한 사람에게만 리플라이
			}

			return new TextMessage("Done!");
		} catch (Exception e) {
			return new TextMessage(e.getMessage());
		}
	}

	private List<LinkedHashMap<String, String>> requestTitleImageList(int range) {
		return new RestTemplate().getForObject(dailyOmgUrl + "/request/ohmygirl/image?range=" + range, List.class);
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
			pushImage(titleImageMap); // 전체 구독자 대상 푸시
		}

		return "done";
	}

	/**
	 * DailyOMG 에서 스케쥴 등 텍스트 전송 시 호출하는 주소
	 *
	 * @param body
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/push/text", method = RequestMethod.POST)
	public String pushTextEvent(@RequestBody List<LinkedHashMap<String, String>> body) throws Exception {
		log.info("body: {}", body);

		StringBuilder text = new StringBuilder();
		String date = "";
		for (LinkedHashMap<String, String> dateScheduleMap : body) {
			Map.Entry<String, String> dateScheduleEntry = dateScheduleMap.entrySet().iterator().next();

			String tmpDate = dateScheduleEntry.getKey();
			String schedule = dateScheduleEntry.getValue();

			if(StringUtils.equals(date, tmpDate) == false) {
				date = tmpDate;
				text.append("[").append(date).append("]\n");
			}

			text.append(schedule).append("\n");

		}
		pushText(text.toString()); // 전체 구독자 대상 푸시

		return text.toString();
	}

	/********** healt **********/
	@RequestMapping(value = "/health", method = RequestMethod.GET)
	public long healthCheck() {
		log.info("Health Check!");
		return System.currentTimeMillis();
	}

	private void pushImage(LinkedHashMap<String, String> titleImageMap) throws Exception {
		String titleImageArr[] = parseTitleImageMap(titleImageMap);
		String title = titleImageArr[0];
		String url = titleImageArr[1];

		Message message = new ImageMessage(url, url);
		PushMessage pushMessage = new PushMessage(dailyOmgId, message);

		BotApiResponse response = lineMessagingClient
				.pushMessage(pushMessage)
				.get();

		log.info("{}", response.toString());
	}

	private void replyImage(String reployToken, LinkedHashMap<String, String> titleImageMap) throws Exception {
		String titleImageArr[] = parseTitleImageMap(titleImageMap);
		String title = titleImageArr[0];
		String url = titleImageArr[1];

		Message message = new ImageMessage(url, url);
		ReplyMessage replyMessage = new ReplyMessage(reployToken, message);

		BotApiResponse response = lineMessagingClient
				.replyMessage(replyMessage)
				.get();

		log.info("{}", response.toString());
	}

	private void pushText(String text) throws Exception {
		Message message = new TextMessage(text);
		PushMessage pushMessage = new PushMessage(dailyOmgId, message);

		BotApiResponse response = lineMessagingClient
				.pushMessage(pushMessage)
				.get();

		log.info("{}", response.toString());
	}

	/**
	 * titleImageMap에서 title과 url을 추출하여 배열에 담아 반환.
	 * 이미지 url이 http 인 경우 line api 전송시 에러가 발생하기에 강제로 https로 변경하여 반환한다.
	 * @param titleImageMap title과 url 이 담긴 LinkedHashMap 객체
	 * @return arr[0] = title, arr[1] = url
	 */
	private String[] parseTitleImageMap(LinkedHashMap<String, String> titleImageMap) {
		Map.Entry<String, String> entry = titleImageMap.entrySet().iterator().next();

		String title = entry.getKey();
		String url = entry.getValue();

		log.info("title: {}", title);
		log.info("url: {}", url);

		if(StringUtils.startsWith(url, "http://")) {
			url = StringUtils.replace(url, "http://", "https://");
			log.warn("Force change http -> https");
		}

		return new String[] { title, url };
	}


	private int checkAndGetRange(String text) throws IllegalArgumentException {
		text = StringUtils.trim(text);

		if(StringUtils.isBlank(text)) {
			throw new IllegalArgumentException("Blank Text!");
		}

		if(StringUtils.isNumeric(text) == false) {
			throw new IllegalArgumentException("Not Numeric!");
		}

		int range = Integer.parseInt(text);
		if(range < 0 || range > 3) {
			throw new IllegalArgumentException("Out of range! please input 0 ~ 3 value!");
		}

		return range;
	}

}
