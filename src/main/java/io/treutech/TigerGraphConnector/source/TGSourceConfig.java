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

import io.treutech.TigerGraphConnector.util.TGBaseConfig;
import io.treutech.TigerGraphConnector.util.TGConfigException;

import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

public class TGSourceConfig extends TGBaseConfig {
  public String tigergraph_query_pattern = this.getString("tigergraph.source.query.pattern");
  public String tigergraph_query = this.getString("tigergraph.source.query");
  public String tigergraph_query_name_key = this.getString("tigergraph.query.name.key");
  public String tigergraph_offset_name_key = this.getString("tigergraph.offset.name.key");
  public String tigergraph_type_name_key = this.getString("tigergraph.type.name.key");
  public String tigergraph_query_args_raw = this.getString("tigergraph.source.args");
  public String[] tigergraph_query_args;
  public boolean timestampEnabled;
  public String timestampAttrName;
  public String timestampFormat;

  public TGSourceConfig(Map props) throws TGConfigException {
    super(getConfig(), props);
    this.tigergraph_query_args = this.tigergraph_query_args_raw.split(",");
    if (this.tigergraph_query.equals("")) {
      throw new TGConfigException("Query not set.");
    } else if (this.tigergraph_query_pattern.equals("")) {
      throw new TGConfigException("Query pattern not set.");
    } else {
      this.timestampEnabled = this.getBoolean("tigergraph.source.timestamp.enabled");
      if (this.timestampEnabled) {
        this.timestampAttrName = this.getString("tigergraph.source.timestamp.attributeName");
        this.timestampFormat = this.getString("tigergraph.source.timestamp.format");
        if (this.timestampAttrName.equals("timestamp")) {
          throw new TGConfigException("Timestamp attribute name not set.");
        }
      }

    }
  }

  public static ConfigDef getConfig() {
    final ConfigDef conf = new ConfigDef(CONFIG_DEF);
    conf.define("tigergraph.source.query.pattern", Type.STRING, "", Importance.HIGH,
            "The pattern to be repeated into the query", "TigerGraph",
            1, Width.MEDIUM, "The pattern to be repeated into the query")
        .define("tigergraph.source.query", Type.STRING, "", Importance.HIGH,
            "The query to be run in TigerGraph", "TigerGraph",
            1, Width.MEDIUM, "The query to be run in TigerGraph")
        .define("tigergraph.source.args", Type.STRING, "", Importance.LOW,
            "The query arguments (comma separated list)", "TigerGraph",
            1, Width.MEDIUM, "The query arguments (comma separated list)")
        .define("tigergraph.source.timestamp.enabled", Type.BOOLEAN, false, Importance.MEDIUM,
            "", "TigerGraph",
            1, Width.MEDIUM, "")
        .define("tigergraph.source.timestamp.attributeName", Type.STRING, "timestamp",
            Importance.MEDIUM, "", "TigerGraph",
            1, Width.MEDIUM, "")
        .define("tigergraph.source.timestamp.format", Type.STRING,
            "yyyy-MMM-dd HH:mm:ss", Importance.MEDIUM, "",
            "TigerGraph", 1, Width.MEDIUM, "")
        .define("tigergraph.query.name.key", Type.STRING, "query", Importance.HIGH,
            "The topic partition key based on query name",
            "TigerGraph", 1, Width.MEDIUM, "The topic partition key based on query name")
        .define("tigergraph.offset.name.key", Type.STRING, "", Importance.HIGH,
            "The topic offset key",
            "TigerGraph", 1, Width.MEDIUM, "The topic offset key")
        .define("tigergraph.type.name.key", Type.STRING, "type", Importance.HIGH,
            "The TG type key",
            "TigerGraph", 1, Width.MEDIUM, "The TG type key");
    return conf;
  }
}
