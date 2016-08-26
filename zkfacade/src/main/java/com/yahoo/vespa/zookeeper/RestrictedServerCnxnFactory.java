package com.yahoo.vespa.zookeeper;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.NIOServerCnxnFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class is created by zookeeper by reflection, see the ZooKeeperServer constructor.
 * 
 * @author bratseth
 */
@SuppressWarnings("unused")
public class RestrictedServerCnxnFactory extends NIOServerCnxnFactory {
    
    private static final Logger log = Logger.getLogger(RestrictedServerCnxnFactory.class.getName());
    private final Set<String> zooKeeperServerHostnames;
    
    public RestrictedServerCnxnFactory() throws IOException {
        super();
        zooKeeperServerHostnames = toHostnameSet(System.getProperty(ZooKeeperServer.ZOOKEEPER_VESPA_SERVERS_PROPERTY));
    }
    
    private Set<String> toHostnameSet(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.isEmpty())
            throw new IllegalArgumentException("We have not received the list of ZooKeeper servers in this system");
        
        Set<String> hostnames = new HashSet<>();
        for (String hostname : commaSeparatedString.split(","))
            hostnames.add(hostname.trim());
        return hostnames;
    }

    @Override
    protected NIOServerCnxn createConnection(SocketChannel socket, SelectionKey selection) throws IOException {
        String remoteHost = ((InetSocketAddress)socket.getRemoteAddress()).getHostName();
        if ( ! zooKeeperServerHostnames.contains(remoteHost)) {
            String errorMessage = "Rejecting connection to ZooKeeper from " + remoteHost +
                                  ": This cluster only allow connection from nodes in this cluster. " +
                                  "Hosts in this cluster: " + zooKeeperServerHostnames;
            log.warning(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        return super.createConnection(socket, selection);
    }

}
