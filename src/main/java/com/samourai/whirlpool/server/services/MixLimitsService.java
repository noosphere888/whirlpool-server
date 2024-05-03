package com.samourai.whirlpool.server.services;

import com.samourai.whirlpool.server.beans.*;
import com.samourai.whirlpool.server.config.WhirlpoolServerConfig;
import com.samourai.whirlpool.server.utils.timeout.ITimeoutWatcherListener;
import com.samourai.whirlpool.server.utils.timeout.TimeoutWatcher;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MixLimitsService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private MixService mixService;;
  private PoolService poolService;
  private BlameService blameService;
  private WhirlpoolServerConfig whirlpoolServerConfig;

  private Map<String, TimeoutWatcher> limitsWatchers;

  @Autowired
  public MixLimitsService(
      PoolService poolService,
      BlameService blameService,
      WhirlpoolServerConfig whirlpoolServerConfig) {
    this.poolService = poolService;
    this.blameService = blameService;
    this.whirlpoolServerConfig = whirlpoolServerConfig;

    this.__reset();
  }

  // avoids circular reference
  public void setMixService(MixService mixService) {
    this.mixService = mixService;
  }

  private TimeoutWatcher getLimitsWatcher(Mix mix) {
    String mixId = mix.getMixId();
    return limitsWatchers.get(mixId);
  }

  public synchronized void manage(Mix mix) {
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    if (limitsWatcher == null) {
      String mixId = mix.getMixId();
      limitsWatcher = computeLimitsWatcher(mix);
      this.limitsWatchers.put(mixId, limitsWatcher);
    }
  }

  public void unmanage(Mix mix) {
    String mixId = mix.getMixId();
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    if (limitsWatcher != null) {
      limitsWatcher.stop();
      limitsWatchers.remove(mixId);
    }
  }

  public void onMixStatusChange(Mix mix) {
    // reset timeout for new mixStatus
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    if (limitsWatcher != null) { // may be null for tests
      limitsWatcher.resetTimeout();
    }
  }

  private TimeoutWatcher computeLimitsWatcher(Mix mix) {
    ITimeoutWatcherListener listener =
        new ITimeoutWatcherListener() {
          @Override
          public Long computeTimeToWait(TimeoutWatcher timeoutWatcher) {
            long elapsedTime = timeoutWatcher.computeElapsedTime();

            Long timeToWait = null;
            switch (mix.getMixStatus()) {
              case CONFIRM_INPUT:
                // timeout before adding more liquidities
                long waitTime = whirlpoolServerConfig.getRegisterInput().getConfirmInterval();
                timeToWait = waitTime * 1000 - elapsedTime;
                break;

              case REGISTER_OUTPUT:
                timeToWait =
                    whirlpoolServerConfig.getRegisterOutput().getTimeout() * 1000 - elapsedTime;
                break;

              case SIGNING:
                timeToWait = whirlpoolServerConfig.getSigning().getTimeout() * 1000 - elapsedTime;
                break;

              case REVEAL_OUTPUT:
                timeToWait =
                    whirlpoolServerConfig.getRevealOutput().getTimeout() * 1000 - elapsedTime;
                break;

              default:
                // no timer
                if (log.isDebugEnabled()) {
                  log.debug(
                      "limitsWatcher.computeTimeToWait => no timer: mixStatus="
                          + mix.getMixStatus());
                }
                break;
            }
            return timeToWait;
          }

          @Override
          public void onTimeout(TimeoutWatcher timeoutWatcher) {
            if (log.isTraceEnabled()) {
              log.trace("limitsWatcher.onTimeout: " + mix.getMixId() + " " + mix.getMixStatus());
            }
            switch (mix.getMixStatus()) {
              case CONFIRM_INPUT:
                poolService.confirmInputs(mix, mixService);
                break;

              case REGISTER_OUTPUT:
                mixService.onTimeoutRegisterOutput(mix);
                break;

              case REVEAL_OUTPUT:
                mixService.onTimeoutRevealOutput(mix);
                break;

              case SIGNING:
                blameForSigningAndResetMix(mix);
                break;

              default:
                if (log.isDebugEnabled()) {
                  log.debug("limitsWatcher.onTimeout => ignored: mixStatus=" + mix.getMixStatus());
                }
            }
          }
        };

    TimeoutWatcher mixLimitsWatcher =
        new TimeoutWatcher(listener, "limitsWatcher-" + mix.getMixId());
    return mixLimitsWatcher;
  }

  public void blameForSigningAndResetMix(Mix mix) {
    log.info(
        "["
            + mix.getMixId()
            + "] SIGNING time over (mix failed, blaming users who didn't sign...)");

    // blame users who didn't sign
    Set<ConfirmedInput> confirmedInputsToBlame =
        mix.getInputs()
            .parallelStream()
            .filter(input -> !mix.getSignedByUsername(input.getRegisteredInput().getUsername()))
            .collect(Collectors.toSet());
    List<String> outpointKeysToBlame = new ArrayList<>();
    for (ConfirmedInput confirmedInputToBlame : confirmedInputsToBlame) {
      blameService.blame(confirmedInputToBlame.getRegisteredInput(), BlameReason.SIGNING, mix);
      outpointKeysToBlame.add(confirmedInputToBlame.getRegisteredInput().getOutPoint().toKey());
    }

    // reset mix
    String outpointKeysToBlameStr = StringUtils.join(outpointKeysToBlame, ";");
    mixService.goFail(mix, FailReason.FAIL_SIGNING, outpointKeysToBlameStr);
  }

  public Long getLimitsWatcherTimeToWait(Mix mix) {
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    if (limitsWatcher != null) {
      return limitsWatcher.computeTimeToWait();
    }
    return null;
  }

  public Long getLimitsWatcherElapsedTime(Mix mix) {
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    if (limitsWatcher != null) {
      return limitsWatcher.computeElapsedTime();
    }
    return null;
  }

  public void __simulateElapsedTime(Mix mix, long elapsedTimeSeconds) {
    String mixId = mix.getMixId();
    log.info("__simulateElapsedTime for mixId=" + mixId);
    TimeoutWatcher limitsWatcher = getLimitsWatcher(mix);
    limitsWatcher.__simulateElapsedTime(elapsedTimeSeconds);
  }

  public void __reset() {
    if (limitsWatchers != null) {
      limitsWatchers.values().forEach(watcher -> watcher.stop());
    }

    this.limitsWatchers = new ConcurrentHashMap<>();
  }
}
