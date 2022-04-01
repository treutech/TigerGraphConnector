//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.treutech.TigerGraphConnector.sink;

import io.treutech.TigerGraphConnector.util.TGConfigException;
import io.treutech.TigerGraphConnector.util.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TGSinkConnector extends SinkConnector {
  private static final Logger log = LoggerFactory.getLogger(TGSinkConnector.class);
  public TGSinkConfig sinkConfig;

  public String version() {
    return Version.getVersion();
  }

  public ConfigDef config() {
    return TGSinkConfig.getConfig();
  }

  public void start(Map props) {
    log.info("Starting TigerGraph Sink Connector");
    try {
      this.sinkConfig = new TGSinkConfig(props);
    } catch (TGConfigException var3) {
      log.error(var3.getMessage());
    }
  }

  public void stop() {
    log.info("Stopping TigerGraph Sink Connector");
  }

  public Class<TGSinkTask> taskClass() {
    return TGSinkTask.class;
  }

  public List<Map<String, String>> taskConfigs(final int maxTasks) {
    final List<Map<String, String>> configs = new ArrayList<>();

    for (int i = 0; i < maxTasks; i++) {
      Map<String, String> conf = new HashMap<>();
      if (!this.sinkConfig.tigergraph_ip.isEmpty()) {
        conf.put("tigergraph.ip", this.sinkConfig.tigergraph_ip);
      }
      if (this.sinkConfig.tigergraph_port != 0) {
        conf.put("tigergraph.port", String.valueOf(this.sinkConfig.tigergraph_port));
      }
      if (!this.sinkConfig.tigergraph_graph.isEmpty()) {
        conf.put("tigergraph.graph", this.sinkConfig.tigergraph_graph);
      }
      if (!this.sinkConfig.tigergraph_username.isEmpty()) {
        conf.put("tigergraph.username", this.sinkConfig.tigergraph_username);
      }
      if (!this.sinkConfig.tigergraph_password.isEmpty()) {
        conf.put("tigergraph.password", this.sinkConfig.tigergraph_password);
      }
      conf.put("tigergraph.ssl.enabled", String.valueOf(this.sinkConfig.tigergraph_ssl_enabled));
      if (this.sinkConfig.tigergraph_ssl_enabled) {
        if (!this.sinkConfig.ssl_truststore_path.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.path", this.sinkConfig.ssl_truststore_path);
        }
        if (!this.sinkConfig.ssl_truststore_password.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.password", this.sinkConfig.ssl_truststore_password);
        }
        if (!this.sinkConfig.ssl_truststore_type.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.type", this.sinkConfig.ssl_truststore_type);
        }
        if (!this.sinkConfig.ssl_keystore_path.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.path", this.sinkConfig.ssl_keystore_path);
        }
        if (!this.sinkConfig.ssl_keystore_password.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.password", this.sinkConfig.ssl_keystore_password);
        }
        if (!this.sinkConfig.ssl_keystore_type.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.type", this.sinkConfig.ssl_keystore_type);
        }
      }
      if (this.sinkConfig.maxRetries != 0) {
        conf.put("tigergraph.sink.max.retries", String.valueOf(this.sinkConfig.maxRetries));
      }
      if (this.sinkConfig.retryBackoffMs != 0) {
        conf.put("tigergraph.sink.retry.backoff.ms", String.valueOf(this.sinkConfig.retryBackoffMs));
      }
      configs.add(conf);
    }
    return configs;
  }
}
