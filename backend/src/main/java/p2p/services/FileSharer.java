package p2p.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.util.UUID;

public class FileSharer {

    private final HttpServer server;

    /**
     * Constructor now takes the existing HttpServer so we don't open new ports (railway offers only 1 port or some shit).
     */
    public FileSharer(HttpServer server) {
        this.server = server;
    }

    /**
     * It registers a new URL route (/download/uuid) on the main railway server.
     */
    public String offerFile(String filePath) {
        // We use a UUID for security so people can't guess download links
        String fileId = UUID.randomUUID().toString();
        String contextPath = "/download/" + fileId;

        // Registering the dynamic route
        server.createContext(contextPath, new HttpDownloadHandler(filePath));
        
        System.out.println("New file registered at path: " + contextPath);
        return fileId;
    }

    private static class HttpDownloadHandler implements HttpHandler {
        private final String filePath;

        public HttpDownloadHandler(String filePath) {
            this.filePath = filePath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // patched : Enable CORS for downloads
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Expose-Headers", "Content-Disposition");
            
            File file = new File(filePath);
            if (!file.exists()) {
                String response = "Error: File not found on server.";
                exchange.sendResponseHeaders(404, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            // Extracting the original filename by removing the UUID prefix (36 chars UUID + 1 char '_')
            String originalFilename = file.getName();
            if (originalFilename.length() > 37 && originalFilename.charAt(36) == '_') {
                originalFilename = originalFilename.substring(37);
            }

            // Setting HTTP Headers for a file download
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + originalFilename + "\"");
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            
            // Sending response status and file lengfth
            exchange.sendResponseHeaders(200, file.length());

            // Streaming the file to the browser
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
