package p2p.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.util.UUID;

public class FileSharer {

    private final HttpServer server;

    /**
     * Constructor now takes the existing HttpServer so we don't open new ports.
     */
    public FileSharer(HttpServer server) {
        this.server = server;
    }

    /**
     * Instead of a port, this returns a unique fileId.
     * It registers a new URL route (/download/uuid) on your main server.
     */
    public String offerFile(String filePath) {
        // We use a UUID for security so people can't guess download links
        String fileId = UUID.randomUUID().toString();
        String contextPath = "/download/" + fileId;

        // Register the dynamic route
        server.createContext(contextPath, new HttpDownloadHandler(filePath));
        
        System.out.println("New file registered at path: " + contextPath);
        return fileId;
    }

    /**
     * This replaces the old Socket-based FileSenderHandler.
     * It uses standard HTTP streaming.
     */
    private static class HttpDownloadHandler implements HttpHandler {
        private final String filePath;

        public HttpDownloadHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 1. Enable CORS for downloads
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            
            File file = new File(filePath);
            if (!file.exists()) {
                String response = "Error: File not found on server.";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            // 2. Set HTTP Headers for a file download
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            
            // 3. Send response status and file length
            exchange.sendResponseHeaders(200, file.length());

            // 4. Stream the file bytes to the browser
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("Successfully served file: " + file.getName());
        }
    }
}
