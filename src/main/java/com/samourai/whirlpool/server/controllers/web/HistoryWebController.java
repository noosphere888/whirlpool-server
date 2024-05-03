package com.samourai.whirlpool.server.controllers.web;

import com.samourai.wallet.api.explorer.ExplorerApi;
import com.samourai.whirlpool.server.config.WhirlpoolServerConfig;
import com.samourai.whirlpool.server.controllers.web.beans.WhirlpoolDashboardTemplateModel;
import com.samourai.whirlpool.server.persistence.to.MixLogTO;
import com.samourai.whirlpool.server.persistence.to.MixTO;
import com.samourai.whirlpool.server.persistence.to.shared.EntityCreatedUpdatedTO;
import com.samourai.whirlpool.server.services.DbService;
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
public class HistoryWebController {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String ENDPOINT = "/status/history";
  private static final int PAGE_SIZE = 100;

  private DbService dbService;
  private WhirlpoolServerConfig serverConfig;
  private ExplorerApi explorerApi;

  @Autowired
  public HistoryWebController(
      DbService dbService, WhirlpoolServerConfig serverConfig, ExplorerApi explorerApi) {
    this.dbService = dbService;
    this.serverConfig = serverConfig;
    this.explorerApi = explorerApi;
  }

  @RequestMapping(value = ENDPOINT, method = RequestMethod.GET)
  public String history(
      Model model,
      @PageableDefault(
              size = PAGE_SIZE,
              sort = EntityCreatedUpdatedTO.UPDATED,
              direction = Sort.Direction.DESC)
          Pageable pageable)
      throws Exception {
    new WhirlpoolDashboardTemplateModel(serverConfig, "history").apply(model);

    Page<MixTO> page = dbService.findMixs(pageable);
    model.addAttribute("page", page);
    model.addAttribute("urlExplorer", explorerApi.getUrlTx());
    model.addAttribute("mixStats", dbService.getMixStats());
    model.addAttribute("ENDPOINT", ENDPOINT);

    // getters used in template
    if (false) {
      for (MixTO mixTO : page) {
        mixTO.getMixId();
        mixTO.getAnonymitySet();
        mixTO.getNbMustMix();
        mixTO.getNbLiquidities();
        mixTO.getFeesAmount();
        mixTO.getFeesPrice();
        mixTO.getMixStatus();
        mixTO.getFailReason();
        mixTO.getFailInfo();
        MixLogTO mixLogTO = mixTO.getMixLog();
        mixLogTO.getTxid();
        mixLogTO.getRawTx();
      }
    }
    return "history";
  }
}
