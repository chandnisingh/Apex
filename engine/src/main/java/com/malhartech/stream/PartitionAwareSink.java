/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.stream;

import com.malhartech.dag.SerDe;
import com.malhartech.dag.Sink;
import com.malhartech.dag.Tuple;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
public class PartitionAwareSink implements Sink
{
  final SerDe serde;
  final HashSet<ByteBuffer> partitions;
  final Sink output;

  public PartitionAwareSink(SerDe serde, List<byte[]> partitions, Sink output)
  {
    this.serde = serde;

    this.partitions = new HashSet<ByteBuffer>(partitions.size());
    for (byte[] partition: partitions) {
      this.partitions.add(ByteBuffer.wrap(partition));
    }

    this.output = output;
  }

  @Override
  public void process(Object payload)
  {
    if (payload instanceof Tuple || partitions.contains(ByteBuffer.wrap(serde.getPartition(payload)))) {
      output.process(payload);
    }
  }
}