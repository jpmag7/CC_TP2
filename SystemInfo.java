import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.*;

/**
 * Escreva a descrição da classe Settings aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class SystemInfo
{
    public final static int PassHash = 123;
    
    public final static int HttpPort = 80;
    public final static int FTRapidPort = 80;
    public final static int PacketSize = 256;
    public final static int ResponseWaitTime = 200; // milliseconds
    public final static int SendWaitTime = 1; // nanoseconds
    
    // ID's
    public final static int REQUEST = -1;
    public final static int ERROR = -50;
    
    // Request control variables
    public static Map<Integer, Lock> fileRequestLock = new HashMap();
    public static Map<Integer, Integer> fileLowestMissing = new HashMap<>();
    public static Map<Integer, Long> fileTimers = new HashMap<>();
    public static Map<Integer, List<Integer>> fileSeq = new HashMap<>();
}
