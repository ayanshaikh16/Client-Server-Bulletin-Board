public class Note{
    private int x;
    private final int y;
    private final String color;
    private String message;

    private int pinCount = 0;

    public Note(int x, int y, String color, String message){
        this.x = x;
        this.y = y;
        this.color = color;
        this.message = message;
    }

    public int getX(){
        return x;
    }
    public int getY(){
        return y;
    }
    public String getColor(){
        return color;
    }
    public String getMessage(){
        return message;
    }

    public boolean isPinned(){
        return pinCount >0;
    }

    public void addPin(){
        pinCount++;
    }

    public boolean removePin(){
        if(pinCount >0){
            pinCount--;
            return true;
        }
        return false;
    }

    public boolean fitsInBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight){
        return x >= 0 && y >= 0 && (x + noteWidth) <= boardWidth && (y + noteHeight) <= boardHeight;
    }

    public boolean completelyOverlaps(Note other){
        return this.x == other.x && this.y == other.y;
    }

    public boolean contains(int px, int py, int noteWidth, int noteHeight){
        return px >= x && px < (x + noteWidth) && py >= y && py < (y + noteHeight);
    }
}