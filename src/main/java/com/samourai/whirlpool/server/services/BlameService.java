package com.samourai.whirlpool.server.services;

import com.google.common.collect.ImmutableMap;
import com.samourai.whirlpool.server.beans.BlameReason;
import com.samourai.whirlpool.server.beans.Mix;
import com.samourai.whirlpool.server.beans.RegisteredInput;
import com.samourai.whirlpool.server.beans.export.ActivityCsv;
import com.samourai.whirlpool.server.persistence.to.BlameTO;
import com.samourai.whirlpool.server.utils.Utils;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BlameService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private DbService dbService;
  private BanService banService;
  private ExportService exportService;
  private MetricService metricService;

  @Autowired
  public BlameService(
      DbService dbService,
      BanService banService,
      ExportService exportService,
      MetricService metricService) {
    this.dbService = dbService;
    this.banService = banService;
    this.exportService = exportService;
    this.metricService = metricService;
  }

  public void blame(RegisteredInput registeredInput, BlameReason reason, Mix mix) {
    // blame
    String identifier = Utils.computeBlameIdentitifer(registeredInput);
    dbService.saveBlame(identifier, reason, mix.getMixId(), registeredInput.getIp());

    // notify banService
    List<BlameTO> blames = dbService.findBlames(identifier);
    banService.onBlame(registeredInput, identifier, blames);

    // log activity
    ActivityCsv activityCsv =
        new ActivityCsv(
            "BLAME",
            mix.getPool().getPoolId(),
            registeredInput,
            ImmutableMap.of("reason", reason.name()),
            null);
    exportService.exportActivity(activityCsv);
    metricService.onBlame(registeredInput);
  }
}
