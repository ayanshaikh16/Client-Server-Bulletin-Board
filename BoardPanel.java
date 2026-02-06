import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BoardPanel extends JPanel {

    public static class NoteView {
        public int x, y;
        public String color;
        public int pinned; // 0 or 1
        public String message;

        public NoteView(int x, int y, String color, int pinned, String message) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.pinned = pinned;
            this.message = message;
        }
    }

    private int boardW = 200, boardH = 200;
    private int noteW = 20, noteH = 20;

    private final List<NoteView> notes = new ArrayList<>();
    private final List<Point> pins = new ArrayList<>();

    public BoardPanel() {
        setBackground(Color.WHITE);
    }

    public void setBoardConfig(int boardW, int boardH, int noteW, int noteH) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        repaint();
    }

    public void setNotes(List<NoteView> newNotes) {
        notes.clear();
        notes.addAll(newNotes);
        repaint();
    }

    public void setPins(List<Point> newPins) {
        pins.clear();
        pins.addAll(newPins);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(900, 420);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 14;
            int drawW = getWidth() - 2 * pad;
            int drawH = getHeight() - 2 * pad;

            // Board border
            g2.setColor(Color.BLACK);
            g2.drawRect(pad, pad, drawW, drawH);

            if (boardW <= 0 || boardH <= 0) return;

            double sx = (double) drawW / boardW;
            double sy = (double) drawH / boardH;

            // Draw notes (rectangles)
            for (NoteView n : notes) {
                int px = pad + (int) Math.round(n.x * sx);
                int py = pad + (int) Math.round(n.y * sy);
                int pw = Math.max(1, (int) Math.round(noteW * sx));
                int ph = Math.max(1, (int) Math.round(noteH * sy));

                g2.setColor(mapColor(n.color));
                g2.fillRect(px, py, pw, ph);

                g2.setColor(Color.BLACK);
                g2.drawRect(px, py, pw, ph);

                // message label (trim)
                String label = (n.message == null) ? "" : n.message;
                if (label.length() > 18) label = label.substring(0, 18) + "...";
                int textX = px + 4;
                int textY = (ph >= 16) ? (py + ph - 4) : (py + 12);
                g2.drawString(label, textX, textY);
            }

            // Draw pins as BLACK CIRCLES at their actual coordinate positions
            g2.setColor(Color.BLACK);
            int r = 4; // radius
            for (Point p : pins) {
                int px = pad + (int) Math.round(p.x * sx);
                int py = pad + (int) Math.round(p.y * sy);
                g2.fillOval(px - r, py - r, r * 2, r * 2);
            }

            // Legend
            g2.setColor(Color.DARK_GRAY);
            g2.drawString("Board: " + boardW + "x" + boardH + " | Note: " + noteW + "x" + noteH,
                    pad, getHeight() - 6);

        } finally {
            g2.dispose();
        }
    }

    private Color mapColor(String c) {
        if (c == null) return new Color(220, 220, 220);
        String s = c.toLowerCase();
        return switch (s) {
            case "red" -> Color.RED;
            case "green" -> Color.GREEN;
            case "yellow" -> Color.YELLOW;
            case "white" -> Color.WHITE;
            case "blue" -> Color.CYAN;
            default -> new Color(220, 220, 220);
        };
    }
}
