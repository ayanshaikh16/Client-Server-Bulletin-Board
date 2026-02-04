import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Board{
    private final int boardWidth;
    private final int boardHeight;
    private final int noteWidth;
    private final int noteHeight;
    private final Set<String> validcolors;

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
}

