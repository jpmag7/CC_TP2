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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;


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
    private InetAddress address;
    private int port;
    
    private int startIndex = 0;
    private int endIndex = 0;
    
    public PacketHandler(DatagramSocket socket, DatagramPacket packet){
        this.socket = socket;
        this.packet = packet;
    }
    
    public void run(){
        this.data = packet.getData();
        endIndex = packet.getLength();
        this.address = packet.getAddress();
        this.port = packet.getPort();
        
        try{
            handlePacket();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void handlePacket() throws Exception{
        int id = verifyPacket(data);
        
        switch(id){
            case SystemInfo.REQUEST:
                receiveRequest();
                break;
            default:
                if (id > SystemInfo.REQUEST) // Received a response
                    receiveResponse(id);
                else System.err.println("Corrupted packet");
        }
    }
    
    
    private int verifyPacket(byte[] packet){
        //int hash = PacketUtil.byteArrayToInt(Arrays.copyOfRange(packet, 0, 4));
        int hash = retriveInt(packet);
        byte[] dataAndID = Arrays.copyOfRange(packet, 4, endIndex);
        data = PacketUtil.crypt(dataAndID, hash);
        data = PacketUtil.crypt(packet, hash, startIndex, endIndex);
        
        if(hash != Arrays.hashCode(dataAndID)) return SystemInfo.ERROR;
        //if(hash != Arrays.hashCode(Arrays.copyOfRange(packet, startIndex, endIndex))) return SystemInfo.ERROR;
        //System.out.println("Got here");
        //int id = retriveInt(dataAndID); // Retrive id from data
        int id = retriveInt(data);
        return id;
    }
    
    
    private int retriveInt(byte[] bytes){
        //int num = PacketUtil.byteArrayToInt(Arrays.copyOfRange(bytes, 0, 4));
        int num = PacketUtil.byteArrayToInt(data, startIndex, startIndex + 4);
        //this.data = Arrays.copyOfRange(bytes, 4, bytes.length);
        startIndex = startIndex + 4;
        return num;
    }
    
    
    private void receiveRequest() throws Exception{
        int file = retriveInt(data);
        int sequence = retriveInt(data);
        int batchSizeSend = retriveInt(data);
        
        if(sequence < 0){ //Send one batch
            if(file == 0) sendListBatch(sequence, batchSizeSend);
            else sendFileBatch(file, sequence, batchSizeSend);
        }
        else{ // Send one packet
            if(file == 0) sendListPacket(sequence);
            else sendFilePacket(file, sequence);
        }
    }
    
    // Send List batch
    private void sendListBatch(int sequence, int batchSizeSend) throws Exception{
        Lock l = SystemInfo.batchRequestedLock.get(address);
        l.lock();
        if(sequence < 0 && SystemInfo.batchesRequested.contains(sequence)){
            l.unlock();
            return;
        }
        else if(sequence < 0) 
            SystemInfo.batchesRequested.add(sequence);
        l.unlock();
        
        int total = SystemInfo.m_list.length;
        int start = (sequence * -1 - 1) * batchSizeSend;
        int end = Math.min(start + batchSizeSend, total);
        
        for(int i = start; i < end; i++){
            byte[] byteList = SystemInfo.m_list[i].getBytes();
            byte[] bytes = PacketUtil.makePacket(i, total, byteList);
            
            PacketUtil.send(socket, address, port, bytes, 0);
        }
        if(start == end){
            byte[] byteList = new byte[0];
            byte[] bytes = PacketUtil.makePacket(-1, 1, byteList);
            
            PacketUtil.send(socket, address, port, bytes, 0);
        }
    }
    
    // Send File batch
    private void sendFileBatch(int file, int sequence, int batchSizeSend) throws Exception{
        int total = FileManager.filesSendSize.get(file);
        int start = (sequence * -1 - 1) * batchSizeSend;
        int end = Math.min(start + batchSizeSend, total);
        
        for(int i = start; i < end; i++){
            byte[] byteList = FileManager.readFile(file, i);
            byte[] bytes = PacketUtil.makePacket(i, total, byteList);
            
            PacketUtil.send(socket, address, port, bytes, file);
        }
        if(start == end){
            byte[] byteList = new byte[0];
            byte[] bytes = PacketUtil.makePacket(-1, 1, byteList);
            
            PacketUtil.send(socket, address, port, bytes, file);
        }
    }
    
    // Send List packet
    private void sendListPacket(int sequence) throws Exception{
        int total = SystemInfo.m_list.length;
        byte[] byteList = SystemInfo.m_list[sequence].getBytes();
        byte[] bytes = PacketUtil.makePacket(sequence, total, byteList);
        
        PacketUtil.send(socket, address, port, bytes, 0);
    }
    
    // Send File packet
    private void sendFilePacket(int file, int sequence) throws Exception{
        int total = FileManager.filesSendSize.get(file);
        byte[] byteList = FileManager.readFile(file, sequence);
        byte[] bytes = PacketUtil.makePacket(sequence, total, byteList);
        
        PacketUtil.send(socket, address, port, bytes, 0);
    }
    
    
    private void receiveResponse(int file) throws Exception{ // The number of the file we are receiving
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
        
        if(file == 0) SystemInfo.their_lists_lock.lock();
        
        if(seq < 0){
            SystemInfo.fileLowestMissing.get(address).put(file, 0);
            fileTimers.put(file, SystemInfo.BatchWaitTime + 1L);
            l.unlock();
            return;
        }

        if(!fileSeq.contains(seq)){ // If we dont have already have this sequence
            fileSeq.add(seq); // Add sequence to list
            
            if (lowestMissingSeq < 0){ // If this seq/packet is the first of the batch to arrive
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
            System.out.println("Repeted packet sequence: " + seq + " ><><><><><><><><><><><><><><><><><><><><><><><>< ");
            return; // We allready have this sequence
        }
        
            
        //Handle packet here -------------------------------------------------------------------------------------------------------------|+|
        String filePart = new String(Arrays.copyOfRange(data, startIndex, endIndex), 0, endIndex - startIndex);
        System.out.println("File part received: " + filePart + " end: " + end + " start: " + start);//" file: " + file + " seq: " + seq + " total: " + total);
        if(file > 0) {
            FileManager.writeFile(address, file, seq, Arrays.copyOfRange(data, startIndex, endIndex));
            
            if(lowestMissingSeq == total)
                FileManager.close(address, file);
        }
        else { // Add to list
            
            if(!SystemInfo.their_lists.containsKey(address))
                SystemInfo.their_lists.put(address, new HashMap<>());
            SystemInfo.their_lists.get(address).put(seq + 1, new String(Arrays.copyOfRange(data, startIndex, endIndex), 0, endIndex - startIndex));
            
            if(lowestMissingSeq == total)
                requestMissingFiles();
            SystemInfo.their_lists_lock.unlock();
        }
        l.unlock();
    }
    
    private void requestMissingFiles(){
        Set<String> list = new HashSet<String>(Arrays.asList(SystemInfo.m_list));
        for(Map.Entry<InetAddress, Map<Integer, String>> tl : SystemInfo.their_lists.entrySet()){
            for(Map.Entry<Integer, String> e : tl.getValue().entrySet()){
                if(!list.contains(e.getValue())){
                    System.err.println("Starting request of file: " + e.getKey() + " name: " + e.getValue());
                    Setup.setupForNewFile(address, e.getKey());
                    new Requester(socket, tl.getKey(), SystemInfo.FTRapidPort, e.getKey()).start();
                }
                else System.out.println("This is in our list: " + e.getValue() + " number: " + e.getKey());
            }
        }
    }
}
