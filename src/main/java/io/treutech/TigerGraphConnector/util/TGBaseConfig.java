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

package io.treutech.TigerGraphConnector.util;

import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

public class TGBaseConfig extends AbstractConfig {
  public final String topic;
  public final String tigergraph_ip;
  public final int tigergraph_port;
  public final String tigergraph_graph;
  public final String tigergraph_username;
  public final String tigergraph_password;
  public final boolean tigergraph_ssl_enabled;
  public final String ssl_truststore_path;
  public final String ssl_truststore_password;
  public final String ssl_truststore_type;
  public final String ssl_keystore_path;
  public final String ssl_keystore_password;
  public final String ssl_keystore_type;
  protected static final ConfigDef CONFIG_DEF;

  public TGBaseConfig(Map props) throws TGConfigException {
    this(CONFIG_DEF, props);
  }

  public TGBaseConfig(ConfigDef config, Map props) throws TGConfigException {
    super(config, props);
    this.topic = (String) props.get("topics");
    this.tigergraph_port = this.getInt("tigergraph.port");
    this.tigergraph_ip = this.getString("tigergraph.ip");
    this.tigergraph_graph = this.getString("tigergraph.graph");
    this.tigergraph_username = this.getString("tigergraph.username");
    this.tigergraph_password = this.getString("tigergraph.password");
    this.tigergraph_ssl_enabled = this.getBoolean("tigergraph.ssl.enabled");
    if (this.tigergraph_port == 0) {
      throw new TGConfigException("Port not set.");
    } else if ("".equals(this.tigergraph_ip)) {
      throw new TGConfigException("IP not set.");
    } else if ("".equals(this.tigergraph_graph)) {
      throw new TGConfigException("Graph not set.");
    } else if ("".equals(this.tigergraph_username)) {
      throw new TGConfigException("Username not set.");
    } else if ("".equals(this.tigergraph_password)) {
      throw new TGConfigException("Password not set.");
    } else {
      if (this.tigergraph_ssl_enabled) {
        this.ssl_truststore_path = this.getString("tigergraph.ssl.trustStore.path");
        this.ssl_truststore_password = this.getString("tigergraph.ssl.trustStore.password");
        this.ssl_truststore_type = this.getString("tigergraph.ssl.trustStore.type");
        this.ssl_keystore_path = this.getString("tigergraph.ssl.keyStore.path");
        this.ssl_keystore_password = this.getString("tigergraph.ssl.keyStore.password");
        this.ssl_keystore_type = this.getString("tigergraph.ssl.keyStore.type");
        if ("".equals(this.ssl_truststore_path)) {
          throw new TGConfigException("SSL trust store path not set.");
        }

        if ("".equals(this.ssl_truststore_password)) {
          throw new TGConfigException("SSL trust store password not set.");
        }

        if ("".equals(this.ssl_keystore_path)) {
          throw new TGConfigException("SSL key store path not set.");
        }

        if ("".equals(this.ssl_keystore_password)) {
          throw new TGConfigException("SSL key store password not set.");
        }
      } else {
        this.ssl_truststore_path = "";
        this.ssl_truststore_password = "";
        this.ssl_truststore_type = "";
        this.ssl_keystore_path = "";
        this.ssl_keystore_password = "";
        this.ssl_keystore_type = "";
      }

    }
  }

  static {
    CONFIG_DEF = (new ConfigDef())
        .define("tigergraph.ip", Type.STRING, "", Importance.HIGH,
            "The IP address pointing to a TigerGraph instance",
            "TigerGraph", 1, Width.MEDIUM, "TigerGraph's IP address")
        .define("tigergraph.port", Type.INT, 0, Importance.HIGH,
            "The port asscociated with a TigerGraph instance",
            "TigerGraph", 1, Width.MEDIUM, "TigerGraph's port")
        .define("tigergraph.username", Type.STRING, "", Importance.HIGH,
            "Username to access TigerGraph",
            "TigerGraph", 1, Width.MEDIUM, "TigerGraph Username")
        .define("tigergraph.password", Type.STRING, "", Importance.HIGH,
            "Password associated with your Username",
            "TigerGraph", 1, Width.MEDIUM, "TigerGraph Password")
        .define("tigergraph.graph", Type.STRING, "", Importance.HIGH,
            "The name of your graph in TigerGraph",
            "TigerGraph", 1, Width.MEDIUM, "A graph in TigerGraph")
        .define("tigergraph.ssl.enabled", Type.BOOLEAN, false, Importance.MEDIUM,
            "Should SSL be enabled or not",
            "TigerGraph", 1, Width.MEDIUM, "SSL Enabled")
        .define("tigergraph.ssl.trustStore.path", Type.STRING, "", Importance.MEDIUM,
            "Path to trust store",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.ssl.trustStore.password", Type.STRING, "", Importance.MEDIUM,
            "Password for trust store",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.ssl.trustStore.type", Type.STRING, "JKS", Importance.MEDIUM,
            "Trust store type",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.ssl.keyStore.path", Type.STRING, "", Importance.MEDIUM,
            "Path to key store",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.ssl.keyStore.password", Type.STRING, "", Importance.MEDIUM,
            "Password for keystore",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.ssl.keyStore.type", Type.STRING, "JKS", Importance.MEDIUM,
            "Key Store type",
            "TigerGraph", 1, Width.MEDIUM, "");
  }
}
