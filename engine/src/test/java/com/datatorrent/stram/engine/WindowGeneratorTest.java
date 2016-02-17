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
package com.datatorrent.stram.engine;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datatorrent.common.util.BaseOperator;
import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.api.DefaultInputPort;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.InputOperator;
import com.datatorrent.api.Sink;

import com.datatorrent.bufferserver.packet.MessageType;
import com.datatorrent.common.util.ScheduledThreadPoolExecutor;
import com.datatorrent.stram.StramLocalCluster;
import com.datatorrent.stram.plan.logical.LogicalPlan;
import com.datatorrent.stram.support.ManualScheduledExecutorService;
import com.datatorrent.stram.tuple.ResetWindowTuple;
import com.datatorrent.stram.tuple.Tuple;

public class WindowGeneratorTest
{
  @Test
  public void test2ndResetWindow() throws InterruptedException
  {
    logger.info("Testing 2nd Reset Window");

    ManualScheduledExecutorService msse = new ManualScheduledExecutorService(1);
    WindowGenerator generator = new WindowGenerator(msse, (WindowGenerator.MAX_WINDOW_ID << 1) + 1024);

    generator.setFirstWindow(0L);
    generator.setResetWindow(0L);
    generator.setWindowWidth(1);

    SweepableReservoir reservoir = generator.acquireReservoir(Node.OUTPUT, (WindowGenerator.MAX_WINDOW_ID << 1) + 1024);
    final AtomicBoolean loggingEnabled = new AtomicBoolean(true);
    reservoir.setSink(new Sink<Object>()
    {
      @Override
      public void put(Object payload)
      {
        if (loggingEnabled.get()) {
          logger.debug(payload.toString());
        }

      }

      @Override
      public int getCount(boolean reset)
      {
        return 0;
      }

    });

    generator.activate(null);

    msse.tick(1); /* reset window and begin window */
    msse.tick(1); /* end window and begin window */
    loggingEnabled.set(false);
    for (int i = 0; i < WindowGenerator.MAX_WINDOW_ID - 2; i++) {
      msse.tick(1); /* end window and begin window */
    }
    loggingEnabled.set(true);
    msse.tick(1); /* end window, reset window, begin window */

    final AtomicInteger beginWindowCount = new AtomicInteger(0);
    final AtomicInteger endWindowCount = new AtomicInteger(0);
    final AtomicInteger resetWindowCount = new AtomicInteger(0);
    Tuple t;
    reservoir.sweep();
    while ((t = reservoir.sweep()) != null) {
      reservoir.remove();
      switch (t.getType()) {
        case BEGIN_WINDOW:
          beginWindowCount.incrementAndGet();
          break;

        case END_WINDOW:
          endWindowCount.incrementAndGet();
          break;

        case RESET_WINDOW:
          resetWindowCount.incrementAndGet();
          break;
      }
    }

    Assert.assertEquals("begin windows", WindowGenerator.MAX_WINDOW_ID + 1 + 1, beginWindowCount.get());
    Assert.assertEquals("end windows", WindowGenerator.MAX_WINDOW_ID + 1, endWindowCount.get());
    Assert.assertEquals("reset windows", 2, resetWindowCount.get());
  }

  /**
   * Test of resetWindow functionality of WindowGenerator.
   */
  @Test
  public void testResetWindow()
  {
    ManualScheduledExecutorService msse = new ManualScheduledExecutorService(1);
    msse.setCurrentTimeMillis(0x0afebabe * 1000L);
    WindowGenerator generator = new WindowGenerator(msse, WindowGenerator.MAX_WINDOW_ID + 1024);

    final long currentTIme = msse.getCurrentTimeMillis();
    final int windowWidth = 0x1234abcd;
    generator.setFirstWindow(currentTIme);
    generator.setResetWindow(currentTIme);
    generator.setWindowWidth(windowWidth);
    SweepableReservoir reservoir = generator.acquireReservoir(Node.OUTPUT, 1024);
    reservoir.setSink(new Sink<Object>()
    {
      boolean firsttime = true;

      @Override
      public int getCount(boolean reset)
      {
        return 0;
      }

      @Override
      public void put(Object payload)
      {
        assert (false);
        if (firsttime) {
          assert (payload instanceof ResetWindowTuple);
          firsttime = false;
        }
        else {
          assert (payload instanceof Tuple);
        }
      }

    });
    generator.activate(null);
    msse.tick(1);

    Assert.assertNull(reservoir.sweep());
    ResetWindowTuple rwt = (ResetWindowTuple)reservoir.sweep();
    reservoir.remove();
    assert (rwt.getWindowId() == 0x0afebabe00000000L);
    assert (rwt.getBaseSeconds() * 1000L == currentTIme);
    assert (rwt.getIntervalMillis() == windowWidth);

    Tuple t = reservoir.sweep();
    reservoir.remove();
    assert (t.getType() == MessageType.BEGIN_WINDOW);
    assert (t.getWindowId() == 0x0afebabe00000000L);

    assert (reservoir.sweep() == null);
  }

  @Test
  public void testWindowGen() throws Exception
  {
    final AtomicLong currentWindow = new AtomicLong();
    final AtomicInteger beginWindowCount = new AtomicInteger();
    final AtomicInteger endWindowCount = new AtomicInteger();

    final AtomicLong windowXor = new AtomicLong();

    Sink<Object> s = new Sink<Object>()
    {
      @Override
      public int getCount(boolean reset)
      {
        return 0;
      }

      @Override
      public void put(Object payload)
      {
        logger.debug("unexpected payload {}", payload);
      }

    };

    ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1, "WindowGenerator");
    int windowWidth = 200;
    long firstWindowMillis = stpe.getCurrentTimeMillis();
    firstWindowMillis -= firstWindowMillis % 1000L;

    WindowGenerator wg = new WindowGenerator(new ScheduledThreadPoolExecutor(1, "WindowGenerator"), WindowGenerator.MAX_WINDOW_ID + 1024);
    wg.setResetWindow(firstWindowMillis);
    wg.setFirstWindow(firstWindowMillis);
    wg.setWindowWidth(windowWidth);
    SweepableReservoir reservoir = wg.acquireReservoir("GeneratorTester", windowWidth);
    reservoir.setSink(s);

    wg.activate(null);
    Thread.sleep(200);
    wg.deactivate();

    reservoir.sweep(); /* just transfer over all the control tuples */
    Tuple t;
    while ((t = reservoir.sweep()) != null) {
      reservoir.remove();
      long windowId = t.getWindowId();

      switch (t.getType()) {
        case BEGIN_WINDOW:
          currentWindow.set(windowId);
          beginWindowCount.incrementAndGet();
          windowXor.set(windowXor.get() ^ windowId);
          break;

        case END_WINDOW:
          endWindowCount.incrementAndGet();
          windowXor.set(windowXor.get() ^ windowId);
          break;

        case RESET_WINDOW:
          break;

        default:
          currentWindow.set(0);
          break;
      }
    }
    long lastWindowMillis = System.currentTimeMillis();

    Assert.assertEquals("only last window open", currentWindow.get(), windowXor.get());

    long expectedCnt = (lastWindowMillis - firstWindowMillis) / windowWidth;

    Assert.assertTrue("Minimum begin window count", expectedCnt + 1 <= beginWindowCount.get());
    Assert.assertEquals("end window count", beginWindowCount.get() - 1, endWindowCount.get());
  }

  static class RandomNumberGenerator implements InputOperator
  {
    public final transient DefaultOutputPort<Integer> output = new DefaultOutputPort<Integer>();

    @Override
    public void emitTuples()
    {
      try {
        Thread.sleep(500);
      }
      catch (InterruptedException ex) {
        logger.debug("interrupted!", ex);
      }

      output.emit(++count);
    }

    @Override
    public void beginWindow(long windowId)
    {
    }

    @Override
    public void endWindow()
    {
    }

    @Override
    public void setup(OperatorContext context)
    {
    }

    @Override
    public void teardown()
    {
    }

    int count;
  }

  static class MyLogger extends BaseOperator
  {
    public final transient DefaultInputPort<Integer> input = new DefaultInputPort<Integer>()
    {
      @Override
      public void process(Integer tuple)
      {
        logger.debug("received {}", tuple);
      }

    };
  }

  @Test
  public void testOutofSequenceError() throws Exception
  {
    logger.info("Testing Out of Sequence Error");
    LogicalPlan dag = new LogicalPlan();

    RandomNumberGenerator rng = dag.addOperator("random", new RandomNumberGenerator());
    MyLogger ml = dag.addOperator("logger", new MyLogger());

    dag.addStream("stream", rng.output, ml.input);

    StramLocalCluster lc = new StramLocalCluster(dag);
    lc.run(10000);
  }

  @Test
  public void testWindowToTime()
  {
    long first = 1431714014000L;

    long time1 = WindowGenerator.getWindowMillis(6149164867354886271L, first, 500);
    long time2 = WindowGenerator.getWindowMillis(6149164867354886272L, first, 500);

    long window1 = WindowGenerator.getWindowId(time1, first, 500);
    long window2 = WindowGenerator.getWindowId(time2, first, 500);

    Assert.assertEquals("window 1", 6149164867354886271L, window1);
    Assert.assertEquals("window 2", 6149164867354886272L, window2);

    Assert.assertTrue(time2 > time1);
  }

  public static final Logger logger = LoggerFactory.getLogger(WindowGeneratorTest.class);
}
