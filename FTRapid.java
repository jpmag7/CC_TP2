import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Escreva a descrição da classe FTRapid aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FTRapid extends Thread
{  
    private DatagramSocket socket;
    private boolean running = true;
    
    public FTRapid(DatagramSocket socket){
        this.socket = socket;
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
            
            PacketHandler ph = new PacketHandler(socket, packet);
            ph.start();
        }
    }
}
