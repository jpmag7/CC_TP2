import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.Random;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Escreva a descrição da classe PacketUtil aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class PacketUtil
{
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
        socket.send(packet);
        
        try{
            TimeUnit.NANOSECONDS.sleep(1);
        }catch (Exception e){
            System.err.println("Error on packet util sleep: " + e);
        }
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
    
    
    
    public static byte[] intToBytes( final int i ) {
        ByteBuffer bb = ByteBuffer.allocate(4); 
        bb.putInt(i); 
        return bb.array();
    }
    
    
    
    public static int byteArrayToInt(byte[] intBytes){
        ByteBuffer byteBuffer = ByteBuffer.wrap(intBytes);
        return byteBuffer.getInt();
    }
}
