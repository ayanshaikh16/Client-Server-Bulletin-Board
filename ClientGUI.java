import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientGUI extends JFrame {

    // UI
    private final JTextField hostField = new JTextField("127.0.0.1", 12);
    private final JTextField portField = new JTextField("4554", 6);
    private final JButton connectBtn = new JButton("Connect");
    private final JButton disconnectBtn = new JButton("Disconnect");

    // Auto refresh controls
    private final JCheckBox autoRefreshBox = new JCheckBox("Auto Refresh", true);
    private Timer autoRefreshTimer; // Swing timer

    private final BoardPanel boardPanel = new BoardPanel();

    private final JTextArea outputArea = new JTextArea(12, 80);

    // POST
    private final JTextField postX = new JTextField(4);
    private final JTextField postY = new JTextField(4);
    private final JTextField postColor = new JTextField(8);
    private final JTextField postMsg = new JTextField(28);
    private final JButton postBtn = new JButton("POST");

    // GET
    private final JTextField getColor = new JTextField(8);
    private final JTextField getContainsX = new JTextField(4);
    private final JTextField getContainsY = new JTextField(4);
    private final JTextField getRefersTo = new JTextField(10);
    private final JButton getBtn = new JButton("GET");
    private final JButton getPinsBtn = new JButton("GET PINS");
    private final JButton refreshBtn = new JButton("Refresh (GET + PINS)");

    // PIN/UNPIN
    private final JTextField pinX = new JTextField(4);
    private final JTextField pinY = new JTextField(4);
    private final JButton pinBtn = new JButton("PIN");

    private final JTextField unpinX = new JTextField(4);
    private final JTextField unpinY = new JTextField(4);
    private final JButton unpinBtn = new JButton("UNPIN");

    // Ops
    private final JButton clearBtn = new JButton("CLEAR");
    private final JButton shakeBtn = new JButton("SHAKE");

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Object ioLock = new Object();

    public ClientGUI() {
        super("CP372 Bulletin Board Client (Visual Notes + Pins)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Host:"));
        top.add(hostField);
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(connectBtn);
        top.add(disconnectBtn);
        top.add(autoRefreshBox);

        disconnectBtn.setEnabled(false);

        JPanel postPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        postPanel.setBorder(BorderFactory.createTitledBorder("POST"));
        postPanel.add(new JLabel("x"));
        postPanel.add(postX);
        postPanel.add(new JLabel("y"));
        postPanel.add(postY);
        postPanel.add(new JLabel("color"));
        postPanel.add(postColor);
        postPanel.add(new JLabel("msg"));
        postPanel.add(postMsg);
        postPanel.add(postBtn);

        JPanel getPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        getPanel.setBorder(BorderFactory.createTitledBorder("GET"));
        getPanel.add(new JLabel("color="));
        getPanel.add(getColor);
        getPanel.add(new JLabel("contains x"));
        getPanel.add(getContainsX);
        getPanel.add(new JLabel("y"));
        getPanel.add(getContainsY);
        getPanel.add(new JLabel("refersTo="));
        getPanel.add(getRefersTo);
        getPanel.add(getBtn);
        getPanel.add(getPinsBtn);
        getPanel.add(refreshBtn);

        JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pinPanel.setBorder(BorderFactory.createTitledBorder("PIN / UNPIN"));
        pinPanel.add(new JLabel("PIN x"));
        pinPanel.add(pinX);
        pinPanel.add(new JLabel("y"));
        pinPanel.add(pinY);
        pinPanel.add(pinBtn);

        pinPanel.add(Box.createHorizontalStrut(18));

        pinPanel.add(new JLabel("UNPIN x"));
        pinPanel.add(unpinX);
        pinPanel.add(new JLabel("y"));
        pinPanel.add(unpinY);
        pinPanel.add(unpinBtn);

        JPanel opsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        opsPanel.setBorder(BorderFactory.createTitledBorder("Operations"));
        opsPanel.add(clearBtn);
        opsPanel.add(shakeBtn);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(postPanel);
        controls.add(getPanel);
        controls.add(pinPanel);
        controls.add(opsPanel);

        JScrollPane outScroll = new JScrollPane(outputArea);

        JPanel center = new JPanel(new BorderLayout());
        center.add(boardPanel, BorderLayout.CENTER);
        center.add(controls, BorderLayout.SOUTH);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(top, BorderLayout.NORTH);
        cp.add(center, BorderLayout.CENTER);
        cp.add(outScroll, BorderLayout.SOUTH);

        // Actions
        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());

        postBtn.addActionListener(e -> doPost());
        getPinsBtn.addActionListener(e -> sendAndHandleAsync("GET PINS", true));
        refreshBtn.addActionListener(e -> refreshBoth(true));
        getBtn.addActionListener(e -> doGet());

        pinBtn.addActionListener(e -> doPin());
        unpinBtn.addActionListener(e -> doUnpin());

        clearBtn.addActionListener(e -> { sendAndHandleAsync("CLEAR", true); refreshBoth(true); });
        shakeBtn.addActionListener(e -> { sendAndHandleAsync("SHAKE", true); refreshBoth(true); });

        setConnectedUI(false);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setConnectedUI(boolean connected) {
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);

        postBtn.setEnabled(connected);
        getBtn.setEnabled(connected);
        getPinsBtn.setEnabled(connected);
        refreshBtn.setEnabled(connected);
        pinBtn.setEnabled(connected);
        unpinBtn.setEnabled(connected);
        clearBtn.setEnabled(connected);
        shakeBtn.setEnabled(connected);
        autoRefreshBox.setEnabled(connected);
    }

    private void log(String s) {
        outputArea.append(s + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            log("CLIENT: Invalid port.");
            return;
        }

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String init = in.readLine();
            log("SERVER: " + init);
            handleInit(init);

            setConnectedUI(true);
            log("CLIENT: Connected.");

            // Start auto refresh
            startAutoRefresh();

            // initial refresh
            refreshBoth(false);

        } catch (IOException ex) {
            log("CLIENT: Connection failed: " + ex.getMessage());
            cleanup();
            setConnectedUI(false);
        }
    }

    private void disconnect() {
        stopAutoRefresh();
        sendAndHandleAsync("DISCONNECT", true);
        cleanup();
        setConnectedUI(false);
        log("CLIENT: Disconnected.");
    }

    private void startAutoRefresh() {
        stopAutoRefresh(); // safety
        autoRefreshTimer = new Timer(1000, e -> {
            if (autoRefreshBox.isSelected()) {
                refreshBoth(false); // silent refresh by default
            }
        });
        autoRefreshTimer.start();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
        }
    }

    private void cleanup() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        socket = null;
        in = null;
        out = null;
    }

    private void refreshBoth(boolean logRequests) {
        // If your server supports plain GET = all notes, this is perfect.
        sendAndHandleAsync("GET", logRequests);
        sendAndHandleAsync("GET PINS", logRequests);
    }

    private void sendAndHandleAsync(String cmd, boolean logRequests) {
        new Thread(() -> sendAndHandle(cmd, logRequests)).start();
    }

    private void sendAndHandle(String cmd, boolean logRequests) {
        if (out == null || in == null) {
            if (logRequests) log("CLIENT: Not connected.");
            return;
        }

        synchronized (ioLock) {
            try {
                if (logRequests) log("YOU> " + cmd);
                out.println(cmd);

                String resp = in.readLine();
                if (resp == null) {
                    if (logRequests) log("SERVER: (connection closed)");
                    cleanup();
                    SwingUtilities.invokeLater(() -> setConnectedUI(false));
                    return;
                }

                if (logRequests) log("SERVER: " + resp);

                if (resp.startsWith("DATA INIT")) {
                    handleInit(resp);
                } else if (resp.startsWith("DATA NOTES")) {
                    List<BoardPanel.NoteView> notes = parseDataNotes(resp);
                    boardPanel.setNotes(notes);
                } else if (resp.startsWith("DATA PINS")) {
                    List<Point> pins = parseDataPins(resp);
                    boardPanel.setPins(pins);
                }

            } catch (IOException ex) {
                if (logRequests) log("CLIENT: IO error: " + ex.getMessage());
                cleanup();
                SwingUtilities.invokeLater(() -> setConnectedUI(false));
            }
        }
    }

    // ---- UI Actions ----

    private void doPost() {
        String x = postX.getText().trim();
        String y = postY.getText().trim();
        String c = postColor.getText().trim();
        String m = postMsg.getText().trim();

        if (x.isEmpty() || y.isEmpty() || c.isEmpty() || m.isEmpty()) {
            log("CLIENT: POST requires x, y, color, message.");
            return;
        }

        sendAndHandleAsync("POST " + x + " " + y + " " + c + " " + m, true);
        refreshBoth(false);
    }

    private void doGet() {
        StringBuilder cmd = new StringBuilder("GET");

        String c = getColor.getText().trim();
        String cx = getContainsX.getText().trim();
        String cy = getContainsY.getText().trim();
        String ref = getRefersTo.getText().trim();

        if (!c.isEmpty()) cmd.append(" ").append("color=").append(c);

        if (!cx.isEmpty() || !cy.isEmpty()) {
            if (cx.isEmpty() || cy.isEmpty()) {
                log("CLIENT: GET contains requires both x and y.");
                return;
            }
            cmd.append(" ").append("contains=").append(cx).append(" ").append(cy);
        }

        if (!ref.isEmpty()) cmd.append(" ").append("refersTo=").append(ref);

        sendAndHandleAsync(cmd.toString(), true);
        sendAndHandleAsync("GET PINS", false);
    }

    private void doPin() {
        String x = pinX.getText().trim();
        String y = pinY.getText().trim();
        if (x.isEmpty() || y.isEmpty()) {
            log("CLIENT: PIN requires x and y.");
            return;
        }
        sendAndHandleAsync("PIN " + x + " " + y, true);
        refreshBoth(false);
    }

    private void doUnpin() {
        String x = unpinX.getText().trim();
        String y = unpinY.getText().trim();
        if (x.isEmpty() || y.isEmpty()) {
            log("CLIENT: UNPIN requires x and y.");
            return;
        }
        sendAndHandleAsync("UNPIN " + x + " " + y, true);
        refreshBoth(false);
    }

    // ---- Parsing ----

    private void handleInit(String initLine) {
        try {
            String[] p = initLine.trim().split("\\s+");
            if (p.length >= 7 && p[0].equals("DATA") && p[1].equals("INIT")) {
                int bw = Integer.parseInt(p[2]);
                int bh = Integer.parseInt(p[3]);
                int nw = Integer.parseInt(p[4]);
                int nh = Integer.parseInt(p[5]);
                boardPanel.setBoardConfig(bw, bh, nw, nh);
            }
        } catch (Exception ignored) {}
    }

    private List<Point> parseDataPins(String resp) {
        List<Point> list = new ArrayList<>();
        String[] parts = resp.trim().split("\\s+");
        if (parts.length < 3) return list;
        if (!parts[0].equals("DATA") || !parts[1].equals("PINS")) return list;

        int k;
        try { k = Integer.parseInt(parts[2]); }
        catch (NumberFormatException e) { return list; }

        int i = 3;
        for (int c = 0; c < k && i + 1 < parts.length; c++) {
            try {
                int x = Integer.parseInt(parts[i++]);
                int y = Integer.parseInt(parts[i++]);
                list.add(new Point(x, y));
            } catch (NumberFormatException e) {
                break;
            }
        }
        return list;
    }

    private List<BoardPanel.NoteView> parseDataNotes(String resp) {
        List<BoardPanel.NoteView> list = new ArrayList<>();
        String[] parts = resp.trim().split("\\s+");
        if (parts.length < 3) return list;
        if (!parts[0].equals("DATA") || !parts[1].equals("NOTES")) return list;

        int k;
        try { k = Integer.parseInt(parts[2]); }
        catch (NumberFormatException e) { return list; }

        int i = 3;
        for (int noteIdx = 0; noteIdx < k; noteIdx++) {
            if (i + 5 > parts.length) break;

            int x, y, pinned, msgLen;
            String color;

            try {
                x = Integer.parseInt(parts[i++]);
                y = Integer.parseInt(parts[i++]);
                color = parts[i++];
                pinned = Integer.parseInt(parts[i++]);
                msgLen = Integer.parseInt(parts[i++]);
            } catch (Exception ex) {
                break;
            }

            StringBuilder msg = new StringBuilder();
            int chars = 0;

            while (i < parts.length && chars < msgLen) {
                String t = parts[i];
                if (msg.length() > 0) { msg.append(" "); chars += 1; }
                msg.append(t);
                chars += t.length();
                i++;
            }

            list.add(new BoardPanel.NoteView(x, y, color, pinned, msg.toString()));
        }

        return list;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
