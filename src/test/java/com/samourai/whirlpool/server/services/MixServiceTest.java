package com.samourai.whirlpool.server.services;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.samourai.whirlpool.protocol.websocket.notifications.MixStatus;
import com.samourai.whirlpool.server.beans.ConfirmedInput;
import com.samourai.whirlpool.server.beans.FailReason;
import com.samourai.whirlpool.server.beans.Mix;
import com.samourai.whirlpool.server.beans.RegisteredInput;
import com.samourai.whirlpool.server.beans.rpc.TxOutPoint;
import com.samourai.whirlpool.server.integration.AbstractIntegrationTest;
import com.samourai.whirlpool.server.utils.Utils;
import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class MixServiceTest extends AbstractIntegrationTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void isConfirmInputReady_noLiquidity() throws Exception {
    MixService spyMixService = Mockito.spy(mixService);
    long denomination = 200000000;
    long feeValue = 10000000;
    long minerFeeMin = 100;
    long minerFeeCap = 9500;
    long minerFeeMax = 10000;
    long minRelayFee = 510;
    int mustMixMin = 1;
    int liquidityMin = 0;
    int anonymitySet = 2;
    Mix mix =
        __nextMix(
            denomination,
            feeValue,
            minerFeeMin,
            minerFeeCap,
            minerFeeMax,
            minRelayFee,
            mustMixMin,
            liquidityMin,
            anonymitySet);

    long mustMixValue = 200000400;

    // 0 mustMix => false
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 1 mustMix => false
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix1",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHash1"));
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 2 mustMix => true
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix2",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHash2"));
    Assertions.assertTrue(spyMixService.isConfirmInputReady(mix));
  }

  @Test
  public void isConfirmInputReady_withLiquidityBefore() throws Exception {
    MixService spyMixService = Mockito.spy(mixService);
    long denomination = 200000000;
    long feeValue = 10000000;
    long minerFeeMin = 100;
    long minerFeeCap = 9500;
    long minerFeeMax = 10000;
    long minRelayFee = 510;
    int mustMixMin = 1;
    int liquidityMin = 0;
    int anonymitySet = 2;
    Mix mix =
        __nextMix(
            denomination,
            feeValue,
            minerFeeMin,
            minerFeeCap,
            minerFeeMax,
            minRelayFee,
            mustMixMin,
            liquidityMin,
            anonymitySet);

    long mustMixValue = 200000255;

    // 0 liquidity => false
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 1 liquidity => false
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "liquidity1",
                true,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHashL1"));
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 2 liquidity => false : minMustMix not reached
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "liquidity2",
                true,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHashL2"));
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 1 mustMix => false : minMustMix reached but minerFeeMix not reached
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix1",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHashM1"));
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 2 mustMix => true : minerFeeMix reached
    mix.registerInput(
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix2",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHashM2"));
    Assertions.assertTrue(spyMixService.isConfirmInputReady(mix));
  }

  @Test
  public void isConfirmInputReady_spentWhileRegisterInput() throws Exception {
    MixService spyMixService = Mockito.spy(mixService);
    mixService = spyMixService;

    long denomination = 200000000;
    long feeValue = 10000000;
    long minerFeeMin = 100;
    long minerFeeCap = 9500;
    long minerFeeMax = 10000;
    long minRelayFee = 510;
    int mustMixMin = 1;
    int liquidityMin = 0;
    int anonymitySet = 2;
    Mix mix =
        __nextMix(
            denomination,
            feeValue,
            minerFeeMin,
            minerFeeCap,
            minerFeeMax,
            minRelayFee,
            mustMixMin,
            liquidityMin,
            anonymitySet);

    long mustMixValue = 200000400;

    // 0 mustMix => false
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 1 mustMix => false
    ConfirmedInput mustMix1 =
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix1",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHash1");
    mix.registerInput(mustMix1);
    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix));

    // 2 mustMix => true
    ConfirmedInput mustMix2 =
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix2",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHash2");
    mix.registerInput(mustMix2);
    Assertions.assertTrue(spyMixService.isConfirmInputReady(mix));
    Assertions.assertEquals(2, mix.getNbInputs());

    String blameIdentifierMustMix1 = Utils.computeBlameIdentitifer(mustMix1.getRegisteredInput());
    Assertions.assertTrue(dbService.findBlames(blameIdentifierMustMix1).isEmpty()); // no blame

    // mustMix spent in meantime => false
    TxOutPoint out1 = mustMix1.getRegisteredInput().getOutPoint();
    rpcClientService.mockSpentOutput(out1.getHash(), out1.getIndex());

    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix)); // mix not valid anymore
    Assertions.assertEquals(1, mix.getNbInputs());

    // no blame as mix was not started yet
    Assertions.assertEquals(dbService.findBlames(blameIdentifierMustMix1).size(), 0);

    // 2 mustMix => true
    ConfirmedInput mustMix3 =
        new ConfirmedInput(
            new RegisteredInput(
                mix.getPool().getPoolId(),
                "mustMix3",
                false,
                generateOutPoint(mustMixValue),
                "127.0.0.1",
                null),
            "userHash3");
    mix.registerInput(mustMix3);
    Assertions.assertTrue(spyMixService.isConfirmInputReady(mix));
    Assertions.assertEquals(2, mix.getNbInputs());

    // REGISTER_OUTPUT
    mix.setMixStatusAndTime(MixStatus.REGISTER_OUTPUT);

    // mustMix spent in meantime => false
    TxOutPoint out3 = mustMix3.getRegisteredInput().getOutPoint();
    rpcClientService.mockSpentOutput(out3.getHash(), out3.getIndex());

    Assertions.assertFalse(spyMixService.isConfirmInputReady(mix)); // mix not valid + trigger fail

    // mix failed
    Assertions.assertEquals(MixStatus.FAIL, mix.getMixStatus());
    Assertions.assertEquals(FailReason.SPENT, mix.getFailReason());
    Assertions.assertEquals(out3.getHash() + ":" + out3.getIndex(), mix.getFailInfo());

    // blame as mix was already started
    String blameIdentifierMustMix3 = Utils.computeBlameIdentitifer(mustMix3.getRegisteredInput());
    Assertions.assertEquals(dbService.findBlames(blameIdentifierMustMix3).size(), 1);
  }

  private TxOutPoint generateOutPoint(long value) {
    TxOutPoint txOutPoint =
        new TxOutPoint(
            Utils.getRandomString(65),
            0,
            value,
            99,
            null,
            testUtils.generateSegwitAddress().getBech32AsString());
    return txOutPoint;
  }
}
