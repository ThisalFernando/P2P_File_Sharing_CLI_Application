import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class FileServer {
    private static final int PORT = 12345;
    private static List<Socket> clientSockets = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("File Server started...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSockets.add(clientSocket);
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                while (true) {
                    String request = in.readUTF();
                    if ("send".equalsIgnoreCase(request)) {
                        receiveFile(in);
                    } else if ("get".equalsIgnoreCase(request)) {
                        sendFile(out, in.readUTF());
                    } else if ("list".equalsIgnoreCase(request)) {
                        sendFileList(out);
                    } else if ("exit".equalsIgnoreCase(request)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("\nClient error: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("\nError closing socket: " + e.getMessage());
                }
            }
        }

        private void receiveFile(DataInputStream in) {
            try {
                String fileName = in.readUTF();
                long fileSize = in.readLong();
                File file = new File("server_" + fileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int read;
                    while (totalRead < fileSize && (read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                    System.out.println("\nReceived file: " + file.getName());
                }
            } catch (IOException e) {
                System.err.println("\nError receiving file: " + e.getMessage());
            }
        }

        private void sendFile(DataOutputStream out, String fileName) {
            File file = new File(fileName);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    out.writeUTF(file.getName());
                    out.writeLong(file.length());
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    System.out.println("\nSent file: " + file.getName());
                } catch (IOException e) {
                    System.err.println("\nError sending file: " + e.getMessage());
                }
            } else {
                System.err.println("\nFile not found: " + fileName);
                try {
                    out.writeUTF("\nFile not found");
                    out.writeLong(0);
                } catch (IOException e) {
                    System.err.println("\nError sending file not found message: " + e.getMessage());
                }
            }
        }

        private void sendFileList(DataOutputStream out) {
            try {
                File folder = new File(".");
                File[] files = folder.listFiles();
                if (files != null && files.length > 0) {
                    out.writeUTF("Files available:");
                    for (File file : files) {
                        if (file.isFile()) {
                            out.writeUTF(file.getName());
                        }
                    }
                } else {
                    out.writeUTF("No files available.");
                }
                out.writeUTF("END");
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending file list: " + e.getMessage());
            }
        }
    }
}