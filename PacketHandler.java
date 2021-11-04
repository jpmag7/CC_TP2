import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.*;
import java.util.Set;


/**
 * Escreva a descrição da classe PacketHandler aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class PacketHandler extends Thread
{
    private DatagramSocket socket;
    private DatagramPacket packet;
    private byte[] data;
    private int dataLength;
    private InetAddress address;
    private int port;
    
    public PacketHandler(DatagramSocket socket, DatagramPacket packet){
        this.socket = socket;
        this.packet = packet;
    }
    
    public void run(){
        this.data = packet.getData();
        this.dataLength = packet.getLength();
        this.address = packet.getAddress();
        this.port = packet.getPort();
        
        try{
            handlePacket();
        }catch(Exception e) {
            System.err.println(e);
        }
    }
    
    public void handlePacket() throws Exception{
        int id = verifyPacket(data);
        
        switch(id){
            case SystemInfo.REQUEST:
                sendFile();
                break;
            default:
                if (id > SystemInfo.REQUEST) // Received a response
                    receiveFile(id);
                else System.err.println("Corrupted packet");
        }
    }
    
    
    private int verifyPacket(byte[] packet){
        int hash = PacketUtil.byteArrayToInt(Arrays.copyOfRange(packet, 0, 4));
        byte[] dataAndID = Arrays.copyOfRange(packet, 4, dataLength);
        dataAndID = PacketUtil.crypt(dataAndID, hash);
        
        if(hash != Arrays.hashCode(dataAndID)) return SystemInfo.ERROR;
        
        int id = retriveInt(dataAndID); // Retrive id from data
        return id;
    }
    
    
    private int retriveInt(byte[] bytes){
        int num = PacketUtil.byteArrayToInt(Arrays.copyOfRange(bytes, 0, 4));
        this.data = Arrays.copyOfRange(bytes, 4, bytes.length);
        dataLength -= 4;
        return num;
    }
    
    
    private void sendFile() throws Exception{
        int file = retriveInt(data);
        int sequence = retriveInt(data);
        
        Lock l = SystemInfo.batchRequestedLock.get(address);
        l.lock();
        if(sequence < 0 && SystemInfo.batchesRequested.contains(sequence)){
            l.unlock();
            return;
        }
        else if(sequence < 0) 
            SystemInfo.batchesRequested.add(sequence);
        l.unlock();
        
        int batchSizeSend = retriveInt(data);
        String[] list = {"queijo.txt", "fiambre.txt", "ovos.micose"};
        int total = 100000; // put file/list size here
        
        if(sequence < 0){
            int start = (sequence * -1 - 1) * batchSizeSend;
            int end = Math.min(start + batchSizeSend, total);
            System.err.println("Request for batch: " + sequence * -1);
            for(int i = start; i < end; i++){
                byte[] byteList = list[i % list.length].getBytes();
                byte[] bytes = PacketUtil.makePacket(i, total, byteList);
                
                PacketUtil.send(socket, address, port, bytes, 0);
            }
            System.err.println("Batch " + start / batchSizeSend + " sent");
        }
        else{
            byte[] byteList = list[sequence % list.length].getBytes();
            byte[] bytes = PacketUtil.makePacket(sequence, total, byteList);
            
            PacketUtil.send(socket, address, port, bytes, 0);
        }
    }
    
    
    private void receiveFile(int file){ // The number of the file we are receiving
        int seq = retriveInt(data); // The sequence number of this packet
        int total = retriveInt(data); // The total number of packet of the original file
        int startBatch = (seq / SystemInfo.BatchSizeReceive); // Number of the current batch
        int start = startBatch * SystemInfo.BatchSizeReceive; // Sequence number of the first packet of the batch
        int end = Math.min(start + SystemInfo.BatchSizeReceive, total); // Sequence number of the last packet of the batch + 1
        
        Lock l = SystemInfo.fileRequestLock.get(address).get(file);
        l.lock();
        
        int lowestMissingSeq = SystemInfo.fileLowestMissing.get(address).get(file);
        Set<Integer> fileSeq = SystemInfo.fileSeq.get(address).get(file);
        Map<Integer, Long> fileTimers = SystemInfo.fileTimers.get(address);

        if(!fileSeq.contains(seq)){ // If we dont have already have this sequence
            fileSeq.add(seq); // Add sequence to list
            
            if (lowestMissingSeq < 0){ // If this seq/packet is the first of the batch to arrive
                System.out.println("Batch end: " + end);
                lowestMissingSeq = start; // Lowest missing is now first packet of the batch
                fileTimers.put(file, 0L); // Reset the request timer
            }
            
            if(lowestMissingSeq == seq){ // If this sequence is the lowest missing
                for(int i = lowestMissingSeq; i < end; i++) // Search for the next lowest missing packet
                    if(!fileSeq.contains(i)){
                        lowestMissingSeq = i;
                        break;
                    }
                    
                if(lowestMissingSeq == seq) // If lowestMissing still this seq then its over
                    lowestMissingSeq = end;
                    
                fileTimers.put(file, 0L); // Reset the request timer
            }
            
            if(lowestMissingSeq == total){ // We received the entire file             Handle entire file ending here ---------------------|+|
                System.err.println("Full file received");
                SystemInfo.fileLowestMissing.get(address).put(file, 0); // Break the request loop
                fileTimers.put(file, SystemInfo.BatchWaitTime + 1L); // Break the request timer
            }
            else if(lowestMissingSeq == end){ // We received the entire file batch    Handle batch ending here ---------------------------|+|
                System.err.println("Full batch " + startBatch + " received");
                SystemInfo.fileLowestMissing.get(address).put(file, (startBatch * -1) - 2); // Request for next batch
                fileTimers.put(file, SystemInfo.BatchWaitTime + 1L); // Break the request timer
            }
            else SystemInfo.fileLowestMissing.get(address).put(file, lowestMissingSeq); // Update the lowest missing packet
        }
        else {
            SystemInfo.RepetedPackets++;
            l.unlock();
            System.err.println("Repeted packet sequence: " + seq + " ><><><><><><><><><><><><><><><><><><><><><><><>< ");
            return; // We allready have this sequence
        }
        l.unlock();
        
        //Handle packet here -------------------------------------------------------------------------------------------------------------|+|
        String filePart = new String(data, 0, data.length);
        System.out.println("File part received: " + filePart + " file: " + file + " seq: " + seq + " total: " + total);
    }
}
