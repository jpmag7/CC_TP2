import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.net.SocketException;

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
            System.err.println("Socket created in port: " + port);
        
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.err.println("New client connection");
                
                HttpThread thread = new HttpThread(clientSocket);
                thread.start();
            }
        }catch (SocketException e){}
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
            catch(Exception e) {System.err.println("Couldn't write Html to client");}
            finally{
                try{
                    out.close();
                    in.close();
                    clientSocket.close();
                }catch (Exception e) {System.err.println("Couldn't close client");}
            }
        }
        
        private void handleRequest() throws Exception{
            switch (url){
                case "/":
                    out.write("HTTP/1.1 200 OK\r\n");
                    //out.write("Content-Type: text/html\r\n");
                    //out.write("Content-Length: 41\r\n");
                    out.write("\r\n");
                    out.write("<TITLE>TEST</TITLE>");
                    out.write("<P>Hello World!</P>");
                default:
                    System.err.println("Wrong Url");
            }
        }
    }
}
