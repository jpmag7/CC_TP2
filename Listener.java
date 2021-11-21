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

/**
 * Escreva a descrição da classe FTRapid aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Listener extends Thread
{  
    public static DatagramSocket socket;
    public static boolean running = true;
    
    public Listener(DatagramSocket socket){
        this.socket = socket;
        
        try{
            socket.setReceiveBufferSize(SystemInfo.ReceiveBufferSize);
            //if(SystemInfo.socketTimeout > 0) socket.setSoTimeout(SystemInfo.socketTimeout);
        }catch(Exception e){
            Setup.log("Error setting receive buffer size: " + e);
        }
    }
    
    public void run(){
        try{
            Listen();
        }catch (SocketException e) {
        }
        catch(SocketTimeoutException e){
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
            
            new PacketHandler(socket, packet).start();
        }
    }
    
    public static void checkIfAllDone(){
        int doneWith = 0;
        for(String a : SystemInfo.their_lists.keySet()){
            if(SystemInfo.fileLowestMissing.get(a).get(SystemInfo.FYN) == null) doneWith++;
        }
        
        if(SystemInfo.their_lists.size() == doneWith &&
           SystemInfo.their_lists.size() == SystemInfo.clientsDoneWithMe.size()) {
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
            System.out.println("Download speed: " + transSpeed + " bytes/second");
            Setup.log("Download speed: " + transSpeed + " bytes/second");
            
            socket.close();
        }
    }
}
