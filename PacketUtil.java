import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.Random;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * Escreva a descrição da classe PacketUtil aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class PacketUtil
{
    public static Random r = new Random();
    private final static boolean simulNet = true;
    
    public static void send(DatagramSocket socket, InetAddress address, int port, byte[] data, int ID) throws Exception{
        byte[] id = intToBytes(ID);
        byte[] dataAndID = new byte[data.length + id.length];
        System.arraycopy(id, 0, dataAndID, 0, id.length);
        System.arraycopy(data, 0, dataAndID, id.length, data.length);
        int hashInt = Arrays.hashCode(dataAndID);
        byte[] hash = intToBytes(hashInt);
        
        dataAndID = crypt(dataAndID, hashInt);
        
        byte[] encryptedData = new byte[hash.length + dataAndID.length];
        System.arraycopy(hash, 0, encryptedData, 0, hash.length);
        System.arraycopy(dataAndID, 0, encryptedData, hash.length, dataAndID.length);
        
        DatagramPacket packet = new DatagramPacket(encryptedData, encryptedData.length, address, port);
        
        if(simulNet) 
            for (int i = 0; i < (ID > 0 ? SystemInfo.Redundancy : 1); i++) 
                simulNet(socket, packet);
        else for (int i = 0; i < (ID > 0 ? SystemInfo.Redundancy : 1); i++)
                socket.send(packet);
    }
    
    
    private static void simulNet(DatagramSocket socket, DatagramPacket packet) throws Exception{
        float packetLossProb = 15;
        int maxPacketDelay = 3;
        
        if(r.nextDouble() * 100f < 100f - packetLossProb){
            try{
                TimeUnit.MILLISECONDS.sleep(r.nextInt(maxPacketDelay + 1));
            }catch (Exception e){
                Setup.log("Error on SimulNet wait: " + e);
            }
            socket.send(packet);
        }
    }
    
    
    public static byte[] makePacket(int seqNum, int totalNum, byte[] data){
        byte[] seq = PacketUtil.intToBytes(seqNum);
        byte[] total = PacketUtil.intToBytes(totalNum);
        
        byte[] bytes = new byte[data.length + seq.length + total.length];
        
        System.arraycopy(seq, 0, bytes, 0, seq.length);
        System.arraycopy(total, 0, bytes, seq.length, total.length);
        System.arraycopy(data, 0, bytes, total.length + seq.length, data.length);
        return bytes;
    }
    
    
    
    public static byte[] crypt(byte[] bytes, int hash){
        Random random = new Random(SystemInfo.PassHash + hash);
        
        byte[] values = new byte[bytes.length];
        random.nextBytes(values);
        
        int i = 0;
        for (byte b : bytes)
            bytes[i] = (byte) (b ^ values[i++]);
        
        return bytes;
    }
    
    
    public static byte[] crypt(byte[] bytes, int hash, int start, int end){
        Random random = new Random(SystemInfo.PassHash + hash);
        
        byte[] values = new byte[end - start];
        random.nextBytes(values);
        
        int v = 0;
        for (int i = start; i < end; i++)
            bytes[i] = (byte) (bytes[i] ^ values[v++]);
        
        return bytes;
    }
    
    
    public static byte[] intToBytes( final int i ) {
        ByteBuffer bb = ByteBuffer.allocate(4); 
        bb.putInt(i); 
        return bb.array();
    }

    
    public static int byteArrayToInt(byte[] intBytes){
        ByteBuffer byteBuffer = ByteBuffer.wrap(intBytes);
        return byteBuffer.getInt();
    }
    
    
    public static int byteArrayToInt(byte[] intBytes, int start, int end){
        ByteBuffer byteBuffer = ByteBuffer.wrap(intBytes, start, end - start);
        return byteBuffer.getInt();
    }
}
