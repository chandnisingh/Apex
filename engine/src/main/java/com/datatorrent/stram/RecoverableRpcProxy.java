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
package com.datatorrent.stram;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;

import static java.lang.Thread.sleep;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.UserGroupInformation;

import com.datatorrent.netlet.util.DTThrowable;
import com.datatorrent.stram.api.StreamingContainerUmbilicalProtocol;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * Heartbeat RPC proxy invocation handler that handles fail over.
 *
 * @since 0.9.3
 */
public class RecoverableRpcProxy implements java.lang.reflect.InvocationHandler, Closeable
{
  private final static Logger LOG = LoggerFactory.getLogger(RecoverableRpcProxy.class);
  public static final String QP_retryTimeoutMillis = "retryTimeoutMillis";
  public static final String QP_retryDelayMillis = "retryDelayMillis";
  public static final String QP_rpcTimeout = "rpcTimeout";
  private final Configuration conf;
  private final String appPath;
  private StreamingContainerUmbilicalProtocol umbilical;
  private String lastConnectURI;
  private long lastCompletedCallTms;
  private long retryTimeoutMillis = 30000;
  private long retryDelayMillis = 10000;
  private int rpcTimeout = 5000;

  public RecoverableRpcProxy(String appPath, Configuration conf) throws IOException
  {
    this.conf = conf;
    this.appPath = appPath;
    connect();
  }

  private void connect() throws IOException
  {
    FSRecoveryHandler fsrh = new FSRecoveryHandler(appPath, conf);
    String uriStr = fsrh.readConnectUri();
    if (!uriStr.equals(lastConnectURI)) {
      // reset timeout
      LOG.debug("Got new RPC connect address {}", uriStr);
      lastCompletedCallTms = System.currentTimeMillis();
      lastConnectURI = uriStr;
    }
    URI heartbeatUri = URI.create(uriStr);

    String queryStr = heartbeatUri.getQuery();
    List<NameValuePair> queryList = null;
    if (queryStr != null) {
      queryList = URLEncodedUtils.parse(queryStr, Charset.defaultCharset());
    }
    if (queryList != null) {
      for (NameValuePair pair : queryList) {
        String value = pair.getValue();
        String key = pair.getName();
        if (QP_rpcTimeout.equals(key)) {
          this.rpcTimeout = Integer.parseInt(value);
        }
        else if (QP_retryTimeoutMillis.equals(key)) {
          this.retryTimeoutMillis = Long.parseLong(value);
        }
        else if (QP_retryDelayMillis.equals(key)) {
          this.retryDelayMillis = Long.parseLong(value);
        }
      }
    }
    InetSocketAddress address = NetUtils.createSocketAddrForHost(heartbeatUri.getHost(), heartbeatUri.getPort());
    umbilical = RPC.getProxy(StreamingContainerUmbilicalProtocol.class, StreamingContainerUmbilicalProtocol.versionID, address, UserGroupInformation.getCurrentUser(), conf, NetUtils.getDefaultSocketFactory(conf), rpcTimeout);
  }

  public StreamingContainerUmbilicalProtocol getProxy()
  {
    StreamingContainerUmbilicalProtocol recoverableProxy = (StreamingContainerUmbilicalProtocol) java.lang.reflect.Proxy.newProxyInstance(umbilical.getClass().getClassLoader(), umbilical.getClass().getInterfaces(), this);
    return recoverableProxy;
  }

  @Override
  @SuppressWarnings("SleepWhileInLoop")
  public Object invoke(Object proxy, Method method, Object[] args) throws ConnectException, SocketTimeoutException, InterruptedException, IllegalAccessException
  {
    Object result;
    for (; ; ) {
      try {
        if (umbilical == null) {
          connect();
        }
        //long start = System.nanoTime();
        result = method.invoke(umbilical, args);
        lastCompletedCallTms = System.currentTimeMillis();
        //long end = System.nanoTime();
        //LOG.info(String.format("%s took %d ns", method.getName(), (end - start)));
        return result;
      }
      catch (InvocationTargetException e) {
        // handle RPC failure
        Throwable targetException = e.getTargetException();
        long connectMillis = System.currentTimeMillis() - lastCompletedCallTms;
        if (connectMillis < retryTimeoutMillis) {
          LOG.warn("RPC failure, attempting reconnect after {} ms (remaining {} ms)", retryDelayMillis, retryTimeoutMillis - connectMillis, targetException);
          close();
          sleep(retryDelayMillis);
        }
        else {
          LOG.error("Giving up RPC connection recovery after {} ms", connectMillis, targetException);
          if (targetException instanceof java.net.ConnectException) {
            throw (java.net.ConnectException) targetException;
          }
          else if (targetException instanceof java.net.SocketTimeoutException) {
            throw (java.net.SocketTimeoutException) targetException;
          }
          else {
            DTThrowable.rethrow(targetException);
          }
        }
      }
      catch (IOException ex) {
        close();
        throw new RuntimeException(ex);
      }
    }
  }

  @Override
  public void close()
  {
    LOG.debug("Closing RPC connection {}", lastConnectURI);
    if (umbilical != null) {
      RPC.stopProxy(umbilical);
      umbilical = null;
    }
  }

  public static URI toConnectURI(InetSocketAddress address, int rpcTimeoutMillis, int retryDelayMillis, int retryTimeoutMillis) throws Exception
  {
    StringBuilder query = new StringBuilder(256);
    query.append(RecoverableRpcProxy.QP_rpcTimeout + '=').append(rpcTimeoutMillis);
    query.append('&');
    query.append(RecoverableRpcProxy.QP_retryDelayMillis + '=').append(retryDelayMillis);
    query.append('&');
    query.append(RecoverableRpcProxy.QP_retryTimeoutMillis + '=').append(retryTimeoutMillis);
    return new URI("stram", null, address.getHostName(), address.getPort(), null, query.toString(), null);
  }

}
