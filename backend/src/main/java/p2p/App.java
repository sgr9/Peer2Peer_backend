package p2p;

import p2p.controller.FileController;
import java.io.IOException;

/**
 * PeerLink - P2P File Sharing Application
 */
public class App {
    public static void main(String[] args) {
        try {
            // Get port from environment variable or default to 8080
            int port = 8080;
            String portEnv = System.getenv("PORT");
            if (portEnv != null && !portEnv.isEmpty()) {
                try {
                    port = Integer.parseInt(portEnv);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid PORT environment variable: " + portEnv + ", using default 8080");
                }
            }
            
            // Start the API server
            FileController fileController = new FileController(port);
            fileController.start();
            
            System.out.println("PeerLink server started on port " + port);
            System.out.println("API endpoints ready for connections");
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                fileController.stop();
            }));
            
            System.out.println("Press Enter to stop the server");
            System.in.read();
            
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
