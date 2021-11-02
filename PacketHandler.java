import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Escreva a descrição da classe PacketHandler aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class PacketHandler extends Thread
{
    private DatagramSocket socket;
    private byte[] data;
    private int dataLength;
    private InetAddress address;
    private int port;
    
    public PacketHandler(DatagramSocket socket, DatagramPacket packet){
        this.socket = socket;
        this.data = packet.getData();
        this.dataLength = packet.getLength();
        this.address = packet.getAddress();
        this.port = packet.getPort();
    }
    
    public void run(){
        try{
            handlePacket();
        }catch(Exception e) {
            System.err.println(e);
        }
    }
    
    public void handlePacket() throws Exception{
        int id = verifyPacket(data);
        
        if (id > SystemInfo.REQUEST) // Received a response
            receiveFile(id);
        else if(id == SystemInfo.REQUEST) // Received a request
            sendFile();
    }
    
    private int verifyPacket(byte[] packet){
        int hash = PacketUtil.byteArrayToInt(Arrays.copyOfRange(packet, 0, 4));
        byte[] dataAndID = Arrays.copyOfRange(packet, 4, dataLength);
        dataAndID = PacketUtil.crypt(dataAndID, hash);
        
        if(hash != Arrays.hashCode(dataAndID)) return SystemInfo.ERROR;
        
        int id = retriveNumber(dataAndID); // Retrive id from data
        return id;
    }
    
    private int retriveNumber(byte[] bytes){
        int num = PacketUtil.byteArrayToInt(Arrays.copyOfRange(bytes, 0, 4));
        this.data = Arrays.copyOfRange(bytes, 4, bytes.length);
        return num;
    }
    
    //* Packet Handlers *//
    private void sendFile() throws Exception{
        int file = retriveNumber(data);
        int sequence = retriveNumber(data);
        String[] list = {"queijo.txt", "fiambre.txt", "ovos.micose"};
        int iterations = 10000;
        byte[] total = PacketUtil.intToBytes(iterations);
        
        if(sequence == 0){
            for(int i = 1; i <= iterations; i++){
                byte[] byteList = list[i % list.length].getBytes();
                byte[] seq = PacketUtil.intToBytes(i);
                
                byte[] bytes = new byte[byteList.length + seq.length + total.length];
                System.arraycopy(seq, 0, bytes, 0, seq.length);
                System.arraycopy(total, 0, bytes, seq.length, total.length);
                System.arraycopy(byteList, 0, bytes, total.length + seq.length, byteList.length);
                
                PacketUtil.send(socket, address, port, bytes, 0);
            }
        }
        else{
            byte[] byteList = list[sequence % list.length].getBytes();
            byte[] seq = PacketUtil.intToBytes(sequence);
            
            byte[] bytes = new byte[byteList.length + seq.length + total.length];
            System.arraycopy(seq, 0, bytes, 0, seq.length);
            System.arraycopy(total, 0, bytes, seq.length, total.length);
            System.arraycopy(byteList, 0, bytes, total.length + seq.length, byteList.length);
            
            PacketUtil.send(socket, address, port, bytes, 0);
        }
    }
    
    
    private void receiveFile(int file){
        int seq = retriveNumber(data);
        int total = retriveNumber(data);
        
        SystemInfo.fileRequestLock.get(file).lock();
        int lowestMissingSeq = SystemInfo.fileLowestMissing.get(file);

        if(!SystemInfo.fileSeq.get(file).contains(seq)){
            SystemInfo.fileSeq.get(file).add(seq);
            if (lowestMissingSeq == 0) lowestMissingSeq++;
            if(lowestMissingSeq == seq){
                for(int i = lowestMissingSeq; i <= total; i++)
                    if(!SystemInfo.fileSeq.get(file).contains(i)){
                        lowestMissingSeq = i;
                        break;
                    }
                if(lowestMissingSeq == seq) lowestMissingSeq = total;
                //Reset the file request timer here
                SystemInfo.fileTimers.put(file, System.currentTimeMillis());
            }
            if(lowestMissingSeq == total) // We received the entire file
                SystemInfo.fileLowestMissing.put(file, -2);
            else SystemInfo.fileLowestMissing.put(file, lowestMissingSeq);
        }
        else {
            SystemInfo.fileRequestLock.get(file).unlock();
            System.out.println("Repetido----------------------------------------------------------------: " + seq);
            return; // We allready have this sequence
        }
        SystemInfo.fileRequestLock.get(file).unlock();
        
        //Handle packet here -------------------------------------------------------------------------------------------------------------|+|
        String filePart = new String(data, 0, data.length);
        System.out.println("File part received: " + filePart + " file: " + file + " seq: " + seq + " total: " + total);
    }
}
