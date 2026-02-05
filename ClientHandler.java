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

            out.println(buildInitLine());




            String line;
            while((line = in.readLine()) != null){
                line = line.trim();
                if(line.isEmpty()){
                    out.println("ERROR INVALID_FORMAT Empty request");
                    continue;
                }

                String[] parts = line.split("\\s+");
                String command = parts[0].toUpperCase();

                if(command.equals("DISCONNECT")){
                    out.println("OK");
                    break;
                }
                else if(command.equals("CLEAR")){
                    board.clear();
                    out.println("OK");
                }
                else if(command.equals("SHAKE")){
                    board.shake();
                    out.println("OK");
                }
                else if(command.equals("POST")){
                    if(parts.length < 5){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "POST required x y color message"));
                        continue;
                    }

                    try{
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);
                        String color = parts[3];

                        int index = nthIndexOf(line, ' ', 4);
                        if(index == -1 || index + 1 >= line.length()){
                            out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "POST missing message"));
                            continue;
                        }
                        String message = line.substring(index + 1);
                        board.post(x, y, color, message);
                        out.println(Protocol.ok());
                    }
                    catch(NumberFormatException e){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "x and y must be integers"));
                    }
                    catch(ProtocolException e){
                        out.println(Protocol.error(e.getCode(), e.getMessage()));
                    }
                }

                else{
                    out.println("ERROR INVALID_COMMAND Unknown command");
                }

            }

        }catch (IOException e){
            System.out.println("ClientHandler IO error: " + e.getMessage());
        }finally{
            try {
                socket.close();
            }
            catch(IOException ignored){}
        }
    }

    private int nthIndexOf(String str, char ch, int n){
        int count = 0;
        for(int i = 0; i < str.length(); i++){
            if(str.charAt(i) == ch){
                count++;
                if(count == n) return i;
            }
        }
        return -1;
    }

    private String buildInitLine(){
        StringBuilder sb = new StringBuilder();
        sb.append("DATA INIT ")
        .append(board.getBoardWidth()).append(" ")
        .append(board.getBoardHeight()).append(" ")
        .append(board.getNoteWidth()).append(" ")
        .append(board.getNoteHeight()).append(" ")
        .append(board.getValidColors().size());

        for(String c : board.getValidColors()) sb.append(" ").append(c);
        return sb.toString();
    }
}


