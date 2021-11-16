import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.locks.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Escreva a descrição da classe FFSync aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FFSync
{
    public static void main(String[] args) throws Exception{ 
        // Args -> "C:\\Users\\jpmag\\OneDrive\\Ambiente de Trabalho\\Test", "localhost"
        // Args -> "C:\\Users\\jpmag\\OneDrive\\Ambiente de Trabalho\\Test","80", "localhost" 
        // Confirm program arguments
        InetAddress[] addresses;
        try{
            if((addresses = Setup.setup(args)) == null) {
                System.err.println("Invalid arguments (folder inexistent or ip's are down)");
                return;
            }
        }catch (Exception e){
            System.err.println("Error on setup: " + e);
            return;
        }
        
        // Http server
        //HttpServer httpServer = new HttpServer();
        //httpServer.start();

        // FT-Rapid
        System.out.println("Start");
        
        //DatagramSocket socketServer = new DatagramSocket(SystemInfo.FTRapidPort);
        DatagramSocket socketClient = new DatagramSocket(SystemInfo.FTRapidPort == 8080 ? 80 : 8080);
        
        Setup.setupSystemInfo(addresses);
        
        //Listener server = new Listener(socketServer);
        //server.start();
        
        Listener client = new Listener(socketClient);
        client.start();
        
        Setup.requestAllLists(socketClient, addresses);
        
        //System.err.println("Ended");
        //System.err.println("Unique packets received: " + SystemInfo.fileSeq.get(addresses[0]).get(0).size());
        //System.err.println("Total received: " + (SystemInfo.fileSeq.get(addresses[0]).get(0).size() + SystemInfo.RepetedPackets));
        //System.err.println("Repeted packets: " + SystemInfo.RepetedPackets);
        
        
        //server.join();
        client.join();
        
        //socketServer.close();
        socketClient.close();
    }
}
