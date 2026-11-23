package com.smartasset.sentinel.config;

import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * Exposes the in-memory H2 database on a local TCP port so the external QA framework
 * can run JDBC assertions against the same database the app is using.
 */
@Configuration
@ConditionalOnProperty(name = "sentinel.h2.tcp.enabled", havingValue = "true")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer(@Value("${sentinel.h2.tcp.port:9092}") String port) throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpPort", port);
    }
}
