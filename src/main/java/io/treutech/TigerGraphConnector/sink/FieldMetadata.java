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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Schema.Type;
import org.apache.kafka.connect.errors.ConnectException;

public class FieldMetadata {
  public final Set<String> keyFieldNames;
  public final Set<String> nonKeyFieldNames;

  private FieldMetadata(final Set<String> keyFieldNames, final Set<String> nonKeyFieldNames) {
    this.keyFieldNames = keyFieldNames;
    this.nonKeyFieldNames = nonKeyFieldNames;
  }

  public static FieldMetadata extract(final String tableName,
                                      final TGSinkConfig.PrimaryKeyMode pkMode,
                                      final List<String> configuredPkFields,
                                      final Pair<Schema, Schema> schemaPair,
                                      final String typeKeyName) {
    final Schema valueSchema = schemaPair.getRight();
    if (valueSchema != null && valueSchema.type() != Type.STRUCT) {
      throw new ConnectException("Value schema must be of type Struct");
    } else {
      final Set<String> keyFieldNames = new LinkedHashSet<>();
      switch (pkMode) {
        case NONE:
          break;
        case RECORD_KEY:
          extractRecordKeyPk(tableName, configuredPkFields, schemaPair.getLeft(), keyFieldNames);
          break;
        case RECORD_VALUE:
          extractRecordValuePk(tableName, configuredPkFields, valueSchema, keyFieldNames, typeKeyName);
          break;
        default:
          throw new ConnectException("Unknown primary key mode: " + pkMode);
      }
      final Set<String> nonKeyFieldNames = new LinkedHashSet<>();
      if (valueSchema != null) {
        for (final Field field : valueSchema.fields()) {
          if (!keyFieldNames.contains(field.name()) && !field.name().contains("_" + typeKeyName)) {
            nonKeyFieldNames.add(field.name());
          }
        }
      }

      return new FieldMetadata(keyFieldNames, nonKeyFieldNames);
    }
  }

  private static void extractRecordKeyPk(final String tableName,
                                         final List<String> configuredPkFields,
                                         final Schema keySchema,
                                         final Set<String> keyFieldNames) {
    if (keySchema == null) {
      throw new ConnectException(
          String.format("PK mode for table '%s' is %s, but record key schema is missing",
              tableName, TGSinkConfig.PrimaryKeyMode.RECORD_KEY));
    } else {
      final Schema.Type keySchemaType = keySchema.type();
      if (keySchemaType.isPrimitive()) {
        if (configuredPkFields.size() != 1) {
          throw new ConnectException(
              String.format("Need exactly one PK column defined since the key schema for records is a primitive type, defined columns are: %s",
                  configuredPkFields));
        }
        keyFieldNames.add(configuredPkFields.get(0));
      } else {
        if (keySchemaType != Type.STRUCT) {
          throw new ConnectException("Key schema must be primitive type or Struct, but is of type: " + keySchemaType);
        }
        final List<Field> fields = keySchema.fields();
        final List<String> fieldNames = new ArrayList<>();
        for (final Field field : fields) {
          fieldNames.add(field.name());
        }
        if (configuredPkFields.isEmpty()) {
          keyFieldNames.addAll(fieldNames);
        } else {
          for (final String fieldName : configuredPkFields) {
            if (!fieldNames.contains(fieldName)) {
              throw new ConnectException(
                  String.format("PK mode for table '%s' is %s with configured PK fields %s, but record key schema does not contain field: %s",
                      tableName, TGSinkConfig.PrimaryKeyMode.RECORD_KEY, configuredPkFields, fieldName));
            }
          }
          keyFieldNames.addAll(configuredPkFields);
        }
      }

    }
  }

  private static void extractRecordValuePk(final String tableName,
                                           final List<String> configuredPkFields,
                                           final Schema valueSchema,
                                           final Set<String> nonKeyFieldNames,
                                           final String typeKeyName) {
    if (valueSchema == null) {
      throw new ConnectException(String.format("PK mode for table '%s' is %s, but record value schema is missing",
          tableName, TGSinkConfig.PrimaryKeyMode.RECORD_VALUE));
    } else {
      if (configuredPkFields.isEmpty()) {
        for (final Field nonKeyField : valueSchema.fields()) {
          if (!nonKeyField.name().contains("_" + typeKeyName)) {
            nonKeyFieldNames.add(nonKeyField.name());
          }
        }
      } else {
        for (final String fieldName : configuredPkFields) {
          if (valueSchema.field(fieldName) == null) {
            throw new ConnectException(
                String.format("PK mode for table '%s' is %s with configured PK fields %s, but record value schema does not contain field: %s",
                    tableName, TGSinkConfig.PrimaryKeyMode.RECORD_VALUE, configuredPkFields, fieldName));
          }
        }
        nonKeyFieldNames.addAll(configuredPkFields);
      }
    }
  }
}
