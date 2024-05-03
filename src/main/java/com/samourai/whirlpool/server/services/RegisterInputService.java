package com.samourai.whirlpool.server.services;

import com.samourai.javaserver.exceptions.NotifiableException;
import com.samourai.whirlpool.server.beans.Pool;
import com.samourai.whirlpool.server.beans.RegisteredInput;
import com.samourai.whirlpool.server.beans.rpc.RpcTransaction;
import com.samourai.whirlpool.server.beans.rpc.TxOutPoint;
import com.samourai.whirlpool.server.exceptions.BannedInputException;
import com.samourai.whirlpool.server.exceptions.IllegalInputException;
import com.samourai.whirlpool.server.exceptions.ServerErrorCode;
import com.samourai.whirlpool.server.persistence.to.BanTO;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RegisterInputService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  public static final String HEALTH_CHECK_UTXO = "HEALTH_CHECK";
  public static final String HEALTH_CHECK_SUCCESS = "HEALTH_CHECK_SUCCESS";
  public static final String ERROR_ALREADY_SPENT = "Waiting for first mix confirmation";

  private PoolService poolService;
  private CryptoService cryptoService;
  private BlockchainDataService blockchainDataService;
  private InputValidationService inputValidationService;
  private BanService banService;
  private DbService dbService;
  private ExportService exportService;

  @Autowired
  public RegisterInputService(
      PoolService poolService,
      CryptoService cryptoService,
      BlockchainDataService blockchainDataService,
      InputValidationService inputValidationService,
      BanService banService,
      DbService dbService,
      ExportService exportService) {
    this.poolService = poolService;
    this.cryptoService = cryptoService;
    this.blockchainDataService = blockchainDataService;
    this.inputValidationService = inputValidationService;
    this.banService = banService;
    this.dbService = dbService;
    this.exportService = exportService;
  }

  public RegisteredInput registerInput(
      String poolId,
      String username,
      String signature,
      String utxoHash,
      long utxoIndex,
      boolean liquidity,
      String ip)
      throws NotifiableException {
    if (HEALTH_CHECK_UTXO.equals(utxoHash)) {
      throw new IllegalInputException(ServerErrorCode.INPUT_REJECTED, HEALTH_CHECK_SUCCESS);
    }
    if (!cryptoService.isValidTxHash(utxoHash)) {
      throw new IllegalInputException(ServerErrorCode.INPUT_REJECTED, "Invalid utxoHash");
    }
    if (utxoIndex < 0) {
      throw new IllegalInputException(ServerErrorCode.INPUT_REJECTED, "Invalid utxoIndex");
    }

    // verify UTXO not banned
    Optional<BanTO> banTO = banService.findActiveBan(utxoHash, utxoIndex);
    if (banTO.isPresent()) {
      log.warn("Rejecting banned UTXO: [" + banTO.get() + "], ip=" + ip);
      String banMessage = banTO.get().computeBanMessage();
      throw new BannedInputException(banMessage);
    }

    try {
      // fetch outPoint
      IllegalInputException notFoundException =
          new IllegalInputException(
              ServerErrorCode.INPUT_REJECTED, "UTXO not found: " + utxoHash + "-" + utxoIndex);
      RpcTransaction rpcTransaction =
          blockchainDataService.getRpcTransaction(utxoHash).orElseThrow(() -> notFoundException);
      TxOutPoint txOutPoint =
          blockchainDataService
              .getOutPoint(rpcTransaction, utxoIndex)
              .orElseThrow(() -> notFoundException);

      // verify signature
      inputValidationService.validateSignature(txOutPoint, poolId, signature);

      // verify unspent
      if (!blockchainDataService.isTxOutUnspent(utxoHash, utxoIndex)) {
        // spent input being resubmitted by client = spending tx is missing in mempool backing CLI
        // we assume it's a mix tx which was removed from CLI mempool due to mempool congestion
        throw new IllegalInputException(
            ServerErrorCode.INPUT_REJECTED, RegisterInputService.ERROR_ALREADY_SPENT);
      }

      // check tx0Whitelist
      String txid = rpcTransaction.getTx().getHashAsString();
      if (!dbService.hasTx0Whitelist(txid)) {
        // verify input is a valid mustMix or liquidity
        Pool pool = poolService.getPool(poolId);
        boolean hasMixTxid = dbService.hasMixTxid(txid, txOutPoint.getValue());
        inputValidationService.validateProvenance(rpcTransaction, liquidity, pool, hasMixTxid);
      } else {
        log.warn("tx0 check disabled by whitelist for txid=" + txid);
      }

      // register input to pool
      RegisteredInput registeredInput =
          poolService.registerInput(poolId, username, liquidity, txOutPoint, ip, null);
      return registeredInput;
    } catch (NotifiableException e) { // validation error or input rejected
      log.warn("Input rejected (" + utxoHash + ":" + utxoIndex + "): " + e.getMessage());
      throw e;
    }
  }
}
