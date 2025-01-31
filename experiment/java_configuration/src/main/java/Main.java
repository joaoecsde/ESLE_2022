import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.metric.log.LogExporterSpi;

import java.net.*;


import java.util.*;

public class Main {
    static int nrOfNodes;

    public static void main(String[] args) {


        nrOfNodes = Integer.parseInt(System.getenv("REPLICAS"));
        System.out.println("REPLICAS = "+nrOfNodes);
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();

        igniteConfiguration.setClientFailureDetectionTimeout(3000);

        //init node discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        spi.failureDetectionTimeoutEnabled(false);
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();

        List<String> nodeAddresses = new ArrayList<>();

        try {
            nodeAddresses = getNodeAddresses();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ipFinder.setAddresses(nodeAddresses);
        spi.setIpFinder(ipFinder);



        //start server
        igniteConfiguration.setDiscoverySpi(spi);
        igniteConfiguration.setMetricsLogFrequency(0);
        //retrieve metrics
        /*LogExporterSpi logExporter = new LogExporterSpi();
        logExporter.setPeriod(600_000);
        logExporter.setExportFilter(mreg -> mreg.name().startsWith("MyCache"));
        igniteConfiguration.setMetricExporterSpi(logExporter);
*/

        Ignite ignite = Ignition.start(igniteConfiguration);


        //init cache
        CacheConfiguration cacheConfig = new CacheConfiguration<Object, Object>("MyCache").setCacheMode(CacheMode.PARTITIONED);
        cacheConfig.setStatisticsEnabled(false);
        IgniteCache cache = ignite.getOrCreateCache(cacheConfig);

        System.out.println("##########cache created");
    }


    /**
     *
     * @return node addresses based on service name
     * @throws UnknownHostException if the service_name is incorrect
     */
    public static List<String> getNodeAddresses() throws UnknownHostException {
        List<String> nodeAddresses = new ArrayList<>();

        List<Inet4Address> inet4Addresses = new ArrayList<>();
        InetAddress[] addresses = InetAddress.getAllByName("ignite_service");
        for (int i = 0; i < addresses.length; i++) {
            InetAddress addr = addresses[i];
            if (addr instanceof Inet4Address) inet4Addresses.add((Inet4Address) addr);
        }

        //i.e.startingAddr = ignite_service/10.0.5.28   is transformed into -> 10.0.5.X
        Inet4Address serviceBaseAddr = inet4Addresses.get(0);

        System.out.println("########Base address of swarm: "+serviceBaseAddr);

        String[] splitIp = serviceBaseAddr.toString().split("/")[1].split("\\.");
        String s = splitIp[0] + "." + splitIp[1] + "." + splitIp[2] + ".";

        int lastDigit = Integer.parseInt(splitIp[splitIp.length - 1]);

        
        for (int i = 1; i <= nrOfNodes; i++) {
            nodeAddresses.add(s + (lastDigit + i));
            System.out.println("########## node_"+ i+"_address: "+ s + (lastDigit + i));
        
        //nodeAddresses.add(s + (lastDigit + 1));
        }
        return nodeAddresses;

    }   
}
