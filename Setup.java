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
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
    public static InetAddress[] addresses;
    private static int[] ports;
    private static Map<InetAddress, List<Integer>> portsByAddresses = new ConcurrentHashMap<>();
    
    // Verificar se o folder existe.
    private static boolean folderVerifica (String folder) {
        Path path = Paths.get(folder);
        return Files.exists(path);
    }
    
    // Verifica se o endereço do parceiro é o correto;
    private static void enderecoVerifica (String[] enderecos) throws Exception {
        addresses = new InetAddress[enderecos.length];
        ports = new int[enderecos.length];
        int i = 0;
        for(String add : enderecos){
            String[] addAndPort = add.split(":", 2);
            InetAddress address = InetAddress.getByName(addAndPort[0]);
            if (!address.isReachable(500)) {
                addresses = null;
                return;
            }
            else {
                try{
                    ports[i] = addAndPort.length == 1 ? SystemInfo.DefaultFTRapidPort : Integer.parseInt(addAndPort[1]);
                    if(!portsByAddresses.containsKey(address)) portsByAddresses.put(address, new ArrayList<>());
                    portsByAddresses.get(address).add(ports[i]);
                }catch(NumberFormatException e){
                    Setup.log("Invalid port: " + addAndPort[1] + " on address " + addAndPort[0]);
                    System.out.println("Invalid port: " + addAndPort[1] + " on address " + addAndPort[0]);
                }
                addresses[i++] = address;
            }
        }
    } 
    
    
    public static int findPort(InetAddress address, int p){
        List<Integer> addPorts = portsByAddresses.get(address);
        int closest = 10000000;
        
        for(Integer i : addPorts){
            if(p >= i && p - i < closest - 1){
                closest = i;
            }
        }
        
        return closest;
    }
    
    // Preenche a nossa lista de ficheiros
    public static void preencheLista(String pasta) throws Exception{
        File file = new File(pasta);
        List<String> l = new ArrayList<>();
        listOfFiles(file, l, "");
        SystemInfo.m_list = l.toArray(new String[0]);
        SystemInfo.m_list_hash = new String[SystemInfo.m_list.length];
        
        int j = 0;
        for(String f : SystemInfo.m_list){
            if(Files.isRegularFile(Paths.get(pasta, f))){
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                //Get the checksum
                File cf = new File(Paths.get(pasta, SystemInfo.m_list[j]).toString());
                String checksum = FileManager.getFileChecksum(md5Digest, cf);
                SystemInfo.m_list_time.put(SystemInfo.m_list[j], cf.lastModified());
                SystemInfo.m_list_hash[j++] = checksum;
            }
            else { // is empty directory
                File cf = new File(Paths.get(pasta, SystemInfo.m_list[j]).toString());
                SystemInfo.m_list_time.put(SystemInfo.m_list[j], cf.lastModified());
                SystemInfo.m_list_hash[j++] = "0";
            }
        }
        
        int i = 1;
        for(String s : SystemInfo.m_list){
            if(Files.isRegularFile(Paths.get(pasta, s))){
                String p = Paths.get(pasta, s).toString();
                FileManager.filesSendSize.put(i, Files.size(Paths.get(p)) / FileManager.payloadSize + 1);
                FileManager.filesSend.put(i, new FileInputStream(p));
                i++;
            }
            else i++;
        }
    }
    
    
    public static void listOfFiles(File dirPath, List<String> l, String op){
        File filesList[] = dirPath.listFiles();
        for(File file : filesList) {
           if(file.isFile()) {
              l.add(op == "" ? file.getName() : Paths.get(op, file.getName()).toString());
           } else {
              listOfFiles(file, l, op == "" ? file.getName() : Paths.get(op, file.getName()).toString());
           }
        }
        // If is empty folder
        if(filesList.length == 0 && !op.equals("")) {
            l.add(Paths.get(op, "§").toString().replace("§", ""));
        }
    }
    
    
    public static boolean setup(String[] args) throws Exception{
        if(args.length < 2){
            System.out.println("Invalid arguments");
            return false;
        }
        
        String pasta = args[0];
        boolean folderExists = folderVerifica(pasta);
        boolean customPort = true;
        int myPort = SystemInfo.DefaultFTRapidPort;
        try{
            myPort = Integer.parseInt(args[1]);
        }catch (NumberFormatException e){
            customPort = false;
        }
        SystemInfo.FTRapidPort = myPort;
        setupLogFile(pasta);
        Setup.log("Setting FTRapid port to: " + SystemInfo.FTRapidPort);
        
        String[] addressesString = Arrays.copyOfRange(args, (customPort ? 2 : 1), Math.min(args.length, SystemInfo.ReceiveBufferSize / SystemInfo.PacketSize + 1));
        enderecoVerifica(addressesString);
        
        if(addresses == null || !folderExists) return false;
        SystemInfo.folder = pasta;
        preencheLista(pasta);
        
        return true;
    }
    
    
    private static void setupLogFile(String pasta){
        try{
            File file = new File(pasta);
            String folderName = file.getName();
            logFile = new FileOutputStream(new File("logs" + folderName + ".txt"));
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("ERROR openning log file");
        }
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
    
    
    public static void setupSystemInfo() throws Exception{
        SystemInfo.BatchSizeReceive = (SystemInfo.ReceiveBufferSize / SystemInfo.PacketSize) / addresses.length;
        int i = 0;
        
        for(InetAddress add : addresses){
            String address = "" + add.toString().substring(add.toString().indexOf("/")) + ":" + ports[i++];
            SystemInfo.fileRequestLock.put(address, new HashMap<>());
            SystemInfo.fileLowestMissing.put(address, new HashMap<>());
            SystemInfo.fileSeq.put(address, new HashMap<>());
            SystemInfo.fileTimers.put(address, new HashMap<>());
            SystemInfo.their_lists.put(address, new HashMap<>());
            SystemInfo.their_lists_hash.put(address, new HashMap<>());
            SystemInfo.their_lists_time.put(address, new HashMap<>());
            SystemInfo.their_lists_size.put(address, new HashMap<>());
            SystemInfo.fileTransferTime.put(address, new HashMap<>());
            FileManager.filesAsked.put(address, new AtomicInteger());
            FileManager.filesReceived.put(address, new AtomicInteger());
            
            SystemInfo.fileLowestMissing.get(address).put(SystemInfo.FYN, -1L);
            
            PacketUtil.theirSUsed.put(address, new AtomicLong());
            
            setupForNewFile(address, 0); // Prepare to receive clint's lists
        }
    }
    
    public static void setupForNewFile(String address, int file){
        SystemInfo.fileRequestLock.get(address).put(file, new ReentrantLock());
        SystemInfo.fileLowestMissing.get(address).put(file, -1L);
        SystemInfo.fileSeq.get(address).put(file, new HashSet<Long>());
    }
    
    
    public static void requestAllLists() throws Exception{
        int i = 0;
        for(InetAddress address : addresses){
            Requester r = new Requester(SystemInfo.socket, address, ports[i++], 0, SystemInfo.REQUEST);
            r.start();
        }
    }
    
}
