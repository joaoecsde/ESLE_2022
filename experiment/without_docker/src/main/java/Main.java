import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.*;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.metric.log.LogExporterSpi;

import java.net.*;


import java.util.*;
import java.util.stream.Collectors;

public class Main {
    static int nrOfNodes;
    static String ip;

    public static void main(String[] args) {

        //parameters
        long cacheSizeInMB = Long.parseLong(args[0]);
        boolean isPartioned = Integer.parseInt(args[1]) == 1;
        boolean usesPersistence = Integer.parseInt(args[2]) == 1;
        nrOfNodes = Integer.parseInt(args[3]);
        ip = args[4];

        System.out.println("cache size: "+cacheSizeInMB);
        System.out.println("isPartioned: "+isPartioned);
        System.out.println("uses Persistence: "+usesPersistence);




        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        igniteConfiguration.setClientFailureDetectionTimeout(3000);

        //init node discovery
        TcpDiscoverySpi spi = new TcpDiscoverySpi();
        //spi.failureDetectionTimeoutEnabled(false);
        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();



        List<String> nodeAddresses = new ArrayList<>();

        try {
            nodeAddresses = getNodeAddresses();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ipFinder.setAddresses(nodeAddresses);

        spi.setIpFinder(ipFinder);


        //create and configure cache
        String dataRegionName="region__"+cacheSizeInMB;

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        //configure DataRegion
        DataRegionConfiguration defaultRegion = new DataRegionConfiguration();
        defaultRegion.setName(dataRegionName);
        defaultRegion.setInitialSize(cacheSizeInMB * 1024 * 1024);
        defaultRegion.setMaxSize(cacheSizeInMB * 1024 * 1024);


        //configure persistence
        defaultRegion.setPersistenceEnabled(usesPersistence);

        storageCfg.setDefaultDataRegionConfiguration(defaultRegion);
        storageCfg.setWalMode(WALMode.NONE);
        storageCfg.setWalArchivePath(storageCfg.getWalPath());


        CacheConfiguration cacheConfig = new CacheConfiguration<Object, Object>("MyCache");
        
        if(isPartioned){
            cacheConfig.setCacheMode(CacheMode.PARTITIONED);}
        else{
            cacheConfig.setCacheMode(CacheMode.REPLICATED);}

        cacheConfig.setDataRegionName(dataRegionName);
        cacheConfig.setStatisticsEnabled(false);

        //start server
        igniteConfiguration.setDiscoverySpi(spi);
        igniteConfiguration.setMetricsLogFrequency(0);
        igniteConfiguration.setDataStorageConfiguration(storageCfg);
        Ignite ignite = Ignition.start(igniteConfiguration);

        System.out.println("waiting for all nodes to join cluster");


        while(true){
            int servers= ignite.cluster().nodes().stream().filter(x -> !x.isClient()).collect(Collectors.toList()).size();
            if(servers == nrOfNodes) break;
        }

        System.out.println("#### all nodes connected!");

        ignite.cluster().active(true);

        System.out.println("#### cluster set to active");

        IgniteCache cache = ignite.getOrCreateCache(cacheConfig);

        System.out.println("\n\n######### nodes and cache started ######\n");

    }

    /**
     *
     * @return node addresses based on service name
     * @throws UnknownHostException if the service_name is incorrect
     */
    public static List<String> getNodeAddresses() throws UnknownHostException {


        String[] split = ip.split("\\.");
        String lastPart = split[split.length - 1];

        String ipBase = ip.substring(0, ip.length() - lastPart.length());


        List<String> nodeAddresses = new ArrayList<>();

        for (int i = 1 ; i <= nrOfNodes+4; i++){
            nodeAddresses.add(ipBase+i);
            //nodeAddresses.add("10.138.0."+i);
        }
        //nodeAddresses.add("10.128.0.2");

        nodeAddresses.forEach(System.out::println);

        return nodeAddresses;
    }   
}
