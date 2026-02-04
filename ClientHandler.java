import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;


public class ClientHandler extends Thread{
    private final Socket socket;
    private final Board board;
    public ClientHandler(Socket socket, Board board){
        this.socket = socket;
        this.board = board;
    }

    @Override
    public void run(){
        try(
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ){
            Set<String> colors = board.getValidColors();
            StringBuilder sb = new StringBuilder();
            sb.append("DATA INIT ")
            .append(board.getBoardWidth()).append(" ")
            .append(board.getBoardHeight()).append(" ")
            .append(board.getNoteWidth()).append(" ")
            .append(board.getNoteHeight()).append(" ")
            .append(colors.size());

            for(String c : colors) sb.append(" ").append(c);
            out.println(sb.toString());

            String line = in.readLine();
            if(line != null){
                out.println("ERROR INVALID_COMMAND Server not implemented yet");
            }
        }catch (IOException e){
            System.out.println("ClientHandler IO error: " + e.getMessage());
        }finally{
            try {socket.close();}
            catch(IOException ignored){}
        }
    }
}


