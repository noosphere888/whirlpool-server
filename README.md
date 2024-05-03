# WARNING: APPARTENLY THESE TEXT FILES ARE TOO DANGEROUS THAT THE U.S. FEDERAL GOVERNMENT HAD TO SEIZE THESE TEXT FILES
# WARNING: MAY NOT BE UP TO DATE

# whirlpool-server
https://github.com/noosphere888/Whirlpool) by Samourai-Wallet.

## Installation
See [README-install.md](README-install.md)

## Configuration
### RPC client
```
server.rpc-client.protocol = http
server.rpc-client.host = CONFIGURE-ME
server.rpc-client.port = CONFIGURE-ME
server.rpc-client.user = CONFIGURE-ME
server.rpc-client.password = CONFIGURE-ME

server.rpc-client.block-height-max-spread = 10
```
The bitcoin node should be running on the same network (main or test).<br/>
The node will be used to verify UTXO and broadcast tx.<br/>
<br/>
Server will reject clients announcing more than *block-height-max-spread* block-height difference.

### Pool: UTXO amounts
```
server.pools[x].denomination: amount in satoshis
server.pools[x].tx0MaxOutputs: max outputs per tx0
server.miner-fees.miner-fee-min: minimum miner-fee accepted for mustMix
server.miner-fees.miner-fee-max: maximum miner-fee accepted for mustMix
server.miner-fees.miner-fee-cap: "soft cap" miner-fee recommended for a new mustMix (should be <= miner-fee-max)
server.miner-fees.min-relay-fee: minimum miner-fee to accumulate for mixing
```

Optional pool override:
```
server.pools[x].miner-fees.miner-fee-min
server.pools[x].miner-fees.miner-fee-max
server.pools[x].miner-fees.miner-fee-cap
server.pools[x].miner-fees.min-relay-fee
```

UTXO should be founded with:<br/>
for mustMix: (*server.mix.denomination* + *server.miner-fees.miner-fee-min*) to (*server.mix.denomination* + *server.miner-fees.miner-fee-max*). New TX0 outputs are capped to (*server.mix.denomination* + *server.miner-fees.miner-fee-cap*).<br/>
for liquidities: (*server.mix.denomination*) to (*server.mix.denomination* + *server.mix.miner-fee-max*)


### Pool: TX0 fees
```
server.pools[x].fee-value: server fee (in satoshis) for each tx0
server.pools[x].fee-accept: alternate fee values accepted (key=fee in sats, value=maxTx0Time)
```
Standard fee configuration is through *fee-value*.
*fee-accept* is useful when changing *fee-value*, to still accept unspent tx0s <= maxTx0Time with previous fee-value.


### UTXO confirmations
```
server.register-input.min-confirmations-must-mix: minimum confirmations for mustMix inputs
server.register-input.min-confirmations-liquidity: minimum confirmations for liquidity inputs
server.register-input.confirm-interval = 10: inputs are confirmed by batch at this frequency (seconds)
```

### UTXO rules
```
server.register-input.max-inputs-same-hash: max inputs with same hash (same origin tx) allowed to register to a mix
server.register-input.max-inputs-same-user-hash: max inputs with same user-hash (same mixing client) allowed to register to a mix
```

### SCodes
```
server.samourai-fees.scodes[foo].payload = 12345
server.samourai-fees.scodes[foo].fee-value-percent = 50
server.samourai-fees.scodes[foo].expiration = 1569484078 # optional

server.samourai-fees.scodes[bar].payload = 23456
server.samourai-fees.scodes[bar].fee-value-percent = 0
```
SCodes are special codes usable to enable special rules for tx0.
Each SCode is mapped to a short value payload (-32,768 to 32,767) which will be embedded into tx0's OP_RETURN as WhirlpoolFeeData.feePayload.
Payload '0' is forbidden, which is mapped to WhirlpoolFeeData.feePayload=NULL.
SCode overrides standard fee-value with a percent of this value.
SCode can expire for tx0s confirmed after a specified time.

### Pool: Mix limits
```
server.pools[x].anonymity-set = 5
server.pools[x].must-mix-min = 1
server.pools[x].liquidity-min = 1
```
Mix will start when *anonymity-set* (mustMix + liquidities) are registered.<br/>

At the beginning of the mix, only mustMix can register up, to *anonymity-set - liquidity-min*. Meanwhile, liquidities are placed on a waiting pool.<br/>
Liquidities are added as soon as *must-mix-min* are reached, up to *anonymity-set* inputs for the mix.<br/>
MustMixs are selected such as accumulated miner-fees >= *min-relay-fee* and >= *must-mix-min x miner-fee-cap* 

### Exports
Mixs are exported to a CSV file:
```
server.export.mixs.directory
server.export.mixs.filename
```

Activity is exported to a CSV file:
```
server.export.activity.directory
server.export.activity.filename
```

### Testing
```
server.rpc-client.mock-tx-broadcast = false
server.test-mode = false
server.fail-mode = DISABLED
```
For testing purpose, *server.rpc-client.mock-tx-broadcast* can be enabled to mock txs instead of broadcasting it.  
When enabled, *server.test-mode* allows client to bypass tx0 checks.  
When enabled, *server.fail-mode* triggers mixing failures.

java -jar -Dspring.profiles.active=test target/whirlpool-server-develop-SNAPSHOT.jar --debug --spring.config.location=classpath:application.properties,/path/to/application-default.properties

## Resources
 * [whirlpool](https://github.com/noosphere888/Whirlpool)
 * [whirlpool-protocol](https://github.com/noosphere888/whirlpool-protocol)
 * [whirlpool-client](https://github.com/noosphere888/whirlpool-client)
