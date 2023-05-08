/**
 * Copyright (c) 2023 eHealth Suisse
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
package ch.admin.bag.vaccination.service.husky;

import lombok.extern.slf4j.Slf4j;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerEventHandlerIF;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.udp.UDPNetSyslogServerConfig;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 
 * Local Syslog server for test purposes
 *
 */
@Component
@Profile("test")
@Slf4j
public class LocalSyslogServer {
  private SyslogServerIF syslogServer;
  private String lastMessage;

  public void startup() {
    SyslogServer.shutdown();

    SyslogServerConfigIF config = new UDPNetSyslogServerConfig();
    config.setUseStructuredData(true);
    config.setHost("localhost");
    config.setPort(9898);

    syslogServer = SyslogServer.createThreadedInstance("udp", config);

    SyslogServerEventHandlerIF myHandler = new SyslogServerEventHandlerIF() {
      @Override
      public void event(SyslogServerIF syslogServer, SyslogServerEventIF event) {
        log.info("syslogEvent: {} {}", syslogServer.getProtocol(), event.getMessage());
        lastMessage = event.getMessage();
      }
    };
    syslogServer.getConfig().addEventHandler(myHandler);
  }

  public String getLastMessage() {
    String copyLastMessage = lastMessage;
    lastMessage = null;
    return copyLastMessage;
  }

  public void shutdown() {
    if (syslogServer == null) {
      return;
    }
    syslogServer.shutdown();
  }
}
