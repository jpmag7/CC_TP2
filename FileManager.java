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
import java.net.DatagramSocket;

/**
 * Escreva a descrição da classe FileManager aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FileManager
{
    public static int payloadSize = SystemInfo.PacketSize - 16;
    public static Map<String, AtomicInteger> filesAsked = new ConcurrentHashMap<>();
    public static Map<String, AtomicInteger> filesReceived = new ConcurrentHashMap<>();
    
    // Send
    public static Map<Integer, Long> filesSendSize = new ConcurrentHashMap<>();
    public static Map<Integer, FileInputStream> filesSend = new ConcurrentHashMap<>();
    
    // Receive
    public static Map<String, Map<Integer, RandomAccessFile>> filesReceive = new ConcurrentHashMap<>();
    
    public static byte[] readFile(int file, long sequence) throws Exception{
        FileInputStream f = filesSend.get(file);
        f.getChannel().position(sequence * payloadSize);
        byte[] bytes = new byte[payloadSize];
        int size = f.read(bytes);
        return size > 0 ? Arrays.copyOf(bytes, size) : new byte[0];
    }
    
    public static void writeFile(String address, int file, long sequence, byte[] bytes) throws Exception{
        RandomAccessFile raf = filesReceive.get(address).get(file);
        raf.seek(sequence * payloadSize);
        raf.write(bytes);
    }
    
    public static void close(InetAddress address, int port, String addString, int file) throws Exception{
        Setup.log("Closing file: " + file);
        SystemInfo.filesReceived.add(SystemInfo.their_lists.get(addString).get(file));
        filesReceive.get(addString).get(file).close();
        SystemInfo.fileTransferTime.get(addString).put(file, System.currentTimeMillis() - SystemInfo.fileTransferTime.get(addString).get(file));
        Setup.log("Number files received: " + filesReceived.get(addString).incrementAndGet() + "/" + filesAsked.get(addString).get());
        
        if(FileManager.filesReceived.get(addString).get() == FileManager.filesAsked.get(addString).get()){
            Setup.setupForNewFile(addString, SystemInfo.FYN);
            new Requester(SystemInfo.socket, address, Setup.findPort(address, port), SystemInfo.FYN, SystemInfo.FYN).start();
        }
        
        Listener.checkIfAllDone();
    }
    
    
    public static void close(InetAddress address, int port, String addString, int file, boolean isFolder) throws Exception{
        Setup.log("Closing file: " + file);
        SystemInfo.filesReceived.add(SystemInfo.their_lists.get(addString).get(file));
        //filesReceive.get(addString).get(file).close();
        SystemInfo.fileTransferTime.get(addString).put(file, System.currentTimeMillis() - SystemInfo.fileTransferTime.get(addString).get(file));
        Setup.log("Number files received: " + filesReceived.get(addString).incrementAndGet() + "/" + filesAsked.get(addString).get());
        
        if(FileManager.filesReceived.get(addString).get() == FileManager.filesAsked.get(addString).get()){
            Setup.setupForNewFile(addString, SystemInfo.FYN);
            new Requester(SystemInfo.socket, address, Setup.findPort(address, port), SystemInfo.FYN, SystemInfo.FYN).start();
        }
        
        Listener.checkIfAllDone();
    }
    
    
    public static String getFileChecksum(MessageDigest digest, File file) throws Exception{
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);
         
        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0; 
          
        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
         
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
