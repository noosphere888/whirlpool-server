package com.samourai.whirlpool.server.controllers.web;

import com.samourai.javaserver.web.controllers.AbstractLoginWebController;
import com.samourai.javaserver.web.models.LoginTemplateModel;
import com.samourai.whirlpool.server.config.WhirlpoolServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LoginWebController extends AbstractLoginWebController {
  public static final String ENDPOINT = "/status/login-form";
  public static final String PROCESS_ENDPOINT = "/status/doLogin";

  private WhirlpoolServerConfig serverConfig;

  @Autowired
  public LoginWebController(WhirlpoolServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  @RequestMapping(value = ENDPOINT, method = RequestMethod.GET)
  public String login(Model model) {
    return super.login(
        model,
        new LoginTemplateModel(serverConfig.getName(), PROCESS_ENDPOINT, serverConfig.getName()));
  }
}
