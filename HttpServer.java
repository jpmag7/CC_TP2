import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.net.SocketException;
import java.util.Arrays;

/**
 * Escreva a descrição da classe HttpServer aqui.
 * 
 * @author (seu nome) 
 * @version (número de versão ou data)
 */
public class HttpServer extends Thread
{
    public static ServerSocket serverSocket;
    private int port;
    
    public HttpServer(int port){
        this.port = port;
    }
    
    public void run(){
        try{
            serverSocket = new ServerSocket(port);
        
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                HttpThread thread = new HttpThread(clientSocket);
                thread.start();
            }
        }catch (SocketException e){
            Setup.log("Closing http server");
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    
    public class HttpThread extends Thread {
        Socket clientSocket;
        BufferedReader in;
        BufferedWriter out;
        String url;
        
        public HttpThread(Socket clientSocket) throws Exception{
            this.clientSocket = clientSocket;
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            url = in.readLine().split(" ")[1];
        }

        public void run(){
            try{handleRequest();}
            catch(Exception e) {Setup.log("Couldn't write Html to client");}
            finally{
                try{
                    out.close();
                    in.close();
                    clientSocket.close();
                }catch (Exception e) {Setup.log("Couldn't close client");}
            }
        }
        
        private void handleRequest() throws Exception{
            switch (url){
                case "/":
                    out.write("HTTP/1.1 200 OK\r\n");
                    //out.write("Content-Type: text/html\r\n");
                    //out.write("Content-Length: 41\r\n");
                    out.write("\r\n");
                    out.write("<TITLE>FTRapid " + SystemInfo.FTRapidPort + "</TITLE>");
                    out.write("<P>Folder: " + SystemInfo.folder + "</P>");
                    out.write("<P>My files: " + Arrays.toString(SystemInfo.m_list) + "</P>");
                    out.write("<P>Clients: " + SystemInfo.their_lists.keySet().toString() + "</P>");
                    out.write("<P>Number of files received: " + Math.max(0, (FileManager.filesReceived.get() - 2)) +
                    "/" + Math.max(0, (FileManager.filesAsked.get() - 2)) + "</P>");
                    out.write("<P>Files received: " + SystemInfo.filesReceived.toString() + "</P>");
                    out.write("<P>Transfered packets: " + SystemInfo.transferedPackets.get() + "</P>");
                    out.write("<P>Sended packets: " + SystemInfo.sendedPackets.get() + "</P>");
                    out.write("<P>Repeated packets: " + SystemInfo.repeatedPackets.get() + "</P>");
                    out.write("<P>Corrupted packets: " + SystemInfo.corruptedPackets.get() + "</P>");
                default:
            }
        }
    }
}
