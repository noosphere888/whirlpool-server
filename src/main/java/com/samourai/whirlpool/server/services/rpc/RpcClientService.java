package com.samourai.whirlpool.server.services.rpc;

import com.samourai.whirlpool.server.exceptions.BroadcastException;
import java.util.Optional;
import org.bitcoinj.core.Transaction;

public interface RpcClientService {
  boolean testConnectivity();

  Optional<RpcRawTransactionResponse> getRawTransaction(String txid);

  boolean isTxOutUnspent(String txid, long index);

  void broadcastTransaction(Transaction tx) throws BroadcastException;

  int getBlockHeight() throws Exception;
}
