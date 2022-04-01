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

import io.treutech.TigerGraphConnector.util.TGBaseConfig;
import io.treutech.TigerGraphConnector.util.TGConfigException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Range;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

public class TGSinkConfig extends TGBaseConfig {
  public String tigergraph_type_name_key = this.getString("tigergraph.type.name.key");
  private static final ConfigDef.Range NON_NEGATIVE_INT_VALIDATOR = Range.atLeast(0);
  public final int maxRetries = this.getInt("tigergraph.sink.max.retries");
  public final int retryBackoffMs = this.getInt("tigergraph.sink.retry.backoff.ms");
  public final TGSinkConfig.PrimaryKeyMode pkMode = TGSinkConfig.PrimaryKeyMode.valueOf(this.getString("pk.mode").toUpperCase());
  public final List<String> pkFields = this.getList("pk.fields");

  public TGSinkConfig(final Map props) throws TGConfigException {
    super(getConfig(), props);
  }

  public static ConfigDef getConfig() {
    return (new ConfigDef(CONFIG_DEF))
        .define("tigergraph.sink.max.retries", Type.INT, 3,
            NON_NEGATIVE_INT_VALIDATOR, Importance.MEDIUM,
            "The maximum number of times to retry on errors before failing the task.",
            "TigerGraph", 1, Width.SHORT, "Maximum Retries")
        .define("tigergraph.sink.retry.backoff.ms", Type.INT, 1000,
            NON_NEGATIVE_INT_VALIDATOR, Importance.MEDIUM,
            "The time in milliseconds to wait following an error before a retry attempt is made.",
            "TigerGraph", 1, Width.SHORT, "Retry Backoff (millis)")
        .define("pk.mode", Type.STRING, "none",
            EnumValidator.in(PrimaryKeyMode.values()), Importance.HIGH,
            "The primary key mode, also refer to ``pk.fields`` documentation for interplay. Supported modes are:\n``none``\n    No keys utilized.\n``record_key``\n    Field(s) from the record key are used, which may be a primitive or a struct.\n``record_value``\n    Field(s) from the record value are used, which must be a struct.",
            "TigerGraph", 2, Width.MEDIUM, "Primary Key Mode")
        .define("pk.fields", Type.LIST, "", Importance.MEDIUM,
            "List of comma-separated primary key field names. The runtime interpretation of this config depends on the ``pk.mode``:\n``none``\n    Ignored as no fields are used as primary key in this mode.\n``record_key``\n    If empty, all fields from the key struct will be used, otherwise used to extract the desired fields - for primitive key only a single field name must be configured.\n``record_value``\n    If empty, all fields from the value struct will be used, otherwise used to extract the desired fields.",
            "TigerGraph", 3, Width.LONG, "Primary Key Fields")
        .define("tigergraph.type.name.key", Type.STRING, "type", Importance.HIGH,
            "The TG type key",
            "TigerGraph", 1, Width.MEDIUM, "The TG type key");
  }

  private static class EnumValidator implements ConfigDef.Validator {
    private final List<String> canonicalValues;
    private final Set<String> validValues;

    private EnumValidator(final List<String> canonicalValues, final Set<String> validValues) {
      this.canonicalValues = canonicalValues;
      this.validValues = validValues;
    }

    public static TGSinkConfig.EnumValidator in(final Object[] enumerators) {
      List<String> canonicalValues = new ArrayList<>(enumerators.length);
      Set<String> validValues = new HashSet<>(enumerators.length * 2);
      for (Object e : enumerators) {
        canonicalValues.add(e.toString().toLowerCase());
        validValues.add(e.toString().toUpperCase());
        validValues.add(e.toString().toLowerCase());
      }
      return new TGSinkConfig.EnumValidator(canonicalValues, validValues);
    }

    @Override
    public void ensureValid(final String key, final Object value) {
      if (!this.validValues.contains((String)value)) {
        throw new ConfigException(key, value, "Invalid enumerator");
      }
    }

    public String toString() {
      return this.canonicalValues.toString();
    }

  }

  public enum PrimaryKeyMode {
    NONE,
    RECORD_KEY,
    RECORD_VALUE
  }
}
