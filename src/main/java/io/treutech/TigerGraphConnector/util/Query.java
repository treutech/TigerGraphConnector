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
import io.treutech.TigerGraphConnector.sink.PreparedStatementBinder;
import io.treutech.TigerGraphConnector.sink.TGSinkConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;

public class Query {
  private final String body;
  private final SinkRecord record;
  private final FieldMetadata fieldMetadata;
  private final Pair<Schema, Schema> schemaPair;
  private final TGSinkConfig config;

  public Query(final String body,
               final TGSinkConfig config,
               final Pair<Schema, Schema> schemaPair,
               final FieldMetadata fieldmetadata,
               final SinkRecord record) {
    this.body = body;
    this.config = config;
    this.schemaPair = schemaPair;
    this.fieldMetadata = fieldmetadata;
    this.record = record;
  }

  public void genericRun(final Connection con) throws SQLException {
    final PreparedStatement stmt = con.prepareStatement(this.body);
    final PreparedStatementBinder binder =
        new PreparedStatementBinder(stmt, this.config.pkMode,
            this.config.tigergraph_type_name_key,
            this.schemaPair, this.fieldMetadata);
    binder.bindRecord(this.record);
    stmt.executeBatch();
  }
}
