import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.*;
import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Escreva a descrição da classe Settings aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class SystemInfo
{
    // Socket setup variables
    public static int FTRapidPort = 80;
    public final static int PacketSize = 512;
    public final static int ReceiveBufferSize = 65536;// * 4;
    public final static int socketTimeout = 10000;
    
    // Sync control variables
    public static String folder;
    public static String[] m_list;
    public static String[] m_list_hash;
    public static Map<String, Long> m_list_time = new HashMap<>();
    public static Map<InetAddress, Map<Integer, String>> their_lists = new HashMap<>();
    public static Map<InetAddress, Map<Integer, String>> their_lists_hash = new HashMap<>();
    public static Map<InetAddress, Map<String, Long>> their_lists_time = new HashMap<>();
    public static Set<InetAddress> doneClients = ConcurrentHashMap.newKeySet(); 
    
    // Traffic control variables
    public static int Redundancy = 1;
    public static int BatchSizeReceive;
    public static long RepetedPackets = 0;
    
    // Performance control variables
    public static AtomicLong transferedPackets = new AtomicLong();
    public static AtomicLong sendedPackets = new AtomicLong();
    public static AtomicLong repeatedPackets = new AtomicLong();
    public static AtomicLong corruptedPackets = new AtomicLong();
    
    // ID's
    public final static int REQUEST = -1;
    public final static int FYN = -2;
    public final static int FYNACK = -3;
    public final static int ERROR = -50;
    
    // Security
    public static int PassHash = 123;
    
    // Request control variables
    public final static int BatchWaitTime = 200; // milliseconds
    public final static int PacketWaitTime = 10; // milliseconds
    public static Map<InetAddress, Map<Integer, Long>> fileTimers = new HashMap<>();
    public static Map<InetAddress, Map<Integer, Lock>> fileRequestLock = new HashMap();
    public static Map<InetAddress, Map<Integer, Set<Integer>>> fileSeq = new HashMap<>();
    public static Map<InetAddress, Map<Integer, Integer>> fileLowestMissing = new HashMap<>();
    public static List<String> filesRequested = new ArrayList<>();
    public static List<String> filesReceived = new ArrayList<>();
}
