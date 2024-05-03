package com.samourai.whirlpool.server.integration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.samourai.wallet.bip47.rpc.BIP47Wallet;
import com.samourai.wallet.bip47.rpc.PaymentCode;
import com.samourai.wallet.bipWallet.BipWallet;
import com.samourai.wallet.hd.BIP_WALLET;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.server.beans.Mix;
import com.samourai.whirlpool.server.beans.rpc.TxOutPoint;
import com.samourai.whirlpool.server.utils.AssertMultiClientManager;
import com.samourai.whirlpool.server.utils.BIP47WalletAndHDWallet;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class WhirlpoolSimpleIntegrationTest extends AbstractIntegrationTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    serverConfig.setTestMode(true);
  }

  @Test
  public void whirlpool_manual_bip47() throws Exception {
    NetworkParameters params = cryptoService.getNetworkParameters();

    // init BIP47 wallet for input
    BIP47WalletAndHDWallet inputWallets = testUtils.generateWallet();
    BIP47Wallet bip47InputWallet = inputWallets.getBip47Wallet();
    HD_Wallet inputWallet = inputWallets.getHdWallet();

    // init BIP47 wallet for output
    BIP47Wallet bip47OutputWallet = testUtils.generateWallet().getBip47Wallet();
    BipWallet bip84Wallet = testUtils.generateWallet().getBip84Wallet(BIP_WALLET.DEPOSIT_BIP84);

    PaymentCode inputPCode = new PaymentCode(bip47InputWallet.getAccount(0).getPaymentCode());
    // sender signs message with payment code notification address privkey
    ECKey inputNotifAddressECKey =
        bip47InputWallet.getAccount(0).getNotificationAddress().getECKey();
    String inputPCodeMessage = inputPCode.toString() + ":" + new Date().toString();
    String inputPCodeSig = messageSignUtil.signMessage(inputNotifAddressECKey, inputPCodeMessage);
    // server validates sender's message with payment code notification address pubkey
    Assertions.assertTrue(
        messageSignUtil.verifySignedMessage(
            inputPCode.notificationAddress(params).getAddressString(),
            inputPCodeMessage,
            inputPCodeSig,
            params));

    PaymentCode outputPCode = new PaymentCode(bip47OutputWallet.getAccount(0).getPaymentCode());
    // receiver signs message with payment code notification address
    ECKey outputNotifAddressECKey =
        bip47OutputWallet.getAccount(0).getNotificationAddress().getECKey();
    String outputPCodeMessage = outputPCode.toString() + ":" + new Date().toString();
    String outputPCodeSig =
        messageSignUtil.signMessage(outputNotifAddressECKey, outputPCodeMessage);
    // server validates receiver's message with payment code notification address pubkey
    Assertions.assertTrue(
        messageSignUtil.verifySignedMessage(
            outputPCode.notificationAddress(params).getAddressString(),
            outputPCodeMessage,
            outputPCodeSig,
            params));

    ECKey utxoKey = inputWallet.getAccount(0).getReceive().getAddressAt(0).getECKey();
    SegwitAddress inputP2SH_P2WPKH =
        new SegwitAddress(utxoKey, cryptoService.getNetworkParameters());

    Mix mix = __getCurrentMix();

    // mock TransactionOutPoint
    long inputBalance =
        mix.getPool().computePremixBalanceMin(false) + mix.getPool().getMinerFeeMix();
    TxOutPoint utxo = createAndMockTxOutPoint(inputP2SH_P2WPKH, inputBalance);

    AssertMultiClientManager multiClientManager = multiClientManager(1, mix);
    multiClientManager.connectWithMock(
        inputP2SH_P2WPKH,
        bip84Wallet,
        null,
        (int) utxo.getIndex(), // TODO wrong n
        inputBalance);

    // register inputs...
    multiClientManager.assertMixStatusConfirmInput(1, false);
  }
}
