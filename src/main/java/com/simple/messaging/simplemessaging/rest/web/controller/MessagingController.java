package com.simple.messaging.simplemessaging.rest.web.controller;

import com.simple.messaging.simplemessaging.entity.common.BaseResponse;
import com.simple.messaging.simplemessaging.entity.dao.MessageData;
import com.simple.messaging.simplemessaging.entity.web.request.MessageDataRequest;
import com.simple.messaging.simplemessaging.rest.web.component.WebBeanMapper;
import com.simple.messaging.simplemessaging.rest.web.component.WebMapper;
import com.simple.messaging.simplemessaging.rest.web.constant.ApiPath;
import com.simple.messaging.simplemessaging.service.api.MessagingService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author : Mario T Juzar
 * @since : 07/11/19
 */
@RestController
@RequestMapping(ApiPath.MESSAGES)
public class MessagingController {

  private static final WebBeanMapper MAPPER = WebMapper.BEAN_MAPPER;

  private MessagingService messagingService;

  private SimpMessagingTemplate template;

  @Autowired
  public void setMessagingService(MessagingService messagingService) {
    this.messagingService = messagingService;
  }

  @Autowired
  public void setTemplate(SimpMessagingTemplate template) {
    this.template = template;
  }

  @PostMapping
  public Mono<BaseResponse<MessageData>> submit(@RequestBody MessageDataRequest messageData) {
    return Mono.defer(() -> messagingService.submitMessage(MAPPER.map(messageData))
        .map(response -> BaseResponse.construct(
            HttpStatus.OK.name(),
            HttpStatus.OK.getReasonPhrase(),
            response,
            null
        ))
        .map(this::sendToBroker)
    ).subscribeOn(Schedulers.elastic());
  }

  @GetMapping(ApiPath.COLLECT)
  public Mono<BaseResponse<List<MessageData>>> collect() {
    return Mono.defer(() -> messagingService.collect()
        .map(response -> BaseResponse.construct(
            HttpStatus.OK.name(),
            HttpStatus.OK.getReasonPhrase(),
            response,
            null
        ))
    ).subscribeOn(Schedulers.elastic());
  }

  private <T> BaseResponse<T> sendToBroker(BaseResponse<T> response) {
    template.convertAndSend("/topic/message", response);
    return response;
  }
}
