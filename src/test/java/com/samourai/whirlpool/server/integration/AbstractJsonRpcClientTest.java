package com.samourai.whirlpool.server.integration;

import com.samourai.javaserver.utils.ServerUtils;
import com.samourai.whirlpool.server.services.rpc.RpcClientService;
import com.samourai.whirlpool.server.utils.TestUtils;
import com.samourai.whirlpool.server.utils.Utils;
import java.lang.invoke.MethodHandles;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/** Tests connecting to a real rpc client node. */
@ActiveProfiles(ServerUtils.PROFILE_DEFAULT)
public abstract class AbstractJsonRpcClientTest {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired protected RpcClientService rpcClientService;

  @Autowired protected TestUtils testUtils;

  @BeforeEach
  public void setUp() throws Exception {
    // enable debug
    Utils.setLoggerDebug();

    // connect to rpc node
    Utils.testJsonRpcClientConnectivity(rpcClientService);
  }
}
