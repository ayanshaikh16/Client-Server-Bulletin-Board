import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;



public class Bboard{
    
    private static void usageAndExit(){
        System.out.println("Usage:");
        System.out.println("  java Bboard <port> <board_width> <board_height> <note_width> <note_height> <color1> <color2>");
        System.out.println("Example:");
        System.out.println("  java Bboard 4554 200 100 20 10 white green yellow");
        System.exit(1);
    }

    private static int parsePositiveInt(String s, String name){
        try{ 
            int v = Integer.parseInt(s);
            if(v <= 0){
                System.out.println("ERROR: " + name + " must be > 0");
                usageAndExit();
            }
            return v;

        }catch (NumberFormatException e){
            System.out.println("ERROR: " + name + "must be an integer");
            usageAndExit();
            return -1; // Unreachable
        }
    }

    public static void main(String[] args){
        if(args.length < 6){
            usageAndExit();
        }
        int port = parsePositiveInt(args[0], "port");
        int boardWidth = parsePositiveInt(args[1], "board_width");
        int boardHeight = parsePositiveInt(args[2], "board_height");
        int noteWidth = parsePositiveInt(args[3], "note_width");
        int noteHeight = parsePositiveInt(args[4], "note_height");

        Set<String> colors = new HashSet<>();
        for(int i = 5; i < args.length; i++){
            String c = args[i].trim();
            if (!c.isEmpty()){
                colors.add(c);
            }
            
        }
        if(colors.isEmpty()){
                System.out.println("ERROR: At least one color must be provided.");
                usageAndExit();
        }

        Board board = new Board(boardWidth, boardHeight, noteWidth, noteHeight, colors);

        System.out.println("Bboard server starting...");
        System.out.println("Port: " + port);
        System.out.println("Board: " + boardWidth + " x " + boardHeight);
        System.out.println("Note: " + noteWidth + " x " + noteHeight);
        System.out.println("Colors: " + Arrays.toString(colors.toArray()));

        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Server listening on port" + port);

            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                ClientHandler handler = new ClientHandler(clientSocket, board);
                handler.start();
            }
        } catch (IOException e){
            System.out.println("ERROR: Could not start server on port " + port);
            e.printStackTrace();
        }
    }
}


