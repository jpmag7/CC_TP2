import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.io.RandomAccessFile;
import java.io.File;

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
    
    
    public Requester(DatagramSocket socket, InetAddress address, int port, int fileNum, int id){
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.fileNum = fileNum;
        this.id = id;
        timers = SystemInfo.fileTimers.get(address);
        missingList = SystemInfo.fileLowestMissing.get(address);
        byte[] file = PacketUtil.intToBytes(fileNum);
        receiveBufferSize = PacketUtil.intToBytes(SystemInfo.BatchSizeReceive);
        bytes = new byte[file.length + file.length + receiveBufferSize.length];
        System.arraycopy(file, 0, bytes, 0, file.length);
    }
    
    
    public void run(){
        int lowestMissing = 0;
        // Set file timer to now
        timers.put(fileNum, 0L);
        
        try{
            // Add fileOutputStream to fileManager
            if(fileNum > 0) addFileToFileManager();
        }
        catch (Exception e){e.printStackTrace();}
        
        while((lowestMissing = missingList.get(fileNum)) != 0){
            // Make request
            System.err.println("Requesting file: " + fileNum + " sequence number: " + lowestMissing);
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
            System.err.println("Couldn't send request packet: " + e);
        }
    }
    
    
    private void await(){ // Wait here for n milliseconds
        SystemInfo.fileRequestLock.get(address).get(fileNum).lock();
        
        while(timers.get(fileNum) <
            (missingList.get(fileNum) < 0 ? SystemInfo.BatchWaitTime : SystemInfo.PacketWaitTime)){
            timers.put(fileNum, timers.get(fileNum) + 1);
            //System.out.println("Timer: " + timers.get(fileNum) + " waiting for: " + missingList.get(fileNum));
            SystemInfo.fileRequestLock.get(address).get(fileNum).unlock();
            
            try{
                TimeUnit.MILLISECONDS.sleep(1);
            }catch (Exception e){
                System.err.println("Error on requester wait: " + e);
            }
            
            SystemInfo.fileRequestLock.get(address).get(fileNum).lock();
        }
        // Reset timer
        timers.put(fileNum, 0L);
        SystemInfo.fileRequestLock.get(address).get(fileNum).unlock();
    }
    
    
    private void addFileToFileManager() throws Exception{
        if(!FileManager.filesReceive.containsKey(address))
            FileManager.filesReceive.put(address, new ConcurrentHashMap<>());
        
        if(!FileManager.filesReceive.get(address).containsKey(fileNum))
            FileManager.filesReceive.get(address).put(fileNum, 
            new RandomAccessFile(
            new File(SystemInfo.folder + "\\\\" + SystemInfo.their_lists.get(address).get(fileNum)),
            "rw"));
    }
}
