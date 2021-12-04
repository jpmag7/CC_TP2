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
import java.net.DatagramSocket;

/**
 * Escreva a descrição da classe Settings aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class SystemInfo
{
    // Socket setup variables
    public static List<DatagramSocket> mySockets = new ArrayList<>();
    public static int FTRapidPort = 80;
    public final static int socketNumber = 35;
    public final static int DefaultFTRapidPort = 80;
    public final static int PacketSize = 512;
    public final static int ReceiveBufferSize = 65536;
    public final static int SendBufferSize = 65536 * 4;
    public final static int socketTimeout = 60000 * 4;
    
    // Sync control variables
    public static String folder;
    public static String[] m_list;
    public static String[] m_list_hash;
    public static Map<String, Long> m_list_time = new HashMap<>();
    public static Map<String, Map<Integer, String>> their_lists = new HashMap<>();
    public static Map<String, Map<Integer, String>> their_lists_hash = new HashMap<>();
    public static Map<String, Map<String, Long>> their_lists_time = new HashMap<>();
    public static Set<String> clientsDoneWithMe = ConcurrentHashMap.newKeySet();
    
    // Traffic control variables
    public static int Redundancy = 1;
    public static int BatchSizeReceive = -1;
    
    // Performance control variables
    public static Long startTime = 0L;
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
    public final static int PacketWaitTime = 30; // milliseconds
    public static Map<String, Map<Integer, Long>> fileTimers = new HashMap<>();
    public static Map<String, Map<Integer, Lock>> fileRequestLock = new HashMap();
    public static Map<String, Map<Integer, Set<Long>>> fileSeq = new HashMap<>();
    public static Map<String, Map<Integer, Long>> fileLowestMissing = new HashMap<>();
    public static Map<String, Map<Integer, Long>> fileTransferTime = new HashMap<>();
    public static Set<String> filesRequested = new HashSet<>();
    public static Set<String> filesReceived = new HashSet<>();
}
