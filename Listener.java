import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.*;

/**
 * Escreva a descrição da classe FTRapid aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Listener extends Thread
{  
    private DatagramSocket socket;
    private boolean running = true;
    
    public Listener(DatagramSocket socket){
        this.socket = socket;
        
        try{
            socket.setReceiveBufferSize(SystemInfo.ReceiveBufferSize);
        }catch(Exception e){
            System.err.println("Error setting receive buffer size: " + e);
        }
    }
    
    public void run(){
        try{
            Listen();
        }catch (Exception e) {
            System.err.println(e);
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
}
