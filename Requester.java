import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

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
    
    
    public Requester(DatagramSocket socket, InetAddress address, int port, int fileNum){
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.fileNum = fileNum;
        byte[] file = PacketUtil.intToBytes(fileNum);
        bytes = new byte[file.length + file.length];
        System.arraycopy(file, 0, bytes, 0, file.length);
    }
    
    
    public void run(){
        int lowestMissing = -2;
        // Set file timer to now
        SystemInfo.fileTimers.put(fileNum, System.currentTimeMillis());
        
        while((lowestMissing = SystemInfo.fileLowestMissing.get(fileNum)) > -2){
            // Make request
            System.out.println("Requesting: " + lowestMissing);
            request(lowestMissing);
            
            await();
        }
    }
    
    
    private void request(int lowestMissing){
        byte[] seq = PacketUtil.intToBytes(lowestMissing);
        System.arraycopy(seq, 0, bytes, 4, seq.length);
        
        try{
            PacketUtil.send(socket, address, port, bytes, SystemInfo.REQUEST);
        } catch(Exception e){
            System.err.println("Couldn't send request packet: " + e);
        }
    }
    
    
    private void await(){
        // Wait here for n milliseconds
        long dif;
        SystemInfo.fileRequestLock.get(fileNum).lock();
        while((dif = System.currentTimeMillis() - SystemInfo.fileTimers.get(fileNum)) <= SystemInfo.ResponseWaitTime){
            SystemInfo.fileRequestLock.get(fileNum).unlock();
            try{
                TimeUnit.MILLISECONDS.sleep(SystemInfo.ResponseWaitTime - dif);
            }catch (Exception e){
                System.err.println("Error on requester sleep: " + e);
            }
            SystemInfo.fileRequestLock.get(fileNum).lock();
        }
        SystemInfo.fileTimers.put(fileNum, System.currentTimeMillis());
        SystemInfo.fileRequestLock.get(fileNum).unlock();
    }
}
