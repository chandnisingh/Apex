/*
 *  Copyright (c) 2012 Malhar, Inc.
 *  All Rights Reserved.
 */
package com.malhartech.dag;

import org.apache.hadoop.conf.Configuration;

/**
 *
 * @author Chetan Narsude <chetan@malhar-inc.com>
 */
public interface Component<T1 extends Configuration, T2 extends Context> extends Sink
{
  /*
   * if the Component is capable of taking only 1 input, it will be called INPUT.
   */
  public static final String INPUT = "input";
  public static final String OUTPUT = "output";

  public void setup(T1 config);

  public void activate(T2 context);

  public void deactivate();

  public void teardown();

  public Sink connect(String port, Sink sink); // connect to output port
}