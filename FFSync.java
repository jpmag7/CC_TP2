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
import java.util.List;

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
        // Args -> "C:\\Users\\jpmag\\OneDrive\\Ambiente de Trabalho\\Test1" "8080" "localhost" 
        // Confirm program arguments
        try{
            if(!Setup.setup(args)) {
                Setup.log("Invalid arguments (folder inexistent or ip's are down)");
                System.out.println("Invalid arguments (folder inexistent or ip's are down)");
                return;
            }
        }catch (Exception e){
            Setup.log("Error on setup: " + e);
            return;
        }
        
        // Http server
        HttpServer httpServer = new HttpServer(SystemInfo.FTRapidPort);
        httpServer.start();
        Setup.log("Http server created");
        
        // Password
        SystemInfo.PassHash = Arrays.hashCode(System.console().readPassword("Pasword:"));
        System.out.println("Starting sync");
        Setup.log("Starting FTRapid");
        
        // Setup system info variables
        Setup.setupSystemInfo();
        
        // Request lists of all clients
        Setup.requestAllLists();
        
        // Start listenning for packets
        List<Listener> ls = new ArrayList<>();
        Listener lm = new Listener(SystemInfo.mainSocket);
        lm.start();
        ls.add(lm);
        for(DatagramSocket s : SystemInfo.receSockets.values()){
            Listener l = new Listener(s);
            l.start();
            ls.add(l);
        }
        for(DatagramSocket s : SystemInfo.sendSockets.values()){
            Listener l = new Listener(s);
            l.start();
            ls.add(l);
        }
        
        // Wait for FYN from clients
        for(Listener l : ls) l.join();
        
        // Type to close
        System.out.println("Syncing process has ended");
        System.console().readPassword("Press enter to close http server and exit");
        HttpServer.serverSocket.close();
        
        Setup.log("Exiting");
        System.out.println("Exiting");
    }
}
