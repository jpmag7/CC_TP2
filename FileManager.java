import java.util.Map;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.net.InetAddress;
import java.util.concurrent.locks.*;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 * Escreva a descrição da classe FileManager aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FileManager
{
    public static int payloadSize = SystemInfo.PacketSize - 24;
    // Send
    public static Map<Integer, Integer> filesSendSize = new HashMap<>();
    public static Map<Integer, FileInputStream> filesSend = new ConcurrentHashMap<>();
    
    // Receive
    public static Map<InetAddress, Map<Integer, RandomAccessFile>> filesReceive = new ConcurrentHashMap<>();
    
    public static byte[] readFile(int file, int sequence) throws Exception{
        FileInputStream f = filesSend.get(file);
        f.getChannel().position(sequence * payloadSize);
        byte[] bytes = new byte[payloadSize];
        int size = f.read(bytes);
        return size > 0 ? Arrays.copyOf(bytes, size) : new byte[0];
    }
    
    public static void writeFile(InetAddress address, int file, int sequence, byte[] bytes) throws Exception{
        RandomAccessFile raf = filesReceive.get(address).get(file);
        System.out.println("Writing file: " + file + " from address: " + address);
        raf.seek(sequence * payloadSize);
        raf.write(bytes);
    }
    
    public static void close(InetAddress address, int file) throws Exception{
        System.out.println("Closing file: " + file);
        filesReceive.get(address).get(file).close();
    }
}
