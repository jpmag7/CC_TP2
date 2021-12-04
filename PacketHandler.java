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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Escreva a descrição da classe PacketHandler aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class PacketHandler extends Thread
{
    private DatagramPacket packet;
    private byte[] data;
    private InetAddress address;
    private String addString;
    private int port;
    public static Map<String, AtomicInteger> FYNReceived = new ConcurrentHashMap<>();//AtomicInteger FYNReceived = new AtomicInteger();
    private int startIndex = 0;
    private int endIndex = 0;
    
    public PacketHandler(DatagramPacket packet){
        this.packet = packet;
    }
    
    public void run(){
        this.data = packet.getData();
        endIndex = packet.getLength();
        this.address = packet.getAddress();
        this.port = Setup.findPort(address, packet.getPort());
        this.addString = "" + address + ":" + port;
        
        if(SystemInfo.startTime == 0L) SystemInfo.startTime = System.currentTimeMillis();
        
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
            case SystemInfo.FYN: // His done with me
                if(!FYNReceived.containsKey(addString)) FYNReceived.put(addString, new AtomicInteger());
                int currentFYN = FYNReceived.get(addString).incrementAndGet();//FYNReceived.incrementAndGet();
                Setup.log("Receiving FYN signal from: " + addString + " Sending FYN ACK");
                PacketUtil.send(addString, address, port, new byte[0], SystemInfo.FYNACK);
                waitForFYN(currentFYN);
                break;
            case SystemInfo.FYNACK: // I'm done with him
                Setup.log("Receiving FYN ACK signal from: " + addString);
                Lock l = SystemInfo.fileRequestLock.get(addString).get(SystemInfo.FYN);
                l.lock();
                SystemInfo.fileLowestMissing.get(addString).put(SystemInfo.FYN, null);
                SystemInfo.fileTimers.get(addString).put(SystemInfo.FYN, SystemInfo.BatchWaitTime + 1L);
                l.unlock();
                Listener.checkIfAllDone();
                break;
            default:
                if (id > SystemInfo.REQUEST) // Received a response
                    receiveResponse(id);
                else handleCorruption();
        }
    }
    
    
    private void handleCorruption(){
        Setup.log("Corrupted packet from: " + addString);
        SystemInfo.corruptedPackets.incrementAndGet();
        if((SystemInfo.transferedPackets.get() + SystemInfo.corruptedPackets.get()) / SystemInfo.their_lists.size() <= 1){
            System.out.println("Probable password mismatch");
            Listener.running = false;
            
            try{
                TimeUnit.MILLISECONDS.sleep(SystemInfo.BatchWaitTime * 3);
            }catch (Exception e){
                Setup.log("Error on Corruption wait: " + e);
            }
            
            Listener.stopAsking();
            
            for(DatagramSocket s : SystemInfo.mySockets) s.close();
        }
    }
    
    
    private void waitForFYN(int currentFYN){
        int time = SystemInfo.BatchWaitTime * 2;
        
        while(time > 0 && currentFYN == FYNReceived.get(addString).get()){//FYNReceived.get()){
            time--;
            try{
                TimeUnit.MILLISECONDS.sleep(1);
            }catch (Exception e){
                Setup.log("Error on FYN wait: " + e);
            }
        }
        
        if(time == 0 && currentFYN == FYNReceived.get(addString).get()){//FYNReceived.get()){
            SystemInfo.clientsDoneWithMe.add(addString);
            Listener.checkIfAllDone();
        }
    }
    
    
    private int verifyPacket(byte[] packet){
        if(endIndex == 0) return SystemInfo.ERROR;
        int hash = retriveInt(packet);
        byte[] dataAndID = Arrays.copyOfRange(packet, 4, endIndex);
        data = PacketUtil.crypt(dataAndID, hash);
        data = PacketUtil.crypt(packet, hash, startIndex, endIndex);
        
        if(hash != Arrays.hashCode(dataAndID)) return SystemInfo.ERROR;
        int id = retriveInt(data);
        return id;
    }
    
    
    private int retriveInt(byte[] bytes){
        int num = PacketUtil.byteArrayToInt(data, startIndex, startIndex + 4);
        startIndex = startIndex + 4;
        return num;
    }
    
    
    private long retriveLong(byte[] bytes){
        long num = PacketUtil.byteArrayToLong(data, startIndex, startIndex + 8);
        startIndex = startIndex + 8;
        return num;
    }
    
    
    private void receiveRequest() throws Exception{
        int file = retriveInt(data);
        long sequence = retriveLong(data);
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
    private void sendListBatch(long sequence, int batchSizeSend) throws Exception{
        int total = SystemInfo.m_list.length;
        int start = (int)((sequence * -1 - 1) * batchSizeSend);
        int end = Math.min(start + batchSizeSend, total);
        
        Setup.log("Received request for list batch " + (sequence * -1 - 1) + " from " + addString);
        
        for(int i = start; i < end; i++){
            byte[] byteList = (SystemInfo.m_list[i] + "§§" + SystemInfo.m_list_hash[i] + "§§" + SystemInfo.m_list_time.get(SystemInfo.m_list[i])).getBytes();//SystemInfo.m_list[i].getBytes();
            byte[] bytes = PacketUtil.makePacket(i, total, byteList);
            
            PacketUtil.send(addString, address, port, bytes, 0);
            Setup.log("Sending list packet " + i + " to " + addString);
        }
        if(start == end){
            byte[] byteList = new byte[0];
            byte[] bytes = PacketUtil.makePacket(-1, 1, byteList);
            
            Setup.log("Sending list EOF code packet to " + addString);
            PacketUtil.send(addString, address, port, bytes, 0);
        }
    }
    
    // Send File batch
    private void sendFileBatch(int file, long sequence, int batchSizeSend) throws Exception{
        long total = FileManager.filesSendSize.get(file);
        long start = (sequence * -1 - 1) * batchSizeSend;
        long end = Math.min(start + batchSizeSend, total);
        
        Setup.log("Received request for file " + file + " batch " + (sequence * -1 - 1) + " from " + addString);
        
        for(long i = start; i < end; i++){
            byte[] byteList = FileManager.readFile(file, i);
            byte[] bytes = PacketUtil.makePacket(i, total, byteList);
            
            PacketUtil.send(addString, address, port, bytes, file);
            Setup.log("Sending file " + file + " packet " + i + " to " + addString);
        }
        if(start == end){
            byte[] byteList = new byte[0];
            byte[] bytes = PacketUtil.makePacket(-1, 1, byteList);
            
            Setup.log("Sending file " + file + " EOF code packet to " + addString);
            PacketUtil.send(addString, address, port, bytes, file);
        }
    }
    
    // Send List packet
    private void sendListPacket(long sequence) throws Exception{
        Setup.log("Received request for list packet " + sequence + " from " + addString);
        int total = SystemInfo.m_list.length;
        byte[] byteList = (SystemInfo.m_list[(int)sequence] + "§§" + SystemInfo.m_list_hash[(int)sequence] + "§§" + SystemInfo.m_list_time.get(SystemInfo.m_list[(int)sequence])).getBytes();//SystemInfo.m_list[sequence].getBytes();
        byte[] bytes = PacketUtil.makePacket(sequence, total, byteList);
        
        PacketUtil.send(addString, address, port, bytes, 0);
        Setup.log("Sending list packet " + sequence + " to " + addString);
    }
    
    // Send File packet
    private void sendFilePacket(int file, long sequence) throws Exception{
        Setup.log("Received request for file " + file + " packet " + sequence + " from " + addString);
        long total = FileManager.filesSendSize.get(file);
        byte[] byteList = FileManager.readFile(file, sequence);
        byte[] bytes = PacketUtil.makePacket(sequence, total, byteList);
        
        PacketUtil.send(addString, address, port, bytes, file);
        Setup.log("Sending file " + file + " packet " + sequence + " to " + addString);
    }
    
    
    private void receiveResponse(int file) throws Exception{ // The number of the file we are receiving
        long seq = retriveLong(data); // The sequence number of this packet
        long total = retriveLong(data); // The total number of packet of the original file
        long startBatch = (seq / SystemInfo.BatchSizeReceive); // Number of the current batch
        long start = startBatch * SystemInfo.BatchSizeReceive; // Sequence number of the first packet of the batch
        long end = Math.min(start + SystemInfo.BatchSizeReceive, total); // Sequence number of the last packet of the batch + 1
        
        // Http stuff
        SystemInfo.transferedPackets.incrementAndGet();
        
        Lock l = SystemInfo.fileRequestLock.get(addString).get(file);
        l.lock();
        
        Long lowestMissingSeq = SystemInfo.fileLowestMissing.get(addString).get(file);
        Set<Long> fileSeq = SystemInfo.fileSeq.get(addString).get(file);
        Map<Integer, Long> fileTimers = SystemInfo.fileTimers.get(addString);
        
        
        if(seq < 0){
            Setup.log("EOF code " + (file == 0 ? "list received" : "file: " + file + " received") + " from: " + addString);
            SystemInfo.fileLowestMissing.get(addString).put(file, null); // Break the request loop
            fileTimers.put(file, SystemInfo.BatchWaitTime + 1L); // Break the request timer
        }
        else if(!fileSeq.contains(seq)){ // If we dont have already have this sequence
            fileSeq.add(seq); // Add sequence to list
            Setup.log("Received packet " + seq + (file == 0 ? " from list" : " from file " + file) + " of " + addString);
            
            if (lowestMissingSeq < 0){ // If this seq/packet is the first of the batch to arrive
                lowestMissingSeq = start; // Lowest missing is now first packet of the batch
                fileTimers.put(file, 0L); // Reset the request timer
            }
            
            if(lowestMissingSeq == seq){ // If this sequence is the lowest missing
                for(long i = lowestMissingSeq; i < end; i++) // Search for the next lowest missing packet
                    if(!fileSeq.contains(i)){
                        lowestMissingSeq = i;
                        break;
                    }
                    
                if(lowestMissingSeq == seq) // If lowestMissing still this seq then its over
                    lowestMissingSeq = end;
                    
                fileTimers.put(file, 0L); // Reset the request timer
            }
            
            if(lowestMissingSeq == total){ // We received the entire file
                Setup.log("Full " + (file == 0 ? "list received" : "file: " + file + " received") + " from: " + addString);
                SystemInfo.fileLowestMissing.get(addString).put(file, null); // Break the request loop
                fileTimers.put(file, SystemInfo.BatchWaitTime + 1L); // Break the request timer
            }
            else if(lowestMissingSeq == end){ // We received the entire file batch
                Setup.log("Full batch " + startBatch + " received from " + (file > 0 ? "file: " + file : "list") + " of: " + addString);
                SystemInfo.fileLowestMissing.get(addString).put(file, (startBatch * -1) - 2); // Request for next batch
                fileTimers.put(file, SystemInfo.BatchWaitTime + 1L); // Break the request timer
            }
            else SystemInfo.fileLowestMissing.get(addString).put(file, lowestMissingSeq); // Update the lowest missing packet
        }
        else {
            l.unlock();
            SystemInfo.repeatedPackets.incrementAndGet();
            Setup.log("Repeted packet sequence: " + seq + " from file: " + file + " of: " + addString);
            return; // We allready have this sequence
        }
            
        //Handling packet content
        if(file > 0) { // Write to file
            FileManager.writeFile(addString, file, seq, Arrays.copyOfRange(data, startIndex, endIndex));
            
            if(lowestMissingSeq == total || seq < 0) // We received the entire file
                FileManager.close(address, port, addString, file);
        }
        else { // Add to list
            if(seq >= 0) {
                String[] nameAndHashAndTime = new String(Arrays.copyOfRange(data, startIndex, endIndex), 0, endIndex - startIndex).split("§§", 3);
                SystemInfo.their_lists.get(addString).put((int)seq + 1, nameAndHashAndTime[0]);
                SystemInfo.their_lists_hash.get(addString).put((int)seq + 1, nameAndHashAndTime[1]);
                SystemInfo.their_lists_time.get(addString).put(nameAndHashAndTime[0], Long.parseLong(nameAndHashAndTime[2]));
            }
            
            if(lowestMissingSeq == total || seq < 0) // We received the entire list
                requestMissingFiles();
        }
        
        l.unlock();
    }
    
    private void requestMissingFiles(){
        boolean asked = false;
        Set<String> list = new HashSet<String>(Arrays.asList(SystemInfo.m_list));
        Set<String> list_hash = new HashSet<String>(Arrays.asList(SystemInfo.m_list_hash));
        Map<String, Long> list_time = SystemInfo.m_list_time;
        Map<Integer, String> his_hash_list = SystemInfo.their_lists_hash.get(addString);
        Map<String, Long> his_time_list = SystemInfo.their_lists_time.get(addString);
        
        for(Map.Entry<Integer, String> e : SystemInfo.their_lists.get(addString).entrySet()){
            if(!list.contains(e.getValue()) || // If I dont have a file with this name
              (!sameHash(e.getValue(), SystemInfo.m_list, SystemInfo.m_list_hash, his_hash_list.get(e.getKey())) &&//!list_hash.contains(his_hash_list.get(e.getKey())) &&
               his_time_list.get(e.getValue()) > list_time.get(e.getValue()))){ // If files have same name but are different ask for the most recent
                   
                String value = e.getValue();
                Integer key = e.getKey();
                
                asked = true;
                SystemInfo.filesRequested.add(value);
                FileManager.filesAsked.get(addString).incrementAndGet();
                Setup.setupForNewFile(addString, key);
                SystemInfo.fileTransferTime.get(addString).put(key, System.currentTimeMillis());
                Setup.log("Starting request of file: " + key + " name: " + value + " of: " + port + addString);
                
                new Requester(address, port, key, SystemInfo.REQUEST).start();
            }
            else Setup.log("File " + e.getKey() + " is in our list. file name: " + e.getValue() + " of: " + addString);
        }
        
        if(!asked){
            Setup.setupForNewFile(addString, SystemInfo.FYN);
            new Requester(address, port, SystemInfo.FYN, SystemInfo.FYN).start();
        }
    }
    
    private boolean sameHash(String fileName, String[] myList, String[] myHashList, String hisHash){
        int pos = -1;
        
        for(int i = 0; i < myHashList.length; i++){
            if(myList[i].equals(fileName)) {
                pos = i;
                break;
            }
        }
        
        if(myHashList[pos].equals(hisHash)) return true;
        return false;
    }
}
