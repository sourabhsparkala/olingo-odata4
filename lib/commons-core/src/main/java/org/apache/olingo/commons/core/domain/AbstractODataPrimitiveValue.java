/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.commons.core.domain;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.UUID;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.domain.AbstractODataValue;
import org.apache.olingo.commons.api.domain.ODataPrimitiveValue;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;

public abstract class AbstractODataPrimitiveValue extends AbstractODataValue implements ODataPrimitiveValue {

  private static final long serialVersionUID = 8889282662298376036L;

  public static abstract class AbstractBuilder implements Builder {

    private final ODataServiceVersion version;

    public AbstractBuilder(final ODataServiceVersion version) {
      this.version = version;
    }

    protected abstract AbstractODataPrimitiveValue getInstance();

    @Override
    public AbstractBuilder setType(final EdmType type) {
      EdmPrimitiveTypeKind primitiveTypeKind = null;
      if (type != null) {
        if (type.getKind() != EdmTypeKind.PRIMITIVE) {
          throw new IllegalArgumentException(String.format("Provided type %s is not primitive", type));
        }
        primitiveTypeKind = EdmPrimitiveTypeKind.valueOf(type.getName());
      }
      return setType(primitiveTypeKind);
    }

    @Override
    public AbstractBuilder setType(final EdmPrimitiveTypeKind type) {
      if (type != null && !type.getSupportedVersions().contains(version)) {
        throw new IllegalArgumentException(String.format(
                "Type %s not supported by OData version %s", type.toString(), version));
      }
      if (type == EdmPrimitiveTypeKind.Stream) {
        throw new IllegalArgumentException(String.format(
                "Cannot build a primitive value for %s", EdmPrimitiveTypeKind.Stream.toString()));
      }
      if (type == EdmPrimitiveTypeKind.Geography || type == EdmPrimitiveTypeKind.Geometry) {
        throw new IllegalArgumentException(
                type + "is not an instantiable type. "
                + "An entity can declare a property to be of type Geometry. "
                + "An instance of an entity MUST NOT have a value of type Geometry. "
                + "Each value MUST be of some subtype.");
      }

      getInstance().typeKind = type == null ? EdmPrimitiveTypeKind.String : type;
      getInstance().type = EdmPrimitiveTypeFactory.getInstance(getInstance().typeKind);

      return this;
    }

    @Override
    public AbstractBuilder setText(final String text) {
      getInstance().text = text;
      return this;
    }

    @Override
    public AbstractBuilder setValue(final Object value) {
      getInstance().value = value;
      return this;
    }

    @Override
    public AbstractODataPrimitiveValue build() {
      if (getInstance().text == null && getInstance().value == null) {
        throw new IllegalArgumentException("Must provide either text or value");
      }
      if (getInstance().text != null && getInstance().value != null) {
        throw new IllegalArgumentException("Cannot provide both text and value");
      }

      if (getInstance().type == null) {
        setType(EdmPrimitiveTypeKind.String);
      }

      if (getInstance().text != null) {
        final Class<?> returnType = getInstance().type.getDefaultType().isAssignableFrom(Calendar.class)
                ? Timestamp.class : getInstance().type.getDefaultType();
        try {
          // TODO: when Edm is available, set facets when calling this method
          getInstance().value = getInstance().type.valueOfString(
                  getInstance().text, null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null,
                  returnType);
        } catch (EdmPrimitiveTypeException e) {
          throw new IllegalArgumentException(e);
        }
      }
      if (getInstance().value != null) {
        try {
          // TODO: when Edm is available, set facets when calling this method
          getInstance().text = getInstance().type.valueToString(
                  getInstance().value, null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null);
        } catch (EdmPrimitiveTypeException e) {
          throw new IllegalArgumentException(e);
        }
      }

      return getInstance();
    }

    @Override
    public ODataPrimitiveValue buildBoolean(final Boolean value) {
      return setType(EdmPrimitiveTypeKind.Boolean).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildInt32(final Integer value) {
      return setType(EdmPrimitiveTypeKind.Int32).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildSingle(final Float value) {
      return setType(EdmPrimitiveTypeKind.Single).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildDouble(final Double value) {
      return setType(EdmPrimitiveTypeKind.Double).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildString(final String value) {
      return setType(EdmPrimitiveTypeKind.String).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildGuid(final UUID value) {
      return setType(EdmPrimitiveTypeKind.Guid).setValue(value).build();
    }

    @Override
    public ODataPrimitiveValue buildBinary(final byte[] value) {
      return setType(EdmPrimitiveTypeKind.Binary).setValue(value).build();
    }

  }

  /**
   * Type kind.
   */
  private EdmPrimitiveTypeKind typeKind;

  /**
   * Type.
   */
  private EdmPrimitiveType type;

  /**
   * Text value.
   */
  private String text;

  /**
   * Actual value.
   */
  private Object value;

  protected AbstractODataPrimitiveValue() {
    super(null);
  }

  @Override
  public String getTypeName() {
    return typeKind.getFullQualifiedName().toString();
  }

  @Override
  public EdmPrimitiveTypeKind getTypeKind() {
    return typeKind;
  }

  @Override
  public EdmPrimitiveType getType() {
    return type;
  }

  @Override
  public Object toValue() {
    return this.value;
  }

  @Override
  public <T> T toCastValue(final Class<T> reference) throws EdmPrimitiveTypeException {
    return typeKind.isGeospatial()
            ? reference.cast(this.value)
            // TODO: when Edm is available, set facets when calling this method
            : type.valueOfString(this.text,
                    null, null, Constants.DEFAULT_PRECISION, Constants.DEFAULT_SCALE, null, reference);
  }

  @Override
  public String toString() {
    return this.text;
  }

}