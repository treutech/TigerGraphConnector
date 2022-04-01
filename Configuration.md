
# Configuration

## Table of Contents

---
- [Configuration](#configuration)
	- [Table of Contents](#table-of-contents)
	- [General Options](#general-options)
	- [Schema Options](#schema-options)
	- [Source Options](#source-options)
	- [Sink Options](#sink-options)
	- [Examples](#examples)
			- [Sink](#sink)
			- [Source](#source)


<div style="page-break-after: always;"></div>

## General Options

| Config Option                      | Description                                      |              Optional | Datatype | Default |
| ---------------------------------- | ------------------------------------------------ | --------------------: | -------- | ------- |
| tigergraph.ip                      | The IP address pointing to a TigerGraph instance |                    No | String   |         |
| tigergraph.port                    | The port asscociated with a TigerGraph instance  |                    No | Integer  |         |
| tigergraph.graph                   | The name of your graph in TigerGraph             |                    No | String   |         |
| tigergraph.username                | Username to access TigerGraph                    |                    No | String   |         |
| tigergraph.password                | Password associated with your Username           |                    No | String   |         |
| tigergraph.ssl.enabled             | Should SSL be enabled or not                     |                   Yes | Boolean  | False   |
| tigergraph.ssl.trustStore.path     | Path to the trust store                          | Not if SSL is enabled | String   |         |
| tigergraph.ssl.trustStore.password | Password for the trust store                     | Not if SSL is enabled | String   |         |
| tigergraph.ssl.trustStore.type     | Trust store type                                 |                   Yes | String   | JKS     |
| tigergraph.ssl.keyStore.path       | Path to key the store                            | Not if SSL is enabled | String   |         |
| tigergraph.ssl.keyStore.password   | Password for the keystore                        | Not if SSL is enabled | String   |         |
| tigergraph.ssl.keyStore.type       | Key store type                                   |                   Yes | String   | JKS     |

For SSL setup with TigerGraph, please go to [TigerGraph's documentation](https://docs.tigergraph.com/admin/admin-guide/data-encryption/encrypting-connections) on the subject.

## Schema Options

The connector supports both AVRO and JSON message formats. Avro messages content is similar to JSON, but each message field has a schema attached to it. 
This configuration is setup in the connect.properties file. See examples below, first json configuration then avro configuration. Key usually has no structured schema.

```properties
value.converter=org.apache.kafka.connect.json.JsonConverter
value.converter.schemas.enable=false
key.converter.schemas.enable=false
key.converter=org.apache.kafka.connect.storage.StringConverter
```

```properties
key.converter=org.apache.kafka.connect.storage.StringConverter
key.converter.schemas.enable=false
value.converter=io.confluent.connect.avro.AvroConverter
value.converter.schema.registry.url=http://localhost:8081
value.converter.schemas.enable=true
```

## Source Options

| Config Option                             | Description                                                        |                    Optional | Default              | Datatype |
| ----------------------------------------- | ------------------------------------------------------------------ | --------------------------: | -------------------- | -------- |
| tigergraph.source.query                   | The query you want to run in Tigergraph                            |                          No |                      | String   |
| tigergraph.source.query.pattern           | The repeateable pattern inside the query                           |                          No |                      | String   |
| tigergraph.source.args                    | A comma separated list of arguments                                |                          No |                      | String   |
| tigergraph.source.timestamp.enabled       | If the query has a timestamp associated with it to use as a filter |                         Yes | False                | Boolean  |
| tigergraph.source.timestamp.attributeName | The timestamp's attribute name in Tigergraph                       | Not if timestamp is enabled | timestamp            | String   |
| tigergraph.source.timestamp.format        | The format of the timestamp                                        |                         Yes | yyyy-MMM-dd HH:mm:ss | String   |
| tigergraph.query.name.key                 | The topic partition key based on query name                        |                          No | query				  | String   |
| tigergraph.offset.name.key                | The topic offset key                                               |                          No | offset_timeset       | String   |
| tigergraph.type.name.key                  | The tiger graph type key                                           |                          No | type                 | String   |

## Sink Options

| Config Option                    | Description                                                                         | Optional | Default | Datatype |
| -------------------------------- | ----------------------------------------------------------------------------------- | -------: | ------- | -------- |
| tigergraph.sink.max.retries      | The maximum number of times to retry on errors before failing the task.             |      Yes | 3       | Integer  |
| tigergraph.sink.retry.backoff.ms | The time in milliseconds to wait following an error before a retry attempt is made. |      Yes | 1000    | Integer  |
| tigergraph.type.name.key         | The tiger graph type key                                                            |       No | type    | String   |

<div style="page-break-after: always;"></div>

## Examples

---
#### Sink
```properties
name=TigerGraphSink
connector.class=io.treutech.TigerGraphConnector.sink.TGSinkConnector
auto.offset.reset=earliest

topics=tigergraph
tasks.max=2

tigergraph.ip= 127.0.0.1
tigergraph.port=14240
tigergraph.username=tigergraph
tigergraph.password=tigergraph

tigergraph.sink.max.retries=5
tigergraph.sink.retry.backoff.ms=3000

tigergraph.graph=MyGraph

tigergraph.sink.max.retries=5
tigergraph.sink.retry.backoff.ms=3000
tigergraph.type.name.key=type
```

#### Source
**Note:**

Queries are prepended by `run` followed by the name of a preinstalled query in Tigergraph with each argument set using the scheme `<arg name>=?`.

The syntax looks like this: `run <query name>(<arg_1>=?, <arg_2>=?, ..., <arg_n>=?)`
Here's an example: `run MyQuery(r=?,t=?)`

```properties
name=TigerGraphSource
connector.class=io.treutech.TigerGraphConnector.source.TGSourceConnector
auto.offset.reset=earliest

topics=tigergraph
tasks.max=2

tigergraph.ip= 127.0.0.1
tigergraph.port=14240
tigergraph.username=tigergraph
tigergraph.password=tigergraph

tigergraph.graph=MyGraph

#depending on the number of arguments the pattern gets replicated inside the query
tigergraph.source.query.pattern=r=?
tigergraph.source.query=run MyQuery(pattern,t=?)
#one can have several arguments separated by comma (0,1,2,3)
tigergraph.source.args=0

tigergraph.source.timestamp.enabled=true
tigergraph.source.timestamp.attributeName=timestamp_
tigergraph.source.timestamp.format=yyyy-MM-dd HH:mm:ss
tigergraph.query.name.key=query
tigergraph.offset.name.key=offset_timeset
tigergraph.type.name.key=type
```
