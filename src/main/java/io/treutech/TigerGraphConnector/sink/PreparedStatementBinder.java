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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PreparedStatementBinder implements StatementBinder {
  private final TGSinkConfig.PrimaryKeyMode pkMode;
  private final String typeNameKey;
  private final PreparedStatement statement;
  private final Pair<Schema, Schema> schemaPair;
  private final FieldMetadata fieldsMetadata;

  public PreparedStatementBinder(final PreparedStatement statement,
                                 final TGSinkConfig.PrimaryKeyMode pkMode,
                                 final String typeNameKey,
                                 final Pair<Schema, Schema> schemaPair,
                                 final FieldMetadata fieldsMetadata) {
    this.pkMode = pkMode;
    this.typeNameKey = typeNameKey;
    this.statement = statement;
    this.schemaPair = schemaPair;
    this.fieldsMetadata = fieldsMetadata;
  }

  public void bindRecord(final SinkRecord record) throws SQLException {
    if (record.valueSchema() != null) {
      this.bindNonKeyFields(record, (Struct) record.value(), this.bindKeyFields(record, 1));
    } else {
      this.bindDirectlyFromRecord(new HashMap<String, Object>((HashMap) record.value()));
    }
    this.statement.addBatch();
  }

  private int bindKeyFields(final SinkRecord record, final int index) throws SQLException {
    int index2 = index;
    switch (this.pkMode) {
      case NONE:
        if (!this.fieldsMetadata.keyFieldNames.isEmpty()) {
          throw new AssertionError();
        }
        break;
      case RECORD_KEY:
        final Schema keySchema = this.schemaPair.getLeft();
        if (keySchema.type().isPrimitive()) {
          assert this.fieldsMetadata.keyFieldNames.size() == 1;
          this.bindField(index2++, keySchema, record.key());
        } else {
          for (String fieldName : this.fieldsMetadata.keyFieldNames) {
            final Field field = keySchema.field(fieldName);
            this.bindField(index2++, field.schema(), ((Struct) record.key()).get(field));
          }
        }
        break;
      case RECORD_VALUE:
        for (String fieldName : this.fieldsMetadata.keyFieldNames) {
          final Field field = ((Schema) this.schemaPair.getRight()).field(fieldName);
          this.bindField(index2++, field.schema(), ((Struct) record.value()).get(field));
        }
        break;
      default:
        throw new ConnectException("Unknown primary key mode: " + this.pkMode);
    }

    return index2;
  }

  private void bindNonKeyFields(final SinkRecord record, final Struct valueStruct, final int index) throws SQLException {
    int index2 = index;
    for (String fieldName : this.fieldsMetadata.nonKeyFieldNames) {
      final Field field = record.valueSchema().field(fieldName);
      this.bindField(index2++, field.schema(), valueStruct.get(field));
    }
  }

  private void bindDirectlyFromRecord(final Map<String, Object> values) throws SQLException {
    if (values != null) {
      String tableKey = null;
      String idKey = null;
      for (String s : values.keySet()) {
        if (s.endsWith("_" + this.typeNameKey)) {
          tableKey = s;
        }
        if (s.endsWith("_id")) {
          idKey = s;
        }
      }
      if (tableKey != null) {
        values.remove(tableKey);
      }
      int index = 1;
      if (idKey != null) {
        this.bindField(index++, Schema.STRING_SCHEMA, values.get(idKey));
        values.remove(idKey);
      }
      Object currentValue;
      Schema currentSchema;
      for (final Iterator<String> iter = values.keySet().iterator();
           iter.hasNext();
           this.bindField(index++, currentSchema, currentValue)) {
        currentValue = values.get(iter.next());
        currentSchema = Schema.STRING_SCHEMA;
        if (currentValue instanceof Integer) {
          currentSchema = Schema.INT64_SCHEMA;
        } else if (currentValue instanceof Float) {
          currentSchema = Schema.FLOAT32_SCHEMA;
        } else if (currentValue instanceof Double) {
          currentSchema = Schema.FLOAT64_SCHEMA;
        } else if (currentValue instanceof Boolean) {
          currentSchema = Schema.BOOLEAN_SCHEMA;
        } else if (currentValue instanceof Long) {
          currentSchema = Schema.INT64_SCHEMA;
          currentValue = ((Long) currentValue).intValue();
        }
      }
    }
  }

  private void bindField(final int index, final Schema schema, final Object value) throws SQLException {
    this.bindField(this.statement, index, schema, value);
  }

  private void bindField(final PreparedStatement statement,
                         final int index,
                         final Schema schema,
                         final Object value) throws SQLException {
    if (value == null) {
      statement.setObject(index, null);
    } else {
      boolean bound = this.bindLogical(statement, index, schema, value);
      if (!bound) {
        bound = this.bindPrimitive(statement, index, schema, value);
      }
      if (!bound) {
        throw new ConnectException("Unsupported source data type: " + schema.type());
      }
    }
  }

  private boolean bindLogical(final PreparedStatement statement,
                              final int index,
                              final Schema schema,
                              final Object value) throws SQLException {
    if (schema.name() != null) {
      Calendar cal = Calendar.getInstance();
      switch (schema.name()) {
        case "org.apache.kafka.connect.data.Date":
          statement.setDate(index, new Date(((java.util.Date) value).getTime()), cal);
          return true;
        case "org.apache.kafka.connect.data.Decimal":
          statement.setBigDecimal(index, (BigDecimal) value);
          return true;
        case "org.apache.kafka.connect.data.Time":
          statement.setTime(index, new Time(((java.util.Date) value).getTime()), cal);
          return true;
        case "org.apache.kafka.connect.data.Timestamp":
          statement.setTimestamp(index, new Timestamp(((java.util.Date) value).getTime()), cal);
          return true;
        default:
          return false;
      }
    } else {
      return false;
    }
  }

  private boolean bindPrimitive(final PreparedStatement statement,
                                final int index,
                                final Schema schema,
                                final Object value) throws SQLException {
    switch (schema.type()) {
      case INT8:
      case INT16:
      case INT32:
      case INT64:
        statement.setInt(index, (Integer) value);
        break;
      case FLOAT32:
        statement.setFloat(index, (Float) value);
        break;
      case FLOAT64:
        statement.setInt(index, ((Double) value).intValue());
        break;
      case BOOLEAN:
        statement.setBoolean(index, (Boolean) value);
        break;
      case STRING:
        statement.setString(index, (String) value);
        break;
      default:
        return false;
    }

    return true;
  }
}
