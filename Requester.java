import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.io.RandomAccessFile;
import java.io.File;
import java.nio.file.Paths;

/**
 * Escreva a descrição da classe Requester aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Requester extends Thread
{
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] bytes;
    private int port;
    private int fileNum;
    private int id;
    private byte[] receiveBufferSize;
    private Map<Integer, Long> timers;
    private Map<Integer, Integer> missingList;
    private String addString;
    
    
    public Requester(DatagramSocket socket, InetAddress address, int port, int fileNum, int id){
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.fileNum = fileNum;
        this.id = id;
        this.addString = "" + address.toString().substring(address.toString().indexOf("/")) + ":" + port;
        timers = SystemInfo.fileTimers.get(addString);
        missingList = SystemInfo.fileLowestMissing.get(addString);
        byte[] file = PacketUtil.intToBytes(fileNum);
        receiveBufferSize = PacketUtil.intToBytes(SystemInfo.BatchSizeReceive);
        bytes = new byte[file.length + file.length + receiveBufferSize.length];
        System.arraycopy(file, 0, bytes, 0, file.length);
    }
    
    
    public void run(){
        Integer lowestMissing = null;
        // Set file timer to now
        timers.put(fileNum, 0L);
        
        try{
            // Add fileOutputStream to fileManager
            if(fileNum > 0) addFileToFileManager();
        }
        catch (Exception e){e.printStackTrace();}
        
        while((lowestMissing = missingList.get(fileNum)) != null){
            // Make request
            Setup.log("Requesting " + (fileNum == 0 ? "list" :
                      (fileNum == SystemInfo.FYN ? " FYN ACK" : "file " + fileNum)) 
                      + (lowestMissing < 0 ? 
                      (fileNum == SystemInfo.FYN ? "" : ", batch " + (lowestMissing * -1 - 1)) :
                      ", sequence " + lowestMissing)
                      + " to " + addString);
            SystemInfo.sendedPackets.incrementAndGet();
            request(lowestMissing);
            
            await();
        }
    }
    
    
    private void request(int lowestMissing){
        byte[] seq = PacketUtil.intToBytes(lowestMissing);
        System.arraycopy(seq, 0, bytes, 4, seq.length);
        System.arraycopy(receiveBufferSize, 0, bytes, 8, receiveBufferSize.length);
        
        try{
            PacketUtil.send(socket, address, port, bytes, id);
        } catch(Exception e){
            Setup.log("Couldn't send request packet: " + e);
        }
    }
    
    
    private void await(){ // Wait here for n milliseconds
        SystemInfo.fileRequestLock.get(addString).get(fileNum).lock();
        
        while(missingList.get(fileNum) != null && timers.get(fileNum) <
            (missingList.get(fileNum) < 0 ? SystemInfo.BatchWaitTime : SystemInfo.PacketWaitTime)){
            timers.put(fileNum, timers.get(fileNum) + 1);
            //Setup.log("Timer: " + timers.get(fileNum) + " waiting for: " + missingList.get(fileNum));
            SystemInfo.fileRequestLock.get(addString).get(fileNum).unlock();
            
            try{
                TimeUnit.MILLISECONDS.sleep(1);
            }catch (Exception e){
                Setup.log("Error on requester wait: " + e);
            }
            
            SystemInfo.fileRequestLock.get(addString).get(fileNum).lock();
        }
        // Reset timer
        timers.put(fileNum, 0L);
        SystemInfo.fileRequestLock.get(addString).get(fileNum).unlock();
    }
    
    
    private void addFileToFileManager() throws Exception{
        if(!FileManager.filesReceive.containsKey(addString))
            FileManager.filesReceive.put(addString, new ConcurrentHashMap<>());
        
        if(!FileManager.filesReceive.get(addString).containsKey(fileNum)){
            File f = new File(Paths.get(SystemInfo.folder, SystemInfo.their_lists.get(addString).get(fileNum)).toString());
            f.getParentFile().mkdirs();
            FileManager.filesReceive.get(addString).put(fileNum, 
            new RandomAccessFile(
            f,
            "rw"));
            FileManager.filesReceive.get(addString).get(fileNum).setLength(0);
        }
    }
}
