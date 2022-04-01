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
import com.tigergraph.jdbc.Driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TGSourceTask extends SourceTask {
  private Connection con;
  protected TGSourceConfig config;
  private String timestamp;
  private static final Logger log = LoggerFactory.getLogger(TGSourceTask.class);

  public String version() {
    return Version.getVersion();
  }

  public List<SourceRecord> poll() throws InterruptedException {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < this.config.tigergraph_query_args.length; i++) {
      builder.append(this.config.tigergraph_query_pattern).append(',');
    }
    final String query = this.config.tigergraph_query.replace("pattern", StringUtils.chop(builder.toString()));
    final String queryName = this.getQueryName(query);
    final Map<String, String> sourcePartition = Collections.singletonMap(this.config.tigergraph_query_name_key, queryName);
    final List<Map<String, String>> partitions = new ArrayList<>();
    partitions.add(sourcePartition);
    final Map<Map<String, String>, Map<String, Object>> offsets = this.context.offsetStorageReader().offsets(partitions);
    Map<String, Object> offset = null;
    if (offsets != null) {
      offset = offsets.get(sourcePartition);
    }
    final List<SourceRecord> records = new ArrayList<>();
    try {
      try (PreparedStatement pstmt = this.con.prepareStatement(query)) {
        final String[] args = this.config.tigergraph_query_args;
        for (int i = 0; i < args.length; i++) {
          pstmt.setInt(i + 1, Integer.parseInt(args[i]));
        }

        if (this.config.timestampEnabled) {
          if (offset != null) {
            this.timestamp = (String) offset.get(this.config.tigergraph_offset_name_key);
          }
          pstmt.setString(args.length + 1,
              this.timestamp.replace(" ", "%20").replace(":", "%3A"));
        }

        try {

          try (ResultSet rs = pstmt.executeQuery()) {
            do {
              final ResultSetMetaData metaData = rs.getMetaData();
              if (metaData.getColumnCount() > 0) {
                final Pair<SchemaMapping, String> schemaMappingResult = SchemaMapping.create(queryName, metaData, this.config);
                final String sourceOffsetValue = (new SimpleDateFormat(this.config.timestampFormat)).format(new Date());
                final Map<String, String> sourceOffset = Collections.singletonMap(this.config.tigergraph_offset_name_key, sourceOffsetValue);
                while (rs.next()) {
                  final Struct record = new Struct(( schemaMappingResult.getLeft()).getSchema());
                  for (final Pair<Field, ColumnConverter> pair : (schemaMappingResult.getLeft()).getFields()) {
                    try {
                      record.put(pair.getLeft(), pair.getRight() != null ?
                          (rs.wasNull() ? null : pair.getRight().convert(rs))
                          : schemaMappingResult.getRight());
                    } catch (IOException ioex) {
                      log.warn("Error mapping fields into Connect record", ioex);
                      throw new ConnectException(ioex);
                    } catch (SQLException sqle) {
                      log.warn("SQL error mapping fields into Connect record", sqle);
                      throw new DataException(sqle);
                    }
                  }
                  records.add(new SourceRecord(sourcePartition, sourceOffset, this.config.topic, record.schema(), record));
                  if (this.config.timestampEnabled) {
                    try {
                      final String timestamp_value = (String) rs.getObject(this.config.timestampAttrName);
                      if (timestamp_value != null) {
                        final Date date = (new SimpleDateFormat(this.config.timestampFormat)).parse(timestamp_value);
                        final Date ts = (new SimpleDateFormat(this.config.timestampFormat)).parse(this.timestamp);
                        if (date.getTime() > ts.getTime()) {
                          this.setTimeStamp(timestamp_value);
                        }
                      }
                    } catch (ParseException pex) {
                      final StringWriter sw = new StringWriter();
                      final PrintWriter pw = new PrintWriter(sw);
                      pex.printStackTrace(pw);
                      log.error("TGSourceTask parse exception {} \n {}", pex.getMessage(), sw);
                    }
                  }
                }
              }
            } while (!rs.isLast());
          }
        } catch (Exception e) {
          final StringWriter sw = new StringWriter();
          final PrintWriter pw = new PrintWriter(sw);
          e.printStackTrace(pw);
          log.error("Failed to build schema{} \n {}", e.getMessage(), sw);
          throw new InterruptedException(MessageFormat.format("Failed to build schema: {0}", e.getMessage()));
        }
      }
      return records;
    } catch (SQLException sqle) {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      sqle.printStackTrace(pw);
      log.error("Failed to create statatement {} \n {}", sqle.getMessage(), sw);
      throw new InterruptedException(MessageFormat.format("Failed to create statement: {0}", sqle.getMessage()));
    }
  }

  public void start(final Map props) {
    log.info("Starting TigerGraph Source Task");
    try {
      this.config = new TGSourceConfig(props);
    } catch (TGConfigException var6) {
      throw new ConnectException(MessageFormat.format("Couldn't start TigerGraph SourceTask due to a configuration error: {0}", var6.getMessage()));
    }
    this.timestamp = (new SimpleDateFormat(this.config.timestampFormat)).format(new Date());
    final Properties properties = new Properties();
    properties.put("username", this.config.tigergraph_username);
    properties.put("password", this.config.tigergraph_password);
    properties.put("graph", this.config.tigergraph_graph);
    StringBuilder sb = new StringBuilder();
    if (this.config.tigergraph_ssl_enabled) {
      properties.put("trustStore", this.config.ssl_truststore_path);
      properties.put("trustStorePassword", this.config.ssl_truststore_password);
      properties.put("trustStoreType", this.config.ssl_truststore_type);
      properties.put("keyStore", this.config.ssl_keystore_path);
      properties.put("keyStorePassword", this.config.ssl_keystore_password);
      properties.put("keyStoreType", this.config.ssl_keystore_type);
      sb.append("jdbc:tg:https://").append(this.config.tigergraph_ip).append(":").append(this.config.tigergraph_port);
    } else {
      sb.append("jdbc:tg:http://").append(this.config.tigergraph_ip).append(":").append(this.config.tigergraph_port);
    }
    try {
      final Driver driver = new Driver();
      this.con = driver.connect(sb.toString(), properties);
    } catch (SQLException var5) {
      throw new ConnectException(MessageFormat.format("Error Starting TigerGraph SourceTask: {0}", var5.getMessage()));
    }
  }

  public void stop() {
    log.info("Stopping TigerGraph Source Task");
    try {
      if (this.con != null) {
        this.con.close();
      }
    } catch (SQLException sqle) {
      log.error("Error stopping TigerGraph Source Task:", sqle);
    }
  }

  private synchronized void setTimeStamp(String date) {
    this.timestamp = date;
  }

  private String getQueryName(final String query) {
    String result = "no_query_name";
    if (!"".equals(query)) {
      int offset1 = 0;
      if (query.contains(" ")) {
        offset1 = query.indexOf(32);
      }
      int offset2 = query.length();
      if (query.contains("(")) {
        offset2 = query.indexOf(40);
      }
      result = query.substring(offset1 + 1, offset2);
    }

    return result;
  }
}
