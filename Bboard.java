import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Bboard{
    private static void usageAndExit(){
        System.out.println("Usage: java Bboard <port> <boardW> <boardH> <noteW> <noteH> <colors...>");
        System.out.println("Example: java Bboard 4554 200 100 20 10 red green white yellow");
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
        }catch(NumberFormatException e){
            System.out.println("ERROR: " + name + " must be an integer");
            usageAndExit();
            return -1;
        }
    }

    public static void main(String[] args){
        if(args.length < 6) usageAndExit();

        int port = parsePositiveInt(args[0], "port");
        int boardW = parsePositiveInt(args[1], "board_width");
        int boardH = parsePositiveInt(args[2], "board_height");
        int noteW  = parsePositiveInt(args[3], "note_width");
        int noteH  = parsePositiveInt(args[4], "note_height");

        Set<String> colors = new HashSet<>();
        for(int i=5;i<args.length;i++){
            String c = args[i].trim();
            if(!c.isEmpty()) colors.add(c);
        }
        if(colors.isEmpty()){
            System.out.println("ERROR: At least one color must be provided.");
            usageAndExit();
        }

        Board board = new Board(boardW, boardH, noteW, noteH, colors);

        System.out.println("Bboard server starting...");
        System.out.println("Port: " + port);
        System.out.println("Board: " + boardW + " x " + boardH);
        System.out.println("Note: " + noteW + " x " + noteH);
        System.out.println("Colors: " + Arrays.toString(colors.toArray()));

        try(ServerSocket serverSocket = new ServerSocket(port)){
            System.out.println("Server listening on port " + port);

            while(true){
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, board);
                handler.start();
            }
        }catch(IOException e){
            System.out.println("ERROR: Could not start server on port " + port);
            e.printStackTrace();
        }
    }
}
