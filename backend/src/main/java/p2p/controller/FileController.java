package p2p.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import p2p.services.FileSharer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        // Bind to 0.0.0.0 for Cloud accessibility
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        
        // Pass the server instance to FileSharer so it can add routes dynamically
        this.fileSharer = new FileSharer(server);
        
        // Using temp directory for cloud-native storage
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);
        
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        // Pleaes register standard routes
        server.createContext("/upload", new UploadHandler());
        server.createContext("/", new DefaultHandler()); 
        
        server.setExecutor(executorService);
    }
    
    public void start() {
        server.start();
        System.out.println("Server running on port: " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
        executorService.shutdown();
    }
* Instead of a port, this returns a unique fileId.
    /**
     * Fixed CORS logic to handle Allow-Credentials properly
     */
    private void setCORSHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        
        // Browser security: If credentials are true, Origin CANNOT be "*"
        if (origin != null) {
            headers.set("Access-Control-Allow-Origin", origin);
        } else {
            headers.set("Access-Control-Allow-Origin", "*");
        }
        
        headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        headers.set("Access-Control-Allow-Credentials", "true");
    }

    private class DefaultHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            String response = "PeerLink Backend Active";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                sendError(exchange, 400, "Invalid Content-Type. Expected multipart/form-data.");
                return;
            }

            try {
                String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(), baos);
                
                MultipartParser parser = new MultipartParser(baos.toByteArray(), boundary);
                MultipartParser.ParseResult result = parser.parse();
                
                if (result == null) {
                    throw new IOException("Failed to parse multipart data");
                }

                // Save file
                String uniqueFilename = UUID.randomUUID().toString() + "_" + result.filename;
                File fileToSave = new File(uploadDir, uniqueFilename);
                
                try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                    fos.write(result.fileContent);
                }

                // Register with FileSharer
                String fileId = fileSharer.offerFile(fileToSave.getAbsolutePath());
                
                String jsonResponse = String.format("{\"fileId\": \"%s\", \"downloadPath\": \"/download/%s\"}", fileId, fileId);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonResponse.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Upload failed: " + e.getMessage());
            }
        }
    }

     // Helper to send clean error responses
    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] response = message.getBytes();
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // MultipartParser remains the same...
    private static class MultipartParser {
        private final byte[] data;
        private final String boundary;
        
        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }
        
        public ParseResult parse() {
            try {
                String dataAsString = new String(data, "ISO-8859-1");
                String filenameMarker = "filename=\"";
                int filenameStart = dataAsString.indexOf(filenameMarker) + filenameMarker.length();
                int filenameEnd = dataAsString.indexOf("\"", filenameStart);
                String filename = dataAsString.substring(filenameStart, filenameEnd);
                
                int headerEnd = dataAsString.indexOf("\r\n\r\n") + 4;
                byte[] boundaryBytes = ("\r\n--" + boundary).getBytes("ISO-8859-1");
                int contentEnd = findSequence(data, boundaryBytes, headerEnd);
                
                byte[] fileContent = new byte[contentEnd - headerEnd];
                System.arraycopy(data, headerEnd, fileContent, 0, fileContent.length);
                
                return new ParseResult(filename, fileContent);
            } catch (Exception e) {
                return null;
            }
        }
        
        private int findSequence(byte[] data, byte[] sequence, int start) {
            for (int i = start; i <= data.length - sequence.length; i++) {
                boolean match = true;
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) return i;
            }
            return -1;
        }

        public static class ParseResult {
            public final String filename;
            public final byte[] fileContent;
            public ParseResult(String filename, byte[] fileContent) {
                this.filename = filename;
                this.fileContent = fileContent;
            }
        }
    }
}
