import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.*;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.ClientConfiguration;


public class Main {

    static String clusterIp = "127.0.0.1:10800";
    static String resultsCSV;
    static String throughputSnapshots;
    static int lowerBound;
    static int higherBound;
    static int minutesPerTest;
    static int timeToWarmUpCache;
    static ClientCache cache;
    static int total;
    static int nodes;

    static long retryFrequency = 5000; //after how many ms connecting to the cluster is reattempted

    public static void main(String[] args) {
        System.out.println("\n #####################################\n");
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        long client_start = System.currentTimeMillis();


        lowerBound = Integer.parseInt(args[0]); //min number of bytes for random String
        higherBound = Integer.parseInt(args[1]); //max number of bytes for random String

        minutesPerTest = Integer.parseInt(args[2]);
        timeToWarmUpCache = Integer.parseInt(args[3]);


        nodes = Integer.parseInt(args[4]);
        resultsCSV = args[5] + "/results.csv";
        throughputSnapshots = args[5] + "/throughput_snapshots/snapshot_";

        System.out.println(format.format(new Date(System.currentTimeMillis())) + "   Client started");
        System.out.println("nodes:" + nodes + " minutesPerTest:" + minutesPerTest + " lowerBound:" + lowerBound + " higherBound:" + higherBound);

        ClientConfiguration cfg = new ClientConfiguration().setAddresses(clusterIp);
        cfg.setRetryLimit(1);

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

                client = Ignition.startClient(cfg);
                Collection<ClusterNode> c = client.cluster().nodes();

                if (c.size() < nodes) continue; //not all nodes have joined the cluster yet

                long afterStart = System.currentTimeMillis();
                System.out.println("All nodes joining cluster took: " + ((afterStart - client_start) / 1000) + " seconds");

                cache = client.createCache("MyCache");
                
                System.out.println("Cache created");
                //perform benchmarks and append the results
                doBenchmarks();
                break;

            } catch (Exception e) {
                //just repeat until connection is possible
            }
        }

        //clean up
        cache.removeAll();
        while (cache.size() != 0) {
        }
        client.destroyCache("MyCache");
        client.close();
        System.out.println("destroyed cache and stopped client-node");
        System.exit(0);
    }

    public static void doBenchmarks() {

        warm_up_cache();



        long startTime = System.currentTimeMillis();
        long stopTime = startTime + minutesPerTest * 60 * 1000;

        System.out.println("starting the measurements: "+ new SimpleDateFormat().format(startTime)+"   stopping at: "+ new SimpleDateFormat().format(stopTime));

        //counters
        long totalBytesSend = 0;
        int operations = 0;
        int index = 0;

        Map<Long,Long> throughputSnapshots = new HashMap<>();

        long previousTime = startTime;
        long previousTotalBytes = 0;


        while (System.currentTimeMillis() < stopTime) {

            //take snapshots of throughput (approx every second)
            if ((System.currentTimeMillis() - previousTime) / 1000 > 1) {
                long currentThroughput =(totalBytesSend - previousTotalBytes)/(System.currentTimeMillis()-previousTime) * 1000;
                throughputSnapshots.put((System.currentTimeMillis() - startTime) / 1000, currentThroughput);
                previousTime = System.currentTimeMillis();
                previousTotalBytes = totalBytesSend;
            }

            //create random string-value of byte size in bounds
            int bytes = (int) ((Math.random() * (higherBound - lowerBound)) + lowerBound);
            String randomString = randomString(bytes);

            if (Math.random() > 0.5d) {
                cache.put(index++, randomString);
                continue;
            }else{
                String retrievedVal = (String) cache.get(Math.floor( operations / 2 ));
            }

            //counters for calculating throughput
            totalBytesSend += bytes;
            operations++;
        }

        long throughput = (1000 * totalBytesSend / (System.currentTimeMillis() - startTime) );
        System.out.println("stopped at: "+ new SimpleDateFormat().format(new Date(System.currentTimeMillis())) );
        System.out.println("throughput: " + throughput);
        System.out.println("operations: " + operations);

        String result = nodes  + "," + minutesPerTest + "," + throughput+","+operations;
        writeToCSV(result, throughputSnapshots);
    }

    public static void warm_up_cache(){
        System.out.println("Begin warming up the cache");
        long start = System.currentTimeMillis();

        int offset = -1000000;
        int index = 0;
        int operations = 0;

        while (System.currentTimeMillis() < start + 5 * 60 * 1000){

            int bytes = (int) ((Math.random() * (higherBound - lowerBound)) + lowerBound);
            String randomString = randomString(bytes);

            if (Math.random() > 0.5d) {
                index++;
                cache.put( offset + index, randomString);
                continue;
            }else{
                String retrievedVal = (String) cache.get(Math.floor((operations) / 2) + offset);
            }
            operations++;
        }

    }


    public static String randomString(int length) {

        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[length];
        random.nextBytes(r);
        String s = new String(r);


        return s;
    }

    public static void writeToCSV(String resultString, Map<Long,Long> snapshots) {
        try {
            File f = new File(resultsCSV);

            String toBeWritten = "";

            if (!f.exists()) {
                toBeWritten += "nodes,time,throughput,operations";
            }
            toBeWritten += "\n" + resultString;
            PrintWriter output = new PrintWriter(new FileWriter(f, true));
            output.write(toBeWritten);
            output.close();

            toBeWritten = "";
            f = new File(throughputSnapshots  + nodes + ".csv");
            if (!f.exists()) {
                toBeWritten += "second,throughput";
            }
            for (long l : snapshots.keySet()) {
                long throughput = snapshots.get(l);
                toBeWritten += "\n"+l+","+throughput;
            }
            output = new PrintWriter(new FileWriter(f, false));
            output.write(toBeWritten);
            output.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
