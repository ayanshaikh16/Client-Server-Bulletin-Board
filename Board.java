
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;


public class Board{
    private final int boardWidth;
    private final int boardHeight;
    private final int noteWidth;
    private final int noteHeight;
    private final Set<String> validcolors;

    private final List<Note> notes = new ArrayList<>();
    private final Map<String, Integer> pinCountsAtCoord = new HashMap<>();

    private String key(int x, int y){
        return x + "," + y;
    }

   public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, Set<String> colors){
    this.boardHeight = boardHeight;
    this.boardWidth = boardWidth;
    this.noteWidth = noteWidth;
    this.noteHeight = noteHeight;
    this.validcolors = new HashSet<>(colors);
   }

   public int getBoardWidth(){
    return boardWidth;
   }
   public int getBoardHeight(){
    return boardHeight;
   }
   public int getNoteWidth(){
    return noteWidth;
   }
    public int getNoteHeight(){
     return noteHeight;
    }

    public Set<String> getValidColors(){
        return Collections.unmodifiableSet(validcolors);
    }

    public synchronized void clear(){
        notes.clear();
        pinCountsAtCoord.clear();
    }

    public synchronized void shake(){
        notes.clear();
        pinCountsAtCoord.clear();
    }

    public synchronized void post(int x, int y, String color, String message) throws ProtocolException{
        if(!validcolors.contains(color)){
            throw new ProtocolException(ErrorCode.COLOR_NOT_SUPPORTED, "Unsupported color");
        }
        Note newNote = new Note(x, y, color, message);

        if(!newNote.fitsInBoard(boardWidth, boardHeight, noteWidth, noteHeight)){
            throw new ProtocolException(ErrorCode.OUT_OF_BOUNDS, "Note out of bounds");
        }

        for(Note existing : notes){
            if(newNote.completelyOverlaps(existing)){
                throw new ProtocolException(ErrorCode.COMPLETE_OVERLAP, "Cannot completely overlap existing note");
            }
        }
        notes.add(newNote);
    }

    public synchronized void pin(int x, int y) throws ProtocolException{
        boolean found = false;

        for(Note n : notes){
            if(n.contains(x, y, noteWidth, noteHeight)){
                n.addPin();
                found = true;
            }
        }
        if(!found){
            throw new ProtocolException(ErrorCode.NO_NOTE_AT_COORDINATE, "No note at given coordinate");
        }
        String key = key(x, y);
        pinCountsAtCoord.put(key, pinCountsAtCoord.getOrDefault(key, 0) + 1);
    }

    public synchronized void unpin(int x, int y) throws ProtocolException{
        String key = key(x, y);
        Integer count = pinCountsAtCoord.get(key);

        if(count == null || count ==0){
            throw new ProtocolException(ErrorCode.PIN_NOT_FOUND, "No pin at given coordinate");
        }

        if(count == 1) pinCountsAtCoord.remove(key);
        else pinCountsAtCoord.put(key, count - 1);

        for(Note n : notes){
            if(n.contains(x, y, noteWidth, noteHeight)){
                n.removePin();
            }
        }
    }
}



