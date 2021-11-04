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
        // Confirm program arguments
        String[] addresses;
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
        
        DatagramSocket socketServer = new DatagramSocket(SystemInfo.FTRapidPort);
        DatagramSocket socketClient = new DatagramSocket(8080);
        InetAddress clientAddress = InetAddress.getByName("localhost");
        int clientPort = SystemInfo.FTRapidPort;
        
        Setup.setupSystemInfo(addresses);
        
        FTRapid server = new FTRapid(socketServer);
        server.start();
        
        FTRapid client = new FTRapid(socketClient);
        client.start();
        
        Setup.requestAllLists(socketServer, addresses);
        
        System.err.println("Ended");
        System.err.println("Unique packets received: " + SystemInfo.fileSeq.get(clientAddress).get(0).size());
        System.err.println("Total received: " + (SystemInfo.fileSeq.get(clientAddress).get(0).size() + SystemInfo.RepetedPackets));
        System.err.println("Repeted packets: " + SystemInfo.RepetedPackets);
        
        
        server.join();
        client.join();
    }
}
