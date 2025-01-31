import java.io.*;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;


public class Main {


    //experiment parameters
    static int minutesPerTest;
    static int timeToWarmUpCache;
    static int lowerBound = 20;
    static int higherBound = 20;
    static double writeRatio = 0.15d;  //i.e ratio of 0.2 -> 20 % writes, 80% reads

    static int clientId;
    static int experimentNR;
    static String ips;


    //factors
    static int nodes;
    static long cacheSize;
    static boolean isPartionAware;
    static boolean cacheIsPartioned;
    static boolean isPerstistent;
    static String cpuCores;


    //files
    static String resultsCSV;
    static String throughputSnapshots;

    static ClientCache cache;

    static String retrievedVal = "";

    static long retryFrequency = 5000; //after how many ms connecting to the cluster is reattempted

    public static void main(String[] args) {
        System.out.println("\n #####################################\n");
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        long client_start = System.currentTimeMillis();

        System.out.println(format.format(new Date(System.currentTimeMillis())) + "   Starting Client");

        //parameters for running the Client
        minutesPerTest = Integer.parseInt(args[0]);
        timeToWarmUpCache = Integer.parseInt(args[1]);
        resultsCSV = args[2] + "/results.csv";
        throughputSnapshots = args[2] + "/throughput_snapshots/snapshot_";
        clientId = Integer.parseInt(args[3]);

        System.out.println(format.format(new Date(System.currentTimeMillis())) + "running experiment for: " + minutesPerTest + "mins  with warmup time: " + timeToWarmUpCache);


        //experiment factors
        experimentNR = Integer.parseInt(System.getenv("Experiment"));
        nodes = Integer.parseInt(System.getenv("Nodes"));
        cacheSize = Long.parseLong(System.getenv("Cache_Size"));
        isPartionAware = Integer.parseInt(System.getenv("Partition_Awareness")) == 1;
        cacheIsPartioned = Integer.parseInt(System.getenv("Is_Partioned")) == 1;
        isPerstistent = Integer.parseInt(System.getenv("Persistence")) == 1;
        ips=String.valueOf(System.getenv("IPS"));

        cpuCores = System.getenv("Cpu_Cores");
        if (cpuCores.equals("")) cpuCores = "-1";


        System.out.println(format.format(new Date(System.currentTimeMillis())) +
                "\n clientID: " + clientId +
                "\n nodes: " + nodes +
                "\n cache_size: " + cacheSize +
                "\n isPartionAware: " + isPartionAware +
                "\n cacheIsPartioned: " + cacheIsPartioned +
                "\n isPersistent: " + isPerstistent +
                "\n Cpu Cores: " + cpuCores);


        ClientConfiguration cfg = new ClientConfiguration();
        IgniteClient client = null;

        long heartbeat_counter = 0;
        long previousTime = 0;

        while (true) {
            try {
                Thread.sleep(retryFrequency);

                long now = System.currentTimeMillis();
                heartbeat_counter += System.currentTimeMillis() - previousTime;
                previousTime = now;
                if (heartbeat_counter / 30000 >= 1) { //print heartbeat every 30 seconds
                    System.out.println(format.format(new Date(now)) + "  Client is still attempting to connect");
                    heartbeat_counter = 0;
                }


                /*
                TcpDiscoverySpi spi = new TcpDiscoverySpi();
                TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
                List<String> nodeAddresses = getNodeAddresses();
                ipFinder.setAddresses(nodeAddresses);
                spi.setIpFinder(ipFinder);
                cfg.setDiscoverySpi(spi);

                client = Ignition.start(cfg);
                 */

                String[] ipsArr = ips.split(",");

                for (int i = 0; i<ipsArr.length; i++){
                    System.out.println("checking ip: "+ipsArr[i]);
                }
                cfg.setAddresses(ipsArr);


                //cfg.setAddresses("10.138.0.2", "10.138.0.3", "10.138.0.4", "10.138.0.5", "10.138.0.6", "10.138.0.7", "10.138.0.8", "10.138.0.9", "10.138.0.10");
                cfg.setPartitionAwarenessEnabled(isPartionAware);
                client = Ignition.startClient(cfg);

                Collection<ClusterNode> c = client.cluster().nodes();

                List<ClusterNode> servers = c.stream().filter(node -> !node.isClient()).collect(Collectors.toList());

                c.forEach(x -> System.out.println(servers.size()+" servernodes of "+ nodes+"  joined."));

                if (servers.size() < nodes) continue; //not all nodes have joined the cluster yet

                long afterStart = System.currentTimeMillis();
                System.out.println("Client connecting to cluster took: " + ((afterStart - client_start) / 1000) + " seconds");


                client.cacheNames().forEach(x -> System.out.println("##### connected to cache+ " + x));
                cache = client.cache("MyCache");

                //perform benchmarks and append the results
                doBenchmarks();
                break;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //clean up
        cache.removeAll();
        while (cache.size() != 0) {
        }

        client.close();
        System.exit(0);
    }

    public static void doBenchmarks() {

        warm_up_cache();

        long startTime = System.currentTimeMillis();
        long stopTime = startTime + minutesPerTest * 60 * 1000;

        System.out.println("starting the measurements: " + new SimpleDateFormat().format(startTime) + "   stopping at: " + new SimpleDateFormat().format(stopTime));

        //counters
        long totalBytesSend = 0;
        int operations = 0;

        Map<Long, Long> throughputSnapshots = new HashMap<>();

        long previousTime = startTime;
        long previousTotalBytes = 0;

        int countReads = 0;
        int countWrites = 0;
        long timeReads = 0;
        long timeWrites = 0;


        while (System.currentTimeMillis() < stopTime) {

            //take snapshots of throughput (approx every second)
            if ((System.currentTimeMillis() - previousTime) / 1000 > 1) {
                long currentThroughput = (totalBytesSend - previousTotalBytes) / (System.currentTimeMillis() - previousTime) * 1000;
                throughputSnapshots.put((System.currentTimeMillis() - startTime) / 1000, currentThroughput);
                previousTime = System.currentTimeMillis();
                previousTotalBytes = totalBytesSend;
            }


            //create random string-value of byte size in bounds
            int bytes = (int) ((Math.random() * (higherBound - lowerBound)) + lowerBound);
            String randomString = randomString(bytes);

            long t1 = System.currentTimeMillis();

            if (Math.random() < writeRatio) {
                cache.put(countWrites, randomString);
                countWrites++;
                timeWrites += System.currentTimeMillis() - t1;
            } else {
                int key= (int) (Math.random() * countWrites);
                retrievedVal = (String) cache.get(key);
                if(operations%100000==0){
                    //System.out.println("########");
                    //System.out.println("current random String: "+randomString);
                    //System.out.println("current nr of writes: "+countWrites);
                    System.out.println("example value read key:"+key +" val:: " +retrievedVal);
                }
                countReads++;
                timeReads += System.currentTimeMillis() - t1;
            }

            //counters for calculating throughput
            totalBytesSend += bytes;
            operations++;
        }

        double avgTimeReads = (double) timeReads / (double) countReads;
        double avgTimeWrites = (double) timeWrites / (double)countWrites;
        System.out.println("final value retrieved was: " + retrievedVal);

        long throughput = (1000 * totalBytesSend / (System.currentTimeMillis() - startTime));
        System.out.println("stopped at: " + new SimpleDateFormat().format(new Date(System.currentTimeMillis())));
        System.out.println("throughput: " + throughput);
        System.out.println("operations: " + operations);
        System.out.println("reads: " + countReads + "     totalTime reads: " + timeReads + "   avg: " + ((double)timeReads /(double) countReads));
        System.out.println("writes: " + countWrites + "     totalTime writes: " + timeWrites + "   avg: " + ((double)timeWrites /(double) countWrites));

        //clientId, nodes, cacheSize, isPartionAware, cacheIsPartioned, isPerstistent  ,cpuCores , throughput,
        // operations, reads, total_time_reads, avg_time_reads, writes, total_time_writes, avg_time_writes
        String result = experimentNR+","
                +clientId + "," +
                        nodes + "," +
                        cacheSize + "," +
                        isPartionAware + "," +
                        cacheIsPartioned + "," +
                        isPerstistent + "," +
                        cpuCores + "," +
                        throughput + "," +
                        avgTimeReads + "," +
                        avgTimeWrites;

        writeToCSV(result, throughputSnapshots);
    }

    public static void warm_up_cache() {
        System.out.println(new SimpleDateFormat().format(System.currentTimeMillis())+":   Begin warming up the cache");
        long start = System.currentTimeMillis();

        int offset = -1000000;
        int index = 0;
        int operations = 0;

        while (System.currentTimeMillis() < start + timeToWarmUpCache * 60 * 1000) {

            int bytes = (int) ((Math.random() * (higherBound - lowerBound)) + lowerBound);
            String randomString = randomString(bytes);

            if (Math.random() > 0.5d) {
                index++;
                cache.put(offset + index, randomString);
                continue;
            } else {
                String retrievedVal = (String) cache.get(Math.floor((operations) / 2) + offset);
            }
            operations++;
        }

    }


    public static String randomString(int length) {

        /*
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[length];
        random.nextBytes(r);
        String s = new String(r);
         */

        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String s = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return s;
    }

    public static void writeToCSV(String resultString, Map<Long, Long> snapshots) {
        try {
            File f = new File(resultsCSV);

            String toBeWritten = "";

            if (!f.exists()) {
                toBeWritten += "experiment,clientId,nodes,cacheSize,isPartionAware,cacheIsPartioned,isPerstistent,cpuCores,throughput,avg_time_reads,avg_time_writes";
            }
            toBeWritten += "\n" + resultString;
            PrintWriter output = new PrintWriter(new FileWriter(f, true));
            output.write(toBeWritten);
            output.close();

            toBeWritten = "";
            f = new File(throughputSnapshots + nodes + ".csv");
            if (!f.exists()) {
                toBeWritten += "second,throughput";
            }
            for (long l : snapshots.keySet()) {
                long throughput = snapshots.get(l);
                toBeWritten += "\n" + l + "," + throughput;
            }
            output = new PrintWriter(new FileWriter(f, false));
            output.write(toBeWritten);
            output.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getNodeAddresses() {

        List<String> nodeAddresses = new ArrayList<>();

        for (int i = 1; i < 10; i++) {
            nodeAddresses.add("10.128.0." + i);
        }
        //nodeAddresses.add("10.128.0.2");
        return nodeAddresses;
    }
}
