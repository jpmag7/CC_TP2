import java.io.File;
import java.nio.file.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.*;
import java.util.HashSet;
import java.net.DatagramSocket;

/**
 * Escreva a descrição da classe Setup aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Setup
{
    // Verificar se o folder existe.
    private static boolean folderVerifica (String folder) {
        Path path = Paths.get(folder);
        return Files.exists(path);
    }
    
    // Verifica se o endereço do parceiro é o correto;
    private static boolean enderecoVerifica (String[] enderecos) throws Exception {
        for(String add : enderecos){
            if (!InetAddress.getByName(add).isReachable(500)) return false;
        }
        return true;
    } 
    
    // Verifica ambas as condições
    public static boolean verificaParametros (String folder,String[] enderecos) throws Exception{
        return (folderVerifica(folder) && enderecoVerifica(enderecos));
    }
    
    // Preenche a nossa lista de ficheiros
    public static void preencheLista(String pasta){
        File f = new File(pasta);
        SystemInfo.m_list = f.list();
    }
    
    
    public static String[] setup(String[] args) throws Exception{
        if(args.length < 2){
            System.out.println("Invalid arguments");
            return null;
        }
        
        String pasta = args[0];
        String[] addresses = Arrays.copyOfRange(args, 1, args.length);
        preencheLista(pasta);
        
        if(verificaParametros(pasta, addresses))
            return addresses;
        else return null;
    }
    
    
    public static void setupSystemInfo(String[] addresses) throws Exception{
        SystemInfo.BatchSizeReceive = SystemInfo.ReceiveBufferSize / SystemInfo.PacketSize / addresses.length;
        SystemInfo.ActiveClients = addresses.length;
        
        for(String add : addresses){
            InetAddress address = InetAddress.getByName(add);
            SystemInfo.fileRequestLock.put(address, new HashMap<>());
            SystemInfo.fileLowestMissing.put(address, new HashMap<>());
            SystemInfo.fileSeq.put(address, new HashMap<>());
            SystemInfo.fileTimers.put(address, new HashMap<>());
            SystemInfo.batchRequestedLock.put(address, new ReentrantLock());
            
            setupForNewFile(address, 0); // Prepare to receive clint's lists
        }
    }
    
    
    public static void setupForNewFile(InetAddress address, int file){
        SystemInfo.fileRequestLock.get(address).put(file, new ReentrantLock());
        SystemInfo.fileLowestMissing.get(address).put(file, -1);
        SystemInfo.fileSeq.get(address).put(file, new HashSet<Integer>());
    }
    
    
    public static void requestAllLists(DatagramSocket socket, String[] addresses) throws Exception{
        for(String add : addresses){
            InetAddress address = InetAddress.getByName(add);
            Requester r = new Requester(socket, address, SystemInfo.FTRapidPort, 0);
            r.start();
            r.join();
        }
    }
}
