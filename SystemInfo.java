import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.*;
import java.net.InetAddress;
import java.util.Set;
import java.util.HashSet;
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
    public final static int ReceiveBufferSize = 65536 * 4;
    public final static int socketTimeout = 10000;
    
    // Sync control variables
    public static String folder;
    public static String[] m_list;
    public static Map<InetAddress, Map<Integer, String>> their_lists = new HashMap<>();
    public static Set<InetAddress> doneClients = ConcurrentHashMap.newKeySet(); 
    
    // Traffic control variables
    public static int Redundancy = 1;
    public static int BatchSizeReceive;
    public static long RepetedPackets = 0;
    
    // ID's
    public final static int REQUEST = -1;
    public final static int SHUTDOWN = -2;
    public final static int SHUTDOWNACK = -3;
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
}
