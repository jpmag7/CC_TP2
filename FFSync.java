import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.concurrent.locks.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * Escreva a descrição da classe FFSync aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class FFSync
{
    public static void main(String[] args) throws Exception{ 
        // Args -> "C:\\Users\\jpmag\\OneDrive\\Ambiente de Trabalho\\Test" "localhost"
        // Args -> "C:\\Users\\jpmag\\OneDrive\\Ambiente de Trabalho\\Test" "80" "localhost" 
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
        
        // Password
        System.out.println("Password:");
        SystemInfo.PassHash = new Scanner(System.in).nextLine().hashCode();
        
        // Http server
        HttpServer httpServer = new HttpServer(SystemInfo.FTRapidPort == 8080 ? 80 : 8080);
        httpServer.start();

        // FT-Rapid
        System.out.println("Start");
        
        DatagramSocket socketClient = new DatagramSocket(SystemInfo.FTRapidPort == 8080 ? 80 : 8080);
        
        Setup.setupSystemInfo(addresses);
        
        Setup.requestAllLists(socketClient, addresses);
        
        Listener client = new Listener(socketClient);
        client.start();
        
        client.join();
        HttpServer.serverSocket.close();
        
        System.out.println("Folders are syncronized. Exiting");
    }
}
