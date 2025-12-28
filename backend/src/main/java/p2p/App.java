package p2p;

import p2p.controller.FileController;
import java.io.IOException;

public class App {
    public static void main(String[] args) {
        try {
            // Railway dynamically assigns a port via the PORT environment variable
            String portEnv = System.getenv("PORT");
            int port = (portEnv != null) ? Integer.parseInt(portEnv) : 8080;

            FileController controller = new FileController(port);
            controller.start();

            // This helps you see in the Railway logs that the app started correctly
            System.out.println("PeerLink Backend initialized successfully.");
            System.out.println("Listening on 0.0.0.0:" + port);

        } catch (IOException e) {
            System.err.println("Critical failure during startup: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
