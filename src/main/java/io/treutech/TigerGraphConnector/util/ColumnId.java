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

public class ColumnId {
  private final TableId tableId;
  private final String name;
  private final String alias;

  public ColumnId(final TableId tableId, final String name, final String alias) {
    this.tableId = tableId;
    this.name = name;
    this.alias = alias;
  }

  public TableId getTableId() {
    return this.tableId;
  }

  public String getName() {
    return this.name;
  }

  public String getAlias() {
    return this.alias;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ColumnId)) {
      return false;
    } else if (this == obj) {
      return true;
    } else {
      ColumnId otherObject = (ColumnId) obj;
      return (new EqualsBuilder()).append(this.tableId, otherObject.getTableId())
          .append(this.name, otherObject.getName())
          .append(this.alias, otherObject.getAlias()).isEquals();
    }
  }

  public int hashCode() {
    return (new HashCodeBuilder()).append(this.tableId).append(this.name).append(this.alias).toHashCode();
  }

  public String toString() {
    return "{ tableId='" + this.getTableId() + "', name='" + this.getName() + "', alias='" + this.getAlias() + "'}";
  }
}
