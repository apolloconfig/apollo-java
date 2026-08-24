/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.foundation.internals;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

public enum NetworkInterfaceManager {
  INSTANCE;

  private InetAddress local;

  private InetAddress localHost;

  NetworkInterfaceManager() {
    load();
  }

  public InetAddress findValidateIp(List<InetAddress> addresses) {
    InetAddress local = null;
    int maxWeight = -1;
    for (InetAddress address : addresses) {
      if (address instanceof Inet4Address) {
        int weight = 0;

        if (address.isSiteLocalAddress()) {
          weight += 8;
        }

        if (address.isLinkLocalAddress()) {
          weight += 4;
        }

        if (address.isLoopbackAddress()) {
          weight += 2;
        }

        /**
         * The following logic is removed as we will sort the network interfaces according to the index asc to determine
         * the priorities between network interfaces, see https://github.com/ctripcorp/apollo/pull/1986
         */
        // has host name
        /*
        if (!Objects.equals(address.getHostName(), address.getHostAddress())) {
          weight += 1;
        }
        */

        if (weight > maxWeight) {
          maxWeight = weight;
          local = address;
        }
      }
    }
    return local;
  }

  public String getLocalHostAddress() {
    return local.getHostAddress();
  }

  public String getLocalHostName() {
    try {
      if (null == localHost) {
        localHost = InetAddress.getLocalHost();
      }
      return localHost.getHostName();
    } catch (UnknownHostException e) {
      return local.getHostName();
    }
  }

  private String getProperty(String name) {
    String value = System.getProperty(name);

    if (value == null) {
      value = System.getenv(name);
    }

    return value;
  }

  private void load() {
    String ip = getProperty("host.ip");

    if (ip != null) {
      try {
        local = InetAddress.getByName(ip);
        return;
      } catch (Exception e) {
        System.err.println(e);
        // ignore
      }
    }

    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces == null) {
        local = InetAddress.getLoopbackAddress();
        return;
      }
      List<NetworkInterface> nis = Collections.list(interfaces);
      //sort the network interfaces according to the index asc
      nis.sort(Comparator.comparingInt(NetworkInterface::getIndex));
      List<InetAddress> addresses = new ArrayList<>();
      InetAddress resolvedAddress = null;

      try {
        for (NetworkInterface ni : nis) {
          if (ni.isUp() && !ni.isLoopback()) {
            addresses.addAll(Collections.list(ni.getInetAddresses()));
          }
        }
        resolvedAddress = findValidateIp(addresses);
      } catch (Exception e) {
        // ignore
      }
      if (resolvedAddress != null) {
        local = resolvedAddress;
        return;
      }
    } catch (SocketException e) {
      // ignore it
    }

    local = InetAddress.getLoopbackAddress();
  }
}
