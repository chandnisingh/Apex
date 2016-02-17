/**
 * Copyright (C) 2015 DataTorrent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.stram.util;

import java.io.*;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.apache.hadoop.io.Writable;

/**
 * Adapter for Hadoop RPC to implement Writable using Java serialization.
 *
 * @since 0.3.2
 */
public abstract class AbstractWritableAdapter implements Writable, Serializable
{
  private static final long serialVersionUID = 201306061421L;

  @Override
  public void readFields(DataInput arg0) throws IOException
  {
    int len = arg0.readInt();
    byte[] bytes = new byte[len];
    arg0.readFully(bytes);
    try {
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      @SuppressWarnings("unchecked")
      Map<String, Object> properties = (Map<String, Object>)ois.readObject();
      Field[] fields = this.getClass().getFields();
      AccessibleObject.setAccessible(fields, true);
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        String fieldName = field.getName();
        if (properties.containsKey(fieldName)) {
          field.set(this, properties.get(fieldName));
        }
      }
      ois.close();
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void write(DataOutput arg0) throws IOException
  {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(bos);
    try {
      Map<String, Object> properties = new java.util.HashMap<String, Object>();
      Field[] fields = this.getClass().getFields();
      AccessibleObject.setAccessible(fields, true);
      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        if (!Modifier.isStatic(field.getModifiers())) {
          String fieldName = field.getName();
          Object fieldValue = field.get(this);
          properties.put(fieldName, fieldValue);
        }
      }
      oos.writeObject(properties);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
    oos.flush();
    byte[] bytes = bos.toByteArray();
    arg0.writeInt(bytes.length);
    arg0.write(bytes);
    oos.close();
  }

}
