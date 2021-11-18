import java.util.Map;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.net.InetAddress;
import java.util.concurrent.locks.*;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.MessageDigest;
import java.io.File;

/**
 * Escreva a descrição da classe FileManager aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FileManager
{
    public static int payloadSize = SystemInfo.PacketSize - 24;
    public static AtomicInteger filesAsked = new AtomicInteger();
    public static AtomicInteger filesReceived = new AtomicInteger();
    
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
        raf.seek(sequence * payloadSize);
        raf.write(bytes);
    }
    
    public static void close(InetAddress address, int port, int file) throws Exception{
        Setup.log("Closing file: " + file);
        SystemInfo.filesReceived.add(SystemInfo.their_lists.get(address).get(file));
        filesReceive.get(address).get(file).close();
        Setup.log("Number files received: " + filesReceived.incrementAndGet());
        
        if(FileManager.filesReceived.get() == FileManager.filesAsked.get()){
            Setup.setupForNewFile(address, SystemInfo.FYN);
            FileManager.filesAsked.incrementAndGet();
            new Requester(Listener.socket, address, port, SystemInfo.FYN, SystemInfo.FYN).start();
        }
        
        Listener.checkIfAllDone();
    }
    
    
    public static String getFileChecksum(MessageDigest digest, File file) throws Exception
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);
         
        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0; 
          
        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
         
        //close the stream; We don't need it now.
        fis.close();
         
        //Get the hash's bytes
        byte[] bytes = digest.digest();
         
        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        //return complete hash
       return sb.toString();
    }
}
