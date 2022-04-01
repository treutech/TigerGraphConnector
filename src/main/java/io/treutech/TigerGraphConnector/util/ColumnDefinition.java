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

public class ColumnDefinition {
  private final ColumnId id;
  private final String typeName;
  private final int tgType;
  private final int displaySize;
  private final int precision;
  private final int scale;
  private final boolean autoIncremented;
  private final boolean isPrimaryKey;
  private final String classNameForType;
  private final boolean signedNumber;

  public ColumnDefinition(final ColumnId id,
                          final int tgType,
                          final String typeName,
                          final String classNameForType,
                          final int precision,
                          final int scale,
                          final int displaySize,
                          final boolean autoIncremented,
                          final boolean isPrimaryKey,
                          final boolean signedNumber) {
    this.id = id;
    this.typeName = typeName;
    this.tgType = tgType;
    this.displaySize = displaySize;
    this.precision = precision;
    this.scale = scale;
    this.autoIncremented = autoIncremented;
    this.isPrimaryKey = isPrimaryKey;
    this.classNameForType = classNameForType;
    this.signedNumber = signedNumber;
  }

  public ColumnId getId() {
    return this.id;
  }

  public String getTypeName() {
    return this.typeName;
  }

  public int getTgType() {
    return this.tgType;
  }

  public int getDisplaySize() {
    return this.displaySize;
  }

  public int getPrecision() {
    return this.precision;
  }

  public int getScale() {
    return this.scale;
  }

  public boolean isAutoIncremented() {
    return this.autoIncremented;
  }

  public boolean getAutoIncremented() {
    return this.autoIncremented;
  }

  public boolean isIsPrimaryKey() {
    return this.isPrimaryKey;
  }

  public boolean getIsPrimaryKey() {
    return this.isPrimaryKey;
  }

  public String getClassNameForType() {
    return this.classNameForType;
  }

  public boolean isSignedNumber() {
    return this.signedNumber;
  }

  public boolean getSignNumber() {
    return this.signedNumber;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ColumnDefinition)) {
      return false;
    } else if (this == obj) {
      return true;
    } else {
      ColumnDefinition otherObject = (ColumnDefinition) obj;
      return (new EqualsBuilder()).append(this.id, otherObject.getId())
          .append(this.typeName, otherObject.getTypeName())
          .append(this.tgType, otherObject.getTgType())
          .append(this.displaySize, otherObject.getDisplaySize())
          .append(this.precision, otherObject.getPrecision())
          .append(this.scale, otherObject.getScale())
          .append(this.autoIncremented, otherObject.getAutoIncremented())
          .append(this.classNameForType, otherObject.getClassNameForType())
          .append(this.signedNumber, otherObject.getSignNumber())
          .append(this.isPrimaryKey, otherObject.getIsPrimaryKey()).isEquals();
    }
  }

  public int hashCode() {
    return (new HashCodeBuilder())
        .append(this.id)
        .append(this.typeName)
        .append(this.classNameForType)
        .append(this.tgType)
        .append(this.displaySize)
        .append(this.precision)
        .append(this.scale)
        .append(this.autoIncremented)
        .append(this.isPrimaryKey)
        .append(this.getSignNumber()).toHashCode();
  }

  public String toString() {
    return "{ id='" + this.getId() +
        "', typeName='" + this.getTypeName() +
        "', tgType='" + this.getTgType() +
        "', displaySize='" + this.getDisplaySize() +
        "', precision='" + this.getPrecision() +
        "', scale='" + this.getScale() +
        "', autoIncremented='" + this.isAutoIncremented() +
        "', autoIncremented='" + this.isSignedNumber() +
        "', isPrimaryKey='" + this.isIsPrimaryKey() + "'}";
  }
}
