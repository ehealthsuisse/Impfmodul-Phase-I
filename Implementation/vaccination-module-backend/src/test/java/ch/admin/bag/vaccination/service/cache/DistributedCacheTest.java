/**
 * Copyright (c) 2022 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.admin.bag.vaccination.service.cache;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * Test the clustering ability of Hazelcast based on multicast discovery.
 *
 */
public class DistributedCacheTest {
  private HazelcastInstance hazelcast1;
  private HazelcastInstance hazelcast2;

  @AfterEach
  public void tearDown() {
    if (hazelcast1 != null) {
      hazelcast1.shutdown();
    }
    if (hazelcast2 != null) {
      hazelcast2.shutdown();
    }
    Hazelcast.shutdownAll();
  }

  @Test
  @Disabled
  public void testPutAndGet() {
    // Start first instance
    hazelcast1 = Hazelcast.newHazelcastInstance(getTcpIpConfig());
    // Wait for the first instance to initialize
    await().atMost(1, SECONDS).until(() -> hazelcast1.getCluster().getMembers().size() == 1);

    // Start second instance
    hazelcast2 = Hazelcast.newHazelcastInstance(getTcpIpConfig());
    // Wait for cluster to form with both members
    await().atMost(1, SECONDS).until(() -> hazelcast1.getCluster().getMembers().size() == 2);
    await().atMost(1, SECONDS).until(() -> hazelcast2.getCluster().getMembers().size() == 2);

    // Test data sharing: hazelcast1 -> hazelcast2
    Map<Integer, String> map1 = hazelcast1.getMap("test");
    map1.put(1, "One");
    map1.put(2, "Two");

    // Wait for replication
    await().atMost(500, MILLISECONDS).until(() -> {
      Map<Integer, String> map2 = hazelcast2.getMap("test");
      return map2.get(1) != null && map2.get(1).equals("One");
    });

    Map<Integer, String> map2 = hazelcast2.getMap("test");
    assertThat(map2.get(1)).isEqualTo("One");
    assertThat(map2.get(2)).isEqualTo("Two");

    // Test data sharing: hazelcast2 -> hazelcast1
    map2.put(3, "Three");
    await().atMost(500, MILLISECONDS).until(() -> map1.get(3) != null && map1.get(3).equals("Three"));
    assertThat(map1.get(3)).isEqualTo("Three");
  }

  private Config getTcpIpConfig() {
    Config config = new Config();
    config.setClusterName("test-cluster");
    NetworkConfig network = config.getNetworkConfig();
    network.setPort(5701).setPortAutoIncrement(true);
    JoinConfig join = network.getJoin();
    join.getMulticastConfig().setEnabled(false);
    join.getTcpIpConfig()
        .setEnabled(true)
        .addMember("127.0.0.1:5701")
        .addMember("127.0.0.1:5702");
    // Enable reuse address to avoid TIME_WAIT issues
    network.setReuseAddress(true);
    // Set JVM to prefer IPv4
    System.setProperty("java.net.preferIPv4Stack", "true");
    return config;
  }
}
