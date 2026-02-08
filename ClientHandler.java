import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;



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

                else if(command.equals("PIN")){
                    if(parts.length != 3){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "PIN requires x y"));
                        continue;
                    }

                    try{
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        board.pin(x,y);
                        out.println(Protocol.ok());
                    }
                    catch(NumberFormatException e){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "x and y must be integers"));
                    }
                    catch(ProtocolException e){
                        out.println(Protocol.error(e.getCode(), e.getMessage()));
                    }
                    
                }

                else if(command.equals("UNPIN")){
                    if(parts.length != 3){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "UNPIN requires x y"));
                        continue;
                    } 

                    try{
                        int x = Integer.parseInt(parts[1]);
                        int y = Integer.parseInt(parts[2]);

                        board.unpin(x, y);
                        out.println(Protocol.ok());
                    }
                    catch(NumberFormatException e){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "x and y must be integers"));
                    }
                    catch(ProtocolException e){
                        out.println(Protocol.error(e.getCode(), e.getMessage()));
                    }
                }

                else if(command.equals("GET")){

                    if(parts.length == 2 && parts[1].equalsIgnoreCase("PINS")){
                        out.println(board.getPinsResponse());
                        continue;
                    }
                    
                    String colorFilter = null;
                    Integer containsX = null;
                    Integer containsY = null;
                    String refersTo = null;

                    try{
                        for(int i = 1; i < parts.length; i++){
                            String tok = parts[i];

                            if(tok.startsWith("color=")){
                                colorFilter = tok.substring(6);
                                if(colorFilter.isEmpty()){
                                    out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "color filter missing value"));
                                    break;
                                }
                            }
                            else if(tok.startsWith("contains=")){
                                String xStr = tok.substring(9);
                                if(xStr.isEmpty()){
                                    out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "contains filter missing x"));
                                    break;
                                }
                                if(i + 1 >=parts.length){
                                    out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "contains filter mssing y"));
                                    break;
                                }
                                containsX = Integer.parseInt(xStr);
                                containsY = Integer.parseInt(parts[i + 1]);
                                i++;
                            }
                            else if(tok.startsWith("refersTo=")){
                                refersTo = tok.substring(9);
                                if(refersTo.isEmpty()){
                                    out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "refersTo filter missing value"));
                                    break;
                                }
                            }
                            else{
                                out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "Unknown GET filter"));
                                break;
                            }
                        }
                        out.println(board.getNotesResponse(colorFilter, containsX, containsY, refersTo));
                    }
                    catch(NumberFormatException e){
                        out.println(Protocol.error(ErrorCode.INVALID_FORMAT, "contains requires integer x and y"));
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


