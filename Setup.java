import java.io.File;
import java.nio.file.*;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.locks.*;
import java.util.HashSet;
import java.net.DatagramSocket;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

/**
 * Escreva a descrição da classe Setup aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class Setup
{
    private static FileOutputStream logFile;
    private static Lock l = new ReentrantLock();
    
    // Verificar se o folder existe.
    private static boolean folderVerifica (String folder) {
        Path path = Paths.get(folder);
        return Files.exists(path);
    }
    
    // Verifica se o endereço do parceiro é o correto;
    private static InetAddress[] enderecoVerifica (String[] enderecos) throws Exception {
        InetAddress[] addresses = new InetAddress[enderecos.length];
        int i = 0;
        for(String add : enderecos){
            InetAddress address = InetAddress.getByName(add);
            if (!address.isReachable(500)) return null;
            else addresses[i++] = address;
        }
        return addresses;
    } 
    
    // Preenche a nossa lista de ficheiros
    public static void preencheLista(String pasta) throws Exception{
        File file = new File(pasta);
        SystemInfo.m_list = file.list();
        SystemInfo.m_list_hash = new String[SystemInfo.m_list.length];
        
        int j = 0;
        for(String f : SystemInfo.m_list){
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            //Get the checksum
            File cf = new File(pasta + "\\\\" + SystemInfo.m_list[j]);
            String checksum = FileManager.getFileChecksum(md5Digest, cf);
            SystemInfo.m_list_time.put(SystemInfo.m_list[j], cf.lastModified());
            SystemInfo.m_list_hash[j++] = checksum;
        }
        
        int i = 1;
        for(String s : SystemInfo.m_list){
            String p = pasta + "\\\\" + s;
            FileManager.filesSendSize.put(i, (int)Files.size(Paths.get(p)) / FileManager.payloadSize + 1);
            FileManager.filesSend.put(i, new FileInputStream(p));
            i++;
        }
    }
    
    
    public static InetAddress[] setup(String[] args) throws Exception{
        if(args.length < 3){ // 2
            System.out.println("Invalid arguments");
            return null;
        }
        
        String pasta = args[0];
        boolean folderExists = folderVerifica(pasta);
        
        SystemInfo.FTRapidPort = Integer.parseInt(args[1]);
        setupLogFile();
        
        String[] addresses = Arrays.copyOfRange(args, 2, Math.min(args.length, SystemInfo.ReceiveBufferSize / SystemInfo.PacketSize + 1));
        InetAddress[] inetAddresses = enderecoVerifica(addresses);
        
        if(inetAddresses == null || !folderExists) return null;
        SystemInfo.folder = pasta;
        preencheLista(pasta);
        
        return inetAddresses;
    }
    
    
    private static void setupLogFile(){
        try{
            logFile = new FileOutputStream("logs" + SystemInfo.FTRapidPort + ".txt");
        }catch(Exception e){}
    }
    
    
    public static void log(String s){
        l.lock();
        try
        {
            logFile.write((System.currentTimeMillis() + ": " + s + "\n").getBytes());
        }
        catch (java.io.IOException ioe)
        {
            ioe.printStackTrace();
        }
        finally{
            l.unlock();
        }
    }
    
    
    public static void setupSystemInfo(InetAddress[] addresses) throws Exception{
        SystemInfo.BatchSizeReceive = SystemInfo.ReceiveBufferSize / SystemInfo.PacketSize / addresses.length;
        
        for(InetAddress address : addresses){
            SystemInfo.fileRequestLock.put(address, new HashMap<>());
            SystemInfo.fileLowestMissing.put(address, new HashMap<>());
            SystemInfo.fileSeq.put(address, new HashMap<>());
            SystemInfo.fileTimers.put(address, new HashMap<>());
            SystemInfo.their_lists.put(address, new HashMap<>());
            SystemInfo.their_lists_hash.put(address, new HashMap<>());
            SystemInfo.their_lists_time.put(address, new HashMap<>());
            
            setupForNewFile(address, 0); // Prepare to receive clint's lists
        }
    }
    
    
    public static void setupForNewFile(InetAddress address, int file){
        SystemInfo.fileRequestLock.get(address).put(file, new ReentrantLock());
        SystemInfo.fileLowestMissing.get(address).put(file, -1);
        SystemInfo.fileSeq.get(address).put(file, new HashSet<Integer>());
    }
    
    
    public static void requestAllLists(DatagramSocket socket, InetAddress[] addresses) throws Exception{
        for(InetAddress address : addresses){
            FileManager.filesAsked.incrementAndGet();
            Requester r = new Requester(socket, address, (SystemInfo.FTRapidPort == 80 ? 8080 : 80), 0, SystemInfo.REQUEST);
            r.start();
        }
    }
}
