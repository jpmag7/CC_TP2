import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;

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
        if(FileManager.filesReceived.get() == FileManager.filesAsked.get() &&
           SystemInfo.their_lists.size() == SystemInfo.doneClients.size()) {
           socket.close();
        }
    }
}
