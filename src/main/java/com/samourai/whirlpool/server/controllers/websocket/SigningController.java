package com.samourai.whirlpool.server.controllers.websocket;

import com.samourai.whirlpool.protocol.WhirlpoolEndpoint;
import com.samourai.whirlpool.protocol.websocket.messages.SigningRequest;
import com.samourai.whirlpool.server.services.ExportService;
import com.samourai.whirlpool.server.services.SigningService;
import com.samourai.whirlpool.server.services.WSMessageService;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class SigningController extends AbstractWebSocketController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private SigningService signingService;

  @Autowired
  public SigningController(
      WSMessageService WSMessageService,
      ExportService exportService,
      SigningService signingService) {
    super(WSMessageService, exportService);
    this.signingService = signingService;
  }

  @MessageMapping(WhirlpoolEndpoint.WS_SIGNING)
  public void signing(
      @Payload SigningRequest payload, Principal principal, StompHeaderAccessor headers)
      throws Exception {
    validateHeaders(headers);

    String username = principal.getName();
    if (log.isDebugEnabled()) {
      log.debug("(<) [" + payload.mixId + "] " + username + " " + headers.getDestination());
    }

    // signing
    signingService.signing(payload.mixId, username, payload.witnesses64);
  }

  @MessageExceptionHandler
  public void handleException(
      Exception exception, Principal principal, SimpMessageHeaderAccessor messageHeaderAccessor) {
    super.handleException(exception, principal, messageHeaderAccessor, "SIGNING:ERROR");
  }
}
