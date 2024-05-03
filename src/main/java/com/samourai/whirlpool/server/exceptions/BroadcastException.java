package com.samourai.whirlpool.server.exceptions;

import wf.bitcoin.javabitcoindrpcclient.BitcoinRPCException;

public class BroadcastException extends MixException {
  private String failInfo;

  public BroadcastException(String failInfo) {
    super("Unable to broadcast tx: " + failInfo);
    this.failInfo = failInfo;
  }

  public static BroadcastException computeBroadcastException(Exception e) {
    String failInfo = null;
    if (BitcoinRPCException.class.isAssignableFrom(e.getClass())) {
      failInfo = ((BitcoinRPCException) e).getResponse();
    }
    return new BroadcastException(failInfo);
  }

  public String getFailInfo() {
    return failInfo;
  }
}
