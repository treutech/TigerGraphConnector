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

import io.treutech.TigerGraphConnector.source.ColumnConverter;
import io.treutech.TigerGraphConnector.source.TGSourceConfig;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TGResourceUtils {
  private static final Logger log = LoggerFactory.getLogger(TGResourceUtils.class);

  public static List<Pair<ColumnId, ColumnDefinition>> describeColumns(final ResultSetMetaData rsMetadata) {
    final List<Pair<ColumnId, ColumnDefinition>> result = new ArrayList<>();
    int columnCount = 0;
    try {
      columnCount = rsMetadata.getColumnCount();
    } catch (SQLException ignored) {}

    for (int i = 1; i <= columnCount; ++i) {
      final ColumnDefinition def = describeColumn(rsMetadata, i);
      result.add(Pair.of(def.getId(), def));
    }
    return result;
  }

  private static String emptyOnException(final Object o, final String method, final int column) {
    try {
      return (String) o.getClass().getMethod(method, int.class).invoke(o, column);
    } catch (Exception e) {
      return "";
    }
  }

  private static int zeroOnException(final Object o, final String method, final int column) {
    try {
      return (int) o.getClass().getMethod(method, int.class).invoke(o, column);
    } catch (Exception e) {
      return 0;
    }
  }

  private static boolean falseOnException(final Object o, final String method, final int column) {
    try {
      return (boolean) o.getClass().getMethod(method, int.class).invoke(o, column);
    } catch (Exception e) {
      return false;
    }
  }

  public static ColumnDefinition describeColumn(final ResultSetMetaData rsMetadata, final int column) {

    final TableId tableId = new TableId(
        emptyOnException(rsMetadata, "getCatalogName", column),
        emptyOnException(rsMetadata, "getSchemaName", column),
        emptyOnException(rsMetadata, "getTableName", column));

    final ColumnId id = new ColumnId(
        tableId,
        emptyOnException(rsMetadata, "getColumnName", column),
        emptyOnException(rsMetadata, "getColumnLabel", column));

    return new ColumnDefinition(
        id,
        zeroOnException(rsMetadata, "getColumnType", column),
        emptyOnException(rsMetadata, "getColumnTypeName", column),
        emptyOnException(rsMetadata, "getColumnClassName", column),
        zeroOnException(rsMetadata, "getPrecision", column),
        zeroOnException(rsMetadata, "getScale", column),
        zeroOnException(rsMetadata, "getColumnDisplaySize", column),
        falseOnException(rsMetadata, "isAutoIncrement", column),
        false,
        falseOnException(rsMetadata, "isSigned", column));
  }

  public static String addFieldToSchema(final ColumnDefinition columnDef, final SchemaBuilder builder) {
    final int precision = columnDef.getPrecision();
    int scale = columnDef.getScale();
    final String fieldName = columnDef.getId().getAlias();
    final int tgType = columnDef.getTgType();
    final SchemaBuilder fieldBuilder;
    switch (tgType) {
      case -16:
      case -15:
      case -9:
      case -1:
      case 1:
      case 12:
      case 70:
      case 2005:
      case 2009:
      case 2011:
        builder.field(fieldName, Schema.STRING_SCHEMA);
        break;
      case -8:
      case 1111:
      case 2000:
      case 2001:
      case 2002:
      case 2003:
      case 2006:
      default:
        log.warn("JDBC type {} ({}) not currently supported", tgType, columnDef.getTypeName());
        return null;
      case -7:
        builder.field(fieldName, Schema.INT8_SCHEMA);
        break;
      case -6:
        if (columnDef.isSignedNumber()) {
          builder.field(fieldName, Schema.INT8_SCHEMA);
        } else {
          builder.field(fieldName, Schema.INT16_SCHEMA);
        }
        break;
      case -5:
        builder.field(fieldName, Schema.INT64_SCHEMA);
        break;
      case -4:
      case -3:
      case -2:
      case 2004:
        builder.field(fieldName, Schema.BYTES_SCHEMA);
        break;
      case 0:
        log.debug("TIGERGRAPH SQL type 'NULL' not currently supported for column '{}'", fieldName);
        return null;
      case 2:
        log.debug("NUMERIC with precision: '{}' and scale: '{}'", precision, scale);
        Schema schema;
        if (scale == 0 && precision < 19) {
          if (precision > 9) {
            schema = Schema.INT64_SCHEMA;
          } else if (precision > 4) {
            schema = Schema.INT32_SCHEMA;
          } else if (precision > 2) {
            schema = Schema.INT16_SCHEMA;
          } else {
            schema = Schema.INT8_SCHEMA;
          }
          builder.field(fieldName, schema);
        } else if (scale > 0) {
          schema = Schema.FLOAT64_SCHEMA;
          builder.field(fieldName, schema);
        }
        break;
      case 3:
        log.debug("DECIMAL with precision: '{}' and scale: '{}'", precision, scale);
        scale = columnDef.getScale();
        if (scale == -127) {
          scale = 127;
        }
        fieldBuilder = Decimal.builder(scale);
        builder.field(fieldName, fieldBuilder.build());
        break;
      case 4:
        if (columnDef.isSignedNumber()) {
          builder.field(fieldName, Schema.INT32_SCHEMA);
        } else {
          builder.field(fieldName, Schema.INT64_SCHEMA);
        }
        break;
      case 5:
        if (columnDef.isSignedNumber()) {
          builder.field(fieldName, Schema.INT16_SCHEMA);
        } else {
          builder.field(fieldName, Schema.INT32_SCHEMA);
        }
        break;
      case 6:
      case 8:
        builder.field(fieldName, Schema.FLOAT64_SCHEMA);
        break;
      case 7:
        builder.field(fieldName, Schema.FLOAT32_SCHEMA);
        break;
      case 16:
        builder.field(fieldName, Schema.BOOLEAN_SCHEMA);
        break;
      case 91:
        fieldBuilder = Date.builder();
        builder.field(fieldName, fieldBuilder.build());
        break;
      case 92:
        fieldBuilder = Time.builder();
        builder.field(fieldName, fieldBuilder.build());
        break;
      case 93:
        fieldBuilder = Timestamp.builder();
        builder.field(fieldName, fieldBuilder.build());
    }
    return fieldName;
  }

  public static ColumnConverter createColumnConverter(final ColumnDefinition def, final int col) {
    switch (def.getTgType()) {
      case -16:
      case -15:
      case -9:
        return r -> r.getNString(col);
      case -8:
      case 0:
      case 1111:
      case 2000:
      case 2001:
      case 2002:
      case 2003:
      case 2006:
      default:
        return null;
      case -6:
        if (def.isSignedNumber()) {
          return r -> r.getByte(col);
        }
        return r -> r.getShort(col);
      case -5:
        return r -> r.getLong(col);
      case -4:
      case -3:
      case -2:
        return r -> r.getBytes(col);
      case -1:
      case 1:
      case 12:
        return r -> r.getString(col);
      case 2:
        int precision = def.getPrecision();
        int scale = def.getScale();
        log.trace("NUMERIC with precision: '{}' and scale: '{}'", precision, scale);
        if (scale == 0 && precision < 19) {
          if (precision > 9) {
            return r -> r.getLong(col);
          } else if (precision > 4) {
            return r -> r.getInt(col);
          } else {
            if (precision > 2) {
              return r -> r.getShort(col);
            }
            return r -> r.getByte(col);
          }
        } else {
          if (precision < 19) {
            if (scale < 1 && scale >= -84) {
              if (precision > 9) {
                return r -> r.getLong(col);
              }
              if (precision > 4) {
                return r -> r.getInt(col);
              }
              if (precision > 2) {
                return r -> r.getShort(col);
              }
              return r -> r.getByte(col);
            }
            if (scale > 0) {
              return r -> r.getDouble(col);
            }
          }
          return r -> r.getBigDecimal(col);
        }
      case 3:
        return r -> r.getBigDecimal(col);
      case 4:
        if (def.isSignedNumber()) {
          return r -> r.getInt(col);
        }
        return r -> r.getLong(col);
      case 5:
        if (def.isSignedNumber()) {
          return r -> r.getShort(col);
        }
        return r -> r.getInt(col);
      case 6:
      case 8:
        return r -> r.getDouble(col);
      case 7:
        return r -> r.getFloat(col);
      case 16:
        return r -> r.getBoolean(col);
      case 70:
        log.debug("SQL type 'DATALINK' not currently supported");
        return null;
      case 91:
        return r -> r.getDate(col, Calendar.getInstance());
      case 92:
        return r -> r.getTime(col, Calendar.getInstance());
      case 93:
        return r -> r.getTimestamp(col, Calendar.getInstance());
      case 2004:
        log.debug("SQL type 'BLOB' not currently supported");
        return null;
      case 2005:
        log.debug("SQL type 'CLOB' not currently supported");
        return null;
      case 2009:
        log.debug("SQL type 'SQLXML' not currently supported");
        return null;
      case 2011:
        log.debug("SQL type 'NCLOB' not currently supported");
        return null;
    }
  }

  public static ColumnDefinition buildTableNameColumnDefinition(final ResultSetMetaData rsMetadata,
                                                                final TGSourceConfig config){
    final TableId tableId = new TableId(
        emptyOnException(rsMetadata, "getCatalogName", 1),
        emptyOnException(rsMetadata, "getSchemaName", 1),
        emptyOnException(rsMetadata, "getTableName", 1));
    String name = emptyOnException(rsMetadata, "getColumnName", 1);
    final int position = name.indexOf(95);
    if (position > 0) {
      name = name.substring(0, position + 1) + config.tigergraph_type_name_key;
    } else {
      name = name + "_" + config.tigergraph_type_name_key;
    }
    return new ColumnDefinition(
        new ColumnId(tableId, name, name),
        zeroOnException(rsMetadata, "getColumnType", 1),
        emptyOnException(rsMetadata, "getColumnTypeName", 1),
        emptyOnException(rsMetadata, "getColumnClassName", 1),
        zeroOnException(rsMetadata, "getPrecision", 1),
        zeroOnException(rsMetadata, "getScale", 1),
        zeroOnException(rsMetadata, "getColumnDisplaySize", 1),
        false,
        false,
        false);
  }

  public static Pair<TableId, String> getTableDetails(final SinkRecord record, final String typeKeyName) {
    final Schema valueSchema = record.valueSchema();
    if (valueSchema != null) {
      List<Field> fields = valueSchema.fields();
      for (Field field : fields) {
        final String name = field.name();
        final int position = name.indexOf(95);
        if (position > 0) {
          final String typeName = name.substring(0, position);
          String elementType = "vertex";
          if (typeName.startsWith("e")) {
            elementType = "edge";
          }
          if (name.endsWith("_" + typeKeyName)) {
            return Pair.of(new TableId("", "", (String) ((Struct) record.value()).get(field)), elementType);
          }
        }
      }
    } else {
      final Map<String, String> values = new HashMap<>((Map<String, String>) record.value());
      for (String key : values.keySet()) {
        final int position = key.indexOf(95);
        if (position > 0) {
          final String typeName = key.substring(0, position);
          String elementType = "vertex";
          if (typeName.startsWith("e")) {
            elementType = "edge";
          }

          if (key.endsWith("_" + typeKeyName)) {
            return Pair.of(new TableId("", "",  values.get(key)), elementType);
          }
        }
      }
    }

    return Pair.of(new TableId("", "", ""), "");
  }
}
