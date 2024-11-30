import java.io.*;
import java.net.*;

public class FileClient {
    // IP address of the server
    private static final String SERVER_ADDRESS = "192.168.x.x";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            String command;
            while (true) {
                System.out.println("Enter command (send / get / list / exit): ");
                command = reader.readLine();
                out.writeUTF(command);
                out.flush();

                if ("send".equalsIgnoreCase(command)) {
                    sendFile(out, reader);
                } else if ("get".equalsIgnoreCase(command)) {
                    requestFile(out, in, reader);
                } else if ("list".equalsIgnoreCase(command)) {
                    viewFileList(in);
                } else if ("exit".equalsIgnoreCase(command)) {
                    break;
                }

                System.out.println();
            }
        } catch (IOException e) {
            System.err.println("\nClient error: " + e.getMessage());
        }
    }

    private static void sendFile(DataOutputStream out, BufferedReader reader) {
        try {
            System.out.println("Enter file path to send: ");
            String filePath = reader.readLine();
            File file = new File(filePath);
            if (file.exists()) {
                out.writeUTF(file.getName());
                out.writeLong(file.length());
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    System.out.println("File sent: " + file.getName());
                }
            } else {
                System.err.println("File not found: " + filePath);
            }
        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
        }
    }

    private static void requestFile(DataOutputStream out, DataInputStream in, BufferedReader reader) {
        try {
            System.out.println("Enter file name to request: ");
            String fileName = reader.readLine();
            out.writeUTF(fileName);
            out.flush();

            String responseFileName = in.readUTF();
            long fileSize = in.readLong();

            if (fileSize > 0) {
                File file = new File("client_" + responseFileName);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int read;
                    while (totalRead < fileSize && (read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                    System.out.println("Received file: " + file.getName());
                }
            } else {
                System.err.println("File not found on server: " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Error requesting file: " + e.getMessage());
        }
    }

    private static void viewFileList(DataInputStream in) {
        try {
            String file;
            while (!(file = in.readUTF()).equals("END")) {
                System.out.println(file);
            }
        } catch (IOException e) {
            System.err.println("Error receiving file list: " + e.getMessage());
        }
    }
}