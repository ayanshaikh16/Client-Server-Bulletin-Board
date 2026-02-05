import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ManualClient {
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 4554;

        // Optional: allow args: java ManualClient <host> <port>
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);

        try (
            Socket socket = new Socket(host, port);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in));
        ) {
            // Read handshake line
            String handshake = serverIn.readLine();
            System.out.println("SERVER: " + handshake);

            while (true) {
                System.out.print("YOU> ");
                String line = userIn.readLine();
                if (line == null) break;

                serverOut.println(line);

                String resp = serverIn.readLine();
                if (resp == null) {
                    System.out.println("SERVER closed connection.");
                    break;
                }

                System.out.println("SERVER: " + resp);

                if (line.trim().equalsIgnoreCase("DISCONNECT")){
                    
                    System.out.println("Disconnected successfully.");
                    break;
                }
                    
            }

        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
            //e.printStackTrace();
        }
    }
}

