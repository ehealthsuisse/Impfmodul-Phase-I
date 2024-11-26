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

import static org.assertj.core.api.Assertions.assertThat;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
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
  public void afterAll() throws Exception {
    hazelcast1.shutdown();
    hazelcast2.shutdown();
  }

  @Test
  public void testPutAndGet() throws Exception {

    hazelcast1 = Hazelcast.newHazelcastInstance(getMulticastConfig());
    Thread.sleep(1000);
    assertThat(hazelcast1.getCluster().getMembers().size()).isEqualTo(1); // 1 member

    hazelcast2 = Hazelcast.newHazelcastInstance(getMulticastConfig());
    Thread.sleep(1000);

    assertThat(hazelcast1.getCluster().getMembers().size()).isEqualTo(2); // 2 members
    assertThat(hazelcast2.getCluster().getMembers().size()).isEqualTo(2);

    // 1 -> 2
    Map<Integer, String> map1 = hazelcast1.getMap("test");
    map1.put(1, "One");
    map1.put(2, "two");
    Thread.sleep(500);
    Map<Integer, String> map2 = hazelcast2.getMap("test");
    assertThat(map2.get(1)).isEqualTo("One");

    // 2 -> 1
    map2.put(3, "Three");
    Thread.sleep(500);
    assertThat(map1.get(3)).isEqualTo("Three");
  }

  private Config getMulticastConfig() {
    Config config = new Config();
    NetworkConfig network = config.getNetworkConfig();
    JoinConfig join = network.getJoin();
    join.getMulticastConfig().setEnabled(true);
    return config;
  }
}
