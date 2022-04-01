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

package io.treutech.TigerGraphConnector.source;

import io.treutech.TigerGraphConnector.util.TGConfigException;
import io.treutech.TigerGraphConnector.util.Version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TGSourceConnector extends SourceConnector {
  private static final Logger log = LoggerFactory.getLogger(TGSourceConnector.class);
  public TGSourceConfig sourceConfig;

  public String version() {
    return Version.getVersion();
  }

  public ConfigDef config() {
    return TGSourceConfig.getConfig();
  }

  public void start(final Map props) {
    log.info("Starting TigerGraph Source Connector");
    try {
      this.sourceConfig = new TGSourceConfig(props);
    } catch (TGConfigException var3) {
      log.error(var3.getMessage());
    }
  }

  public void stop() {
    log.info("Stopping TigerGraph Source Connector");
  }

  public Class<TGSourceTask> taskClass() {
    return TGSourceTask.class;
  }

  public List<Map<String, String>> taskConfigs(int maxTasks) {
    List<Map<String, String>> configs = new ArrayList<>();
    for (int i = 0; i < maxTasks; ++i) {
      Map<String, String> conf = new HashMap<>();
      if (!this.sourceConfig.tigergraph_ip.isEmpty()) {
        conf.put("tigergraph.ip", this.sourceConfig.tigergraph_ip);
      }
      if (this.sourceConfig.tigergraph_port != 0) {
        conf.put("tigergraph.port", String.valueOf(this.sourceConfig.tigergraph_port));
      }
      if (!this.sourceConfig.tigergraph_graph.isEmpty()) {
        conf.put("tigergraph.graph", this.sourceConfig.tigergraph_graph);
      }
      if (!this.sourceConfig.tigergraph_username.isEmpty()) {
        conf.put("tigergraph.username", this.sourceConfig.tigergraph_username);
      }
      if (!this.sourceConfig.tigergraph_password.isEmpty()) {
        conf.put("tigergraph.password", this.sourceConfig.tigergraph_password);
      }
      if (!this.sourceConfig.tigergraph_query_pattern.isEmpty()) {
        conf.put("tigergraph.source.query.pattern", this.sourceConfig.tigergraph_query_pattern);
      }
      if (!this.sourceConfig.tigergraph_query.isEmpty()) {
        conf.put("tigergraph.source.query", this.sourceConfig.tigergraph_query);
      }
      conf.put("tigergraph.ssl.enabled", String.valueOf(this.sourceConfig.tigergraph_ssl_enabled));
      if (this.sourceConfig.tigergraph_ssl_enabled) {
        conf.put("tigergraph.ssl.enabled", String.valueOf(this.sourceConfig.tigergraph_ssl_enabled));
        if (!this.sourceConfig.ssl_truststore_path.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.path", this.sourceConfig.ssl_truststore_path);
        }
        if (!this.sourceConfig.ssl_truststore_password.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.password", this.sourceConfig.ssl_truststore_password);
        }
        if (!this.sourceConfig.ssl_truststore_type.isEmpty()) {
          conf.put("tigergraph.ssl.trustStore.type", this.sourceConfig.ssl_truststore_type);
        }
        if (!this.sourceConfig.ssl_keystore_path.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.path", this.sourceConfig.ssl_keystore_path);
        }
        if (!this.sourceConfig.ssl_keystore_password.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.password", this.sourceConfig.ssl_keystore_password);
        }
        if (!this.sourceConfig.ssl_keystore_type.isEmpty()) {
          conf.put("tigergraph.ssl.keyStore.type", this.sourceConfig.ssl_keystore_type);
        }
      }
      if (this.sourceConfig.tigergraph_query_args != null) {
        conf.put("tigergraph.source.args", this.sourceConfig.tigergraph_query_args_raw);
      } else {
        conf.put("tigergraph.source.args", "");
      }
      conf.put("tigergraph.source.timestamp.enabled", String.valueOf(this.sourceConfig.timestampEnabled));
      if (this.sourceConfig.timestampEnabled) {
        if (!this.sourceConfig.timestampAttrName.isEmpty()) {
          conf.put("tigergraph.source.timestamp.attributeName", this.sourceConfig.timestampAttrName);
        }
        if (!this.sourceConfig.timestampFormat.isEmpty()) {
          conf.put("tigergraph.source.timestamp.format", this.sourceConfig.timestampFormat);
        }
      }
      configs.add(conf);
    }
    return configs;
  }
}
