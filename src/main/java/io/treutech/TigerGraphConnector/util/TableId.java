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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class TableId {
  private final String catalogName;
  private final String schemaName;
  private final String tableName;

  public TableId(final String catalogName, final String schemaName, final String tableName) {
    this.catalogName = catalogName;
    this.schemaName = schemaName;
    this.tableName = tableName;
  }

  public String getCatalogName() {
    return this.catalogName;
  }

  public String getSchemaName() {
    return this.schemaName;
  }

  public String getTableName() {
    return this.tableName;
  }

  public boolean equals(final Object obj) {
    if (!(obj instanceof TableId)) {
      return false;
    } else if (this == obj) {
      return true;
    } else {
      TableId otherObject = (TableId) obj;
      return (new EqualsBuilder())
          .append(this.catalogName, otherObject.getCatalogName())
          .append(this.schemaName, otherObject.getSchemaName())
          .append(this.tableName, otherObject.getTableName()).isEquals();
    }
  }

  public int hashCode() {
    return (new HashCodeBuilder())
        .append(this.catalogName)
        .append(this.schemaName)
        .append(this.tableName).toHashCode();
  }

  public String toString() {
    return "{ catalogName='" + this.getCatalogName() +
        "', schemaName='" + this.getSchemaName() +
        "', tableName='" + this.getTableName() + "'}";
  }
}
