package p2p.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import p2p.utils.UploadUtils;

public class FileSharer {

    private HashMap<Integer, String> availableFiles;
    public FileSharer() {
        availableFiles = new HashMap<>();
    }

    public int offerFiles(String filePath){
        int port;
        while(true){
            port = UploadUtils.generateCode();
            if(!availableFiles.containsKey(port)){
                availableFiles.put(port, filePath);
                return port;
            }
        }
    }

    public void startFileServer(int port){
        String filePath = availableFiles.get(port);
        if(filePath == null){
            System.out.println("No file associated on this port."+ port);
            return;
        }
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serving file"+ new File(filePath).getName() +" on port "+ port);
            // Keep accepting connections until server is closed
            while(!serverSocket.isClosed()){
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client Connection:" + clientSocket.getInetAddress());
                    // Handle each client in a separate thread
                    new Thread(new FileSenderHandler(clientSocket, filePath)).start();
                } catch (IOException ex){
                    if(!serverSocket.isClosed()){
                        System.err.println("Error accepting client on port "+ port + ": " + ex.getMessage());
                    }
                    break;
                }
            }
        }catch (IOException ex){
            System.err.println("Error handling file server on port:"+ port);
        }
    }


    private static class FileSenderHandler implements Runnable{

        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket, String filePath){
            this.clientSocket = clientSocket;
            this.filePath = filePath;
        }


        @Override
        public void run() {
            try(FileInputStream fis = new FileInputStream(filePath)){
                OutputStream oos = clientSocket.getOutputStream();
                String fileName = new File(filePath).getName();
                String header = "FILENAME:" + fileName + "\n";
                oos.write(header.getBytes());

                byte[] buffer = new byte[4096];
                int byteRead;
                while((byteRead = fis.read(buffer)) != -1){
                    oos.write(buffer, 0, byteRead);
                }
                System.out.println("File "+ fileName +" sent to "+ clientSocket.getInetAddress()); 
            }catch(Exception ex){
                System.err.println("Error sending file: " + ex.getMessage());
            }finally{
                try{
                    clientSocket.close();
                }catch(Exception e){
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
    
}
