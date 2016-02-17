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

import java.util.Comparator;

/**
 * Used to wrap around long int values safely<p>
 * <br>
 * Needed to ensure that windowId wrap around safely<br>
 * {@see StablePriorityQueue}<br>
 * <br>
 *
 * @since 0.3.2
 */


class StableWrapper<E>
{
  public final int id;
  public final E object;

  public StableWrapper(E o, int id)
  {
    this.id = id;
    this.object = o;
  }
}

class StableWrapperNaturalComparator<E> implements Comparator<StableWrapper<E>>
{
  @Override
  public int compare(StableWrapper<E> o1, StableWrapper<E> o2)
  {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    int ret = ((Comparable) o1.object).compareTo(o2.object);

    if (ret == 0) {
      if (o1.id > o2.id) {
        ret = 1;
      }
      else if (o1.id < o2.id) {
        ret = -1;
      }
    }

    return ret;
  }
}

class StableWrapperProvidedComparator<E> implements Comparator<StableWrapper<E>>
{
  public final Comparator<? super E> comparator;

  public StableWrapperProvidedComparator(Comparator<? super E> comparator)
  {
    this.comparator = comparator;
  }

  @Override
  public int compare(StableWrapper<E> o1, StableWrapper<E> o2)
  {
    int ret = comparator.compare(o1.object, o2.object);

    if (ret == 0) {
      if (o1.id > o2.id) {
        ret = 1;
      }
      else if (o1.id < o2.id) {
        ret = -1;
      }
    }

    return ret;
  }
}
