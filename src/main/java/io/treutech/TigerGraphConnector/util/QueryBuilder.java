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

import io.treutech.TigerGraphConnector.sink.FieldMetadata;
import io.treutech.TigerGraphConnector.sink.TGSinkConfig;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;

public class QueryBuilder {
  public static Query generateQuery(final SinkRecord record, final TGSinkConfig config) {
    final Pair<Schema, Schema> schemaPair = Pair.of(record.keySchema(), record.valueSchema());
    final Pair<TableId, String> tableDetails = TGResourceUtils.getTableDetails(record, config.tigergraph_type_name_key);
    final FieldMetadata fieldmetadata = FieldMetadata.extract((tableDetails.getLeft()).getTableName(),
        config.pkMode, config.pkFields, schemaPair, config.tigergraph_type_name_key);
    final String queryBody = buildInsertStatement(tableDetails,
        fieldmetadata.keyFieldNames, fieldmetadata.nonKeyFieldNames, record, config.tigergraph_type_name_key);
    return new Query(queryBody, config, schemaPair, fieldmetadata, record);
  }

  public static String buildInsertStatement(final Pair<TableId, String> tableDetails,
                                            final Collection<String> keyColumns,
                                            final Collection<String> nonKeyColumns,
                                            final SinkRecord record,
                                            final String typeKeyName) {
    Map<String, String> recordValues = null;
    final StringJoiner namePart = new StringJoiner(",", "(", ")");
    final String tableType = tableDetails.getRight();
    if (tableType.startsWith("v")) {
      if (!keyColumns.isEmpty() && !keyColumns.contains("id")) {
        namePart.add("id");
      }
      for (final String keyColumn : keyColumns) {
        namePart.add(keyColumn);
      }
      if (!nonKeyColumns.isEmpty()) {
        for (final String nonKeyColumn : nonKeyColumns) {
          if (!nonKeyColumn.startsWith("v_")) {
            namePart.add(nonKeyColumn);
          } else {
            namePart.add("id");
          }
        }
      } else if (record.valueSchema() == null) {
        final Map<String, String> values = new HashMap<>((Map<String, String>)record.value());
        String tableKey = null;

        for (String s : values.keySet()) {
          if (s.endsWith("_" + typeKeyName)) {
            tableKey = s;
          }
        }
        if (tableKey != null) {
          values.remove(tableKey);
        }
        namePart.add("id");
        for (final String s : values.keySet()) {
          if (!s.startsWith("v_")) {
            namePart.add(s);
          }
        }
        recordValues = values;
      }
    } else {
      for (final String keyColumn : keyColumns) {
        namePart.add(keyColumn);
      }
      if (!nonKeyColumns.isEmpty()) {
        for (final String nonKeyColumn : nonKeyColumns) {
          namePart.add(nonKeyColumn);
        }
      } else if (record.valueSchema() == null) {
        HashMap<String, String> values = new HashMap<>((Map<String, String>) record.value());
        String tableKey = null;
        for (final String s : values.keySet()) {
          if (s.endsWith("_" + typeKeyName)) {
            tableKey = s;
          }
        }
        if (tableKey != null) {
          values.remove(tableKey);
        }
        for (final String s : values.keySet()) {
          namePart.add(s);
        }
        recordValues = values;
      }
    }

    final StringJoiner valuePart = new StringJoiner(",", "(", ")");

    if (keyColumns.isEmpty() && nonKeyColumns.isEmpty()) {
      if (recordValues != null) {
        for (int i = 0; i < recordValues.size(); i++) {
          valuePart.add("?");
        }
      }
    } else {
      for (int i = 0; i < keyColumns.size() + nonKeyColumns.size(); i++) {
        valuePart.add("?");
      }
    }

    return "INSERT INTO " + tableType + " " + (tableDetails.getLeft()).getTableName() + " " + namePart + " " +
        "VALUES " + valuePart;
  }
}
