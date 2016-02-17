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
package com.datatorrent.bufferserver.policy;

import java.util.Set;

import com.datatorrent.bufferserver.internal.PhysicalNode;
import com.datatorrent.bufferserver.util.SerializedData;

/**
 *
 * Distributes to downstream nodes in a roundrobin fashion. A round robin load balancing policy<p>
 * <br>
 * A round robin load balaning policy. Does not take into account busy/load of a downstream physical node. Extends the base class {@link com.datatorrent.bufferserver.policy.AbstractPolicy}<br>
 * <br>
 *
 * @since 0.3.2
 */
public class RoundRobin extends AbstractPolicy
{
  int index;

  /**
   * Constructor
   */
  public RoundRobin()
  {
    index = 0;
  }

  /**
   *
   *
   * @param nodes Set of downstream {@link com.datatorrent.bufferserver.PhysicalNode}s
   * @param data Opaque {@link com.datatorrent.bufferserver.util.SerializedData} to be send
   */

  @Override
  public boolean distribute(Set<PhysicalNode> nodes, SerializedData data) throws InterruptedException
  {
    int size = nodes.size();
    if (size > 0) { // why do i need to do this check? synchronization issues? because if there is no one interested, the logical group should not exist!
      index %= size;
      int count = index++;
      /*
       * May need to look at accessing nodes as arrays, so that iteration can be avoided
       * This matters if say there are 1000+ partitions(?) and this may happen in a Big Message
       * application
       *
       */
      for (PhysicalNode node : nodes) {
        if (count-- == 0) {
          return node.send(data);
        }
      }
    }

    return false;
  }
}
