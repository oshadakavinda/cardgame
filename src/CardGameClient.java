import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;


public class CardGameClient extends JFrame {
    private JPanel scorePanel, cardsPanel;
    private JTextArea logArea;
    private List<JButton> cardButtons = new ArrayList<>();
    private PrintWriter out;
    private int playerNumber;
    private JLabel[] scoreLabels = new JLabel[3];

    public CardGameClient(String serverAddress, int port) {
        setTitle("Card Game Client");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Score panel
        scorePanel = new JPanel(new GridLayout(1, 3));
        for (int i = 0; i < 3; i++) {
            scoreLabels[i] = new JLabel("Player " + (i+1) + ": 0", SwingConstants.CENTER);
            scorePanel.add(scoreLabels[i]);
        }
        add(scorePanel, BorderLayout.NORTH);

        // Cards panel
        cardsPanel = new JPanel(new FlowLayout());
        add(new JScrollPane(cardsPanel), BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(5, 20);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        try {
            Socket socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new ServerListener(socket)).start();
        } catch (IOException e) {
            logArea.append("Connection error: " + e.getMessage() + "\n");
        }

        setVisible(true);
    }

    private void updateScores(String[] scores) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
                scoreLabels[i].setText("Player " + (i+1) + ": " + scores[i]);
            }
        });
    }

    private class ServerListener implements Runnable {
        private BufferedReader in;

        public ServerListener(Socket socket) throws IOException {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CARD:")) {
                        String card = message.substring(5);
                        addCardButton(card);
                    } else if (message.startsWith("PLAYED:")) {
                        String[] parts = message.split(":");
                        logArea.append("Player " + parts[1] + " played " + parts[2] + "\n");
                    } else if (message.startsWith("SCORES:")) {
                        String[] scores = message.substring(7).split(",");
                        updateScores(scores);
                    } else if (message.equals("YOUR_TURN")) {
                        enableCards(true);
                        logArea.append("It's your turn! Select a card to play.\n");
                    }
                }
            } catch (IOException e) {
                logArea.append("Connection lost: " + e.getMessage() + "\n");
            }
        }
    }

    private void addCardButton(String card) {
        JButton button = new JButton(card);
        button.setEnabled(false);
        button.addActionListener(e -> {
            out.println("PLAY:" + card);
            cardsPanel.remove(button);
            cardsPanel.revalidate();
            cardsPanel.repaint();
        });
        cardButtons.add(button);
        cardsPanel.add(button);
        cardsPanel.revalidate();
    }

    private void enableCards(boolean enable) {
        SwingUtilities.invokeLater(() -> {
            for (JButton button : cardButtons) {
                button.setEnabled(enable);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CardGameClient("localhost", 12345));
    }
}