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

import io.treutech.TigerGraphConnector.util.ColumnDefinition;
import io.treutech.TigerGraphConnector.util.ColumnId;
import io.treutech.TigerGraphConnector.util.TGResourceUtils;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;

public final class SchemaMapping {
  private final Schema schema;
  private final List<Pair<Field, ColumnConverter>> fields;

  public Schema getSchema() {
    return this.schema;
  }

  public List<Pair<Field, ColumnConverter>> getFields() {
    return this.fields;
  }

  public static Pair<SchemaMapping, String> create(final String schemaName,
                                                   final ResultSetMetaData metadata,
                                                   final TGSourceConfig config) {
    final List<Pair<ColumnId, ColumnDefinition>> colDefns = TGResourceUtils.describeColumns(metadata);
    final List<Pair<String, ColumnConverter>> colConvertersByFieldName = new ArrayList<>();
    final SchemaBuilder builder = SchemaBuilder.struct().name(schemaName);
    int columnNumber = 0;
    for (final Pair<ColumnId, ColumnDefinition> def : colDefns) {
      columnNumber++;
      final String fieldName = TGResourceUtils.addFieldToSchema(def.getRight(), builder);
      if (fieldName != null) {
        final ColumnConverter converter = TGResourceUtils.createColumnConverter(def.getRight(), columnNumber);
        colConvertersByFieldName.add(Pair.of(fieldName, converter));
      }
    }
    final ColumnDefinition tableColDefn = TGResourceUtils.buildTableNameColumnDefinition(metadata, config);
    final String tableFieldName = TGResourceUtils.addFieldToSchema(tableColDefn, builder);
    colConvertersByFieldName.add(Pair.of(tableFieldName, null));
    final Schema schema = builder.build();
    return Pair.of(new SchemaMapping(schema, colConvertersByFieldName), tableColDefn.getId().getTableId().getCatalogName());
  }

  private SchemaMapping(final Schema schema, final List<Pair<String, ColumnConverter>> convertersByFieldName) {
    assert schema != null;
    assert convertersByFieldName != null;
    assert !convertersByFieldName.isEmpty();
    this.schema = schema;
    final List<Pair<Field, ColumnConverter>> fieldSetters = new ArrayList<>(convertersByFieldName.size());
    for (final Pair<String, ColumnConverter> pair : convertersByFieldName) {
      final ColumnConverter converter = pair.getRight();
      final Field field = schema.field(pair.getLeft());
      assert field != null;
      fieldSetters.add(Pair.of(field, converter));
    }
    this.fields = Collections.unmodifiableList(fieldSetters);
  }

  public String toString() {
    return "Mapping for " + this.schema.name();
  }
}
