import java.util.*;

public class Board{
    private final int boardWidth;
    private final int boardHeight;
    private final int noteWidth;
    private final int noteHeight;
    private final Set<String> validcolors;

    private final List<Note> notes = new ArrayList<>();

    // pins by coordinate "x,y" -> count
    private final Map<String, Integer> pinCountsAtCoord = new HashMap<>();

    public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, Set<String> colors){
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.noteWidth = noteWidth;
        this.noteHeight = noteHeight;
        this.validcolors = new HashSet<>(colors);
    }

    public int getBoardWidth(){ return boardWidth; }
    public int getBoardHeight(){ return boardHeight; }
    public int getNoteWidth(){ return noteWidth; }
    public int getNoteHeight(){ return noteHeight; }

    public Set<String> getValidColors(){
        return Collections.unmodifiableSet(validcolors);
    }

    private String key(int x, int y){
        return x + "," + y;
    }

    public synchronized void clear(){
        notes.clear();
        pinCountsAtCoord.clear();
    }

    // simple shake: remove unpinned notes; keep pins map as-is
    public synchronized void shake(){
        notes.removeIf(n -> !n.isPinned());
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
                found = true;
                break;
            }
        }
        if(!found){
            throw new ProtocolException(ErrorCode.NO_NOTE_AT_COORDINATE, "No note at given coordinate");
        }

        String k = key(x, y);

        // pin affects all notes containing the point
        for(Note n : notes){
            if(n.contains(x, y, noteWidth, noteHeight)){
                n.addPin();
            }
        }

        pinCountsAtCoord.put(k, pinCountsAtCoord.getOrDefault(k, 0) + 1);
    }

    public synchronized void unpin(int x, int y) throws ProtocolException{
        String k = key(x, y);
        Integer count = pinCountsAtCoord.get(k);

        if(count == null || count == 0){
            throw new ProtocolException(ErrorCode.PIN_NOT_FOUND, "No pin at given coordinate");
        }

        if(count == 1) pinCountsAtCoord.remove(k);
        else pinCountsAtCoord.put(k, count - 1);

        // remove pin from notes containing point
        for(Note n : notes){
            if(n.contains(x, y, noteWidth, noteHeight)){
                n.removePin();
            }
        }
    }

    // single-line response: DATA PINS <k> x1 y1 x2 y2 ...
    public synchronized String getPinsResponse(){
        int totalPins = 0;
        for(int c : pinCountsAtCoord.values()) totalPins += c;

        StringBuilder sb = new StringBuilder();
        sb.append("DATA PINS ").append(totalPins);

        // stable ordering (optional)
        List<String> keys = new ArrayList<>(pinCountsAtCoord.keySet());
        keys.sort((a,b)->{
            String[] A=a.split(","), B=b.split(",");
            int ax=Integer.parseInt(A[0]), ay=Integer.parseInt(A[1]);
            int bx=Integer.parseInt(B[0]), by=Integer.parseInt(B[1]);
            if(ax!=bx) return Integer.compare(ax,bx);
            return Integer.compare(ay,by);
        });

        for(String coordKey : keys){
            int count = pinCountsAtCoord.get(coordKey);
            String[] xy = coordKey.split(",");
            int px = Integer.parseInt(xy[0]);
            int py = Integer.parseInt(xy[1]);

            for(int i=0;i<count;i++){
                sb.append(" ").append(px).append(" ").append(py);
            }
        }

        return sb.toString();
    }

    // single-line response: DATA NOTES <k> x y color pinned msgLen message ...
    public synchronized String getNotesResponse(String colorFilter, Integer containsX, Integer containsY, String refersTo){
        List<Note> matches = new ArrayList<>();

        for(Note n : notes){
            if(colorFilter != null && !n.getColor().equalsIgnoreCase(colorFilter)) continue;

            if(containsX != null && containsY != null){
                if(!n.contains(containsX, containsY, noteWidth, noteHeight)) continue;
            }

            if(refersTo != null){
                String msg = n.getMessage() == null ? "" : n.getMessage();
                if(!msg.toLowerCase().contains(refersTo.toLowerCase())) continue;
            }

            matches.add(n);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DATA NOTES ").append(matches.size());

        for(Note n : matches){
            String msg = n.getMessage() == null ? "" : n.getMessage();
            int pinned = n.isPinned() ? 1 : 0;
            sb.append(" ")
              .append(n.getX()).append(" ")
              .append(n.getY()).append(" ")
              .append(n.getColor()).append(" ")
              .append(pinned).append(" ")
              .append(msg.length()).append(" ")
              .append(msg);
        }

        return sb.toString();
    }
}
