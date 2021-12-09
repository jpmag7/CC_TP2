import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.io.RandomAccessFile;

/**
 * Escreva a descrição da classe FTRapid aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Listener extends Thread
{  
    private DatagramSocket socket;
    public static boolean running = true;
    private static boolean saiu = false;
    
    public Listener(DatagramSocket s){
        this.socket = s;
    }
    
    public void run(){
        try{
            Listen();
        }catch (SocketException e) {
        }
        catch(SocketTimeoutException e){
            //stopAsking();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private void Listen() throws Exception{
        while(running){
            byte[] buf = new byte[SystemInfo.PacketSize];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            
            new PacketHandler(packet).start();
        }
    }
    
    public static void checkIfAllDone(){
        int doneWith = 0;
        for(String a : SystemInfo.their_lists.keySet()){
            if(SystemInfo.fileLowestMissing.get(a).get(SystemInfo.FYN) == null) doneWith++;
        }
        
        if(saiu) return;
        if(SystemInfo.sendSockets.size() == 0 && SystemInfo.receSockets.size() == 0) {
            saiu = true;
            Long totalTime = 0L;
            for(Map<Integer, Long> e : SystemInfo.fileTransferTime.values()){
                for(Long l : e.values()) {
                    totalTime += l;
                }
            } 
            
            double transTime = (System.currentTimeMillis() - SystemInfo.startTime) / 1000d;
            System.out.println("Sync took: " + transTime + " seconds");
            Setup.log("Sync took: " + transTime + " seconds");
            
            double transSpeed = totalTime == 0 ? 0 : (SystemInfo.transferedPackets.get() * SystemInfo.PacketSize) / (totalTime / 1000d);
            System.out.println("Download speed: " + (transSpeed * 8) + " bits/second");
            Setup.log("Download speed: " + (transSpeed * 8) + " bits/second");
            
            for(DatagramSocket s : SystemInfo.sendSockets.values()) if(!s.isClosed()) s.close();
            if(!SystemInfo.mainSocket.isClosed()) SystemInfo.mainSocket.close();
            for(DatagramSocket s : SystemInfo.receSockets.values()) if(!s.isClosed()) s.close();
            try{
        	for(Map<Integer, RandomAccessFile> m : FileManager.filesReceive.values())
                    for(RandomAccessFile f : m.values()) f.close();
            }catch(Exception e) {}
        }
    }
    
    public static void stopAsking() {
        // Get all locks
        for(Map<Integer, Lock> m : SystemInfo.fileRequestLock.values())
            for(Lock l : m.values()) l.lock();
        
        // Stop all request loops
        for(Map<Integer, Long> m : SystemInfo.fileLowestMissing.values())
            m.replaceAll((k, v) -> v = null);
        
        // Release all locks
        for(Map<Integer, Lock> m : SystemInfo.fileRequestLock.values())
            for(Lock l : m.values()) l.unlock();
    }
}
