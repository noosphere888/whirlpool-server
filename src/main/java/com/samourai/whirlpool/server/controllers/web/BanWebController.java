package com.samourai.whirlpool.server.controllers.web;

import com.samourai.wallet.api.explorer.ExplorerApi;
import com.samourai.whirlpool.server.config.WhirlpoolServerConfig;
import com.samourai.whirlpool.server.controllers.web.beans.WhirlpoolDashboardTemplateModel;
import com.samourai.whirlpool.server.persistence.to.BanTO;
import com.samourai.whirlpool.server.persistence.to.shared.EntityCreatedTO;
import com.samourai.whirlpool.server.services.BanService;
import java.lang.invoke.MethodHandles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class BanWebController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ENDPOINT = "/status/ban";
  private static final int PAGE_SIZE = 100;

  private BanService banService;
  private WhirlpoolServerConfig serverConfig;
  private ExplorerApi explorerApi;

  @Autowired
  public BanWebController(
      BanService banService, WhirlpoolServerConfig serverConfig, ExplorerApi explorerApi) {
    this.banService = banService;
    this.serverConfig = serverConfig;
    this.explorerApi = explorerApi;
  }

  @RequestMapping(value = ENDPOINT, method = RequestMethod.GET)
  public String ban(
      Model model,
      @PageableDefault(
              size = PAGE_SIZE,
              sort = EntityCreatedTO.CREATED,
              direction = Sort.Direction.DESC)
          Pageable pageable)
      throws Exception {
    new WhirlpoolDashboardTemplateModel(serverConfig, "ban").apply(model);

    Page<BanTO> page = banService.findActiveBans(pageable);
    model.addAttribute("page", page);
    model.addAttribute("urlExplorer", explorerApi.getUrlTx());
    model.addAttribute("ENDPOINT", ENDPOINT);
    model.addAttribute("banConfig", serverConfig.getBan());

    // getters used in template
    if (false) {
      for (BanTO banTO : page) {
        banTO.computeBanMessage();
        banTO.getCreated();
        banTO.getExpiration();
        banTO.getIdentifier();
        banTO.getNotes();
        serverConfig.getBan().getBlames();
        serverConfig.getBan().getExpiration();
        serverConfig.getBan().getPeriod();
      }
    }
    return "ban";
  }
}
