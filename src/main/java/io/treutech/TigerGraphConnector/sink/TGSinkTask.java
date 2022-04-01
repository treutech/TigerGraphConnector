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

import io.treutech.TigerGraphConnector.util.Query;
import io.treutech.TigerGraphConnector.util.QueryBuilder;
import io.treutech.TigerGraphConnector.util.TGConfigException;
import io.treutech.TigerGraphConnector.util.Version;
import com.tigergraph.jdbc.Driver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TGSinkTask extends SinkTask {
  private static final Logger log = LoggerFactory.getLogger(TGSinkTask.class);
  private Connection con;
  private TGSinkConfig config;
  int remainingRetries;

  public String version() {
    return Version.getVersion();
  }

  public void put(final Collection records) {
    for (Object record : records) {
      final SinkRecord sinkRecord = (SinkRecord) record;
      Query query = QueryBuilder.generateQuery(sinkRecord, this.config);
      int retryBackoffMs = this.config.retryBackoffMs;
      int prevRemainingRetries = this.remainingRetries;
      boolean success = false;
      while (this.remainingRetries > 0 && !success) {
        try {
          query.genericRun(this.con);
        } catch (SQLException sqle) {
          StringBuilder messages = new StringBuilder();
          for (Throwable e : sqle) {
            messages.append(e).append(System.lineSeparator());
          }
          this.remainingRetries--;
          if (this.remainingRetries == 0) {
            log.error("Failed to write query for sinkRecord value {}, using a total of {}",
                sinkRecord.value(), this.config.maxRetries);
            throw new ConnectException(new SQLException(messages.toString()));
          }

          log.warn("Failed to write query for sinkRecord value {}, remaining retries={}",
              sinkRecord.value(), this.remainingRetries, sqle);
          this.context.timeout(retryBackoffMs);
          retryBackoffMs *= 2;
        } finally {
          if (this.remainingRetries == prevRemainingRetries) {
            success = true;
          }
          prevRemainingRetries = this.remainingRetries;
        }
      }

      this.remainingRetries = this.config.maxRetries;
    }
  }

  public void start(final Map props) {
    log.info("Starting TigerGraph Sink Task");
    try {
      this.config = new TGSinkConfig(props);
    } catch (TGConfigException e) {
      throw new ConnectException("Couldn't start TigerGraph SinkTask due to a configuration error", e);
    }
    this.remainingRetries = this.config.maxRetries;
    Properties properties = new Properties();
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
      Driver driver = new Driver();
      this.con = driver.connect(sb.toString(), properties);
    } catch (SQLException e) {
      throw new ConnectException("Error Starting TigerGraph SinkTask", e);
    }
  }

  public void stop() {
    log.info("Stopping TigerGraph Sink Task");
    try {
      if (this.con != null) {
        this.con.close();
      }
    } catch (SQLException e) {
      log.error("Failed to stop TigerGraph Sink Task: {}", e.getMessage());
    }
  }
}
