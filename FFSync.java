import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.locks.*;
import java.util.ArrayList;

/**
 * Escreva a descrição da classe FFSync aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FFSync
{
    public static void main() throws Exception{
        // Confirm program arguments
        
        
        // Http server
        //HttpServer httpServer = new HttpServer();
        //httpServer.start();

        // FT-Rapid
        System.out.println("Start");
        
        DatagramSocket socketServer = new DatagramSocket(SystemInfo.FTRapidPort);
        DatagramSocket socketClient = new DatagramSocket(8080);
        InetAddress clientAddress = InetAddress.getByName("localhost");
        int clientPort = SystemInfo.FTRapidPort;
        
        FTRapid server = new FTRapid(socketServer);
        server.start();
        
        FTRapid client = new FTRapid(socketClient);
        client.start();
        
        SystemInfo.fileRequestLock.put(0, new ReentrantLock());
        SystemInfo.fileLowestMissing.put(0, 0);
        SystemInfo.fileSeq.put(0, new ArrayList<Integer>());
        
        Requester requester = new Requester(socketServer, clientAddress, clientPort, 0);
        
        long startTime = System.currentTimeMillis();
        
        requester.start();
        requester.join();
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("Ended");
        System.out.println("Packets received: " + SystemInfo.fileSeq.get(0).size());
        System.out.println("Transfer took: " + (endTime - startTime) + " milliseconds");
        
        
        server.join();
        client.join();
    }
}
