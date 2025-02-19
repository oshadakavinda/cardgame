import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class CardGameClient extends JFrame {
    private static final String SERVER_IP = "192.168.1.9"; // Replace with your server's IP
    private static final int SERVER_PORT = 12345;

    private JPanel scorePanel, cardsPanel;
    private JTextArea logArea;
    private List<Card> cardObjects = new ArrayList<>();
    private PrintWriter out;
    private JLabel[] scoreLabels = new JLabel[3];
    private JButton restartButton;

    public CardGameClient() {
        setTitle("Card Game Client");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Score Panel
        scorePanel = new JPanel(new GridLayout(1, 3));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Scores"));
        for (int i = 0; i < 3; i++) {
            scoreLabels[i] = new JLabel("Player " + (i + 1) + ": 0", SwingConstants.CENTER);
            scoreLabels[i].setFont(new Font("Arial", Font.BOLD, 14));
            scorePanel.add(scoreLabels[i]);
        }
        add(scorePanel, BorderLayout.NORTH);

        // Cards Panel
        cardsPanel = new JPanel(new FlowLayout());
        cardsPanel.setBackground(Color.DARK_GRAY);
        add(new JScrollPane(cardsPanel), BorderLayout.CENTER);

        // Log Area
        logArea = new JTextArea(5, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        // Restart Button
        restartButton = new JButton("Restart Game");
        restartButton.setFont(new Font("Arial", Font.BOLD, 14));
        restartButton.setForeground(Color.WHITE);
        restartButton.setBackground(Color.RED);
        restartButton.setVisible(false);
        restartButton.addActionListener(e -> {
            out.println("RESTART");
            restartButton.setVisible(false);
            cardsPanel.removeAll();
            cardObjects.clear();
            cardsPanel.revalidate();
            cardsPanel.repaint();
        });
        add(restartButton, BorderLayout.EAST);

        // Connect to Server
        try {
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            new Thread(new ServerListener(socket)).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Handle Window Close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (out != null) {
                    out.println("QUIT");
                    out.close();
                }
            }
        });

        setVisible(true);
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
                    String finalMessage = message;
                    SwingUtilities.invokeLater(() -> processMessage(finalMessage));
                }
            } catch (IOException e) {
                logArea.append("Connection lost: " + e.getMessage() + "\n");
            }
        }
    }

    private void processMessage(String message) {
        if (message.startsWith("CARD:")) {
            addCard(message.substring(5));
        } else if (message.startsWith("PLAYED:")) {
            String[] parts = message.split(":");
            logArea.append("Player " + parts[1] + " played " + parts[2] + "\n");
            enableCards(false);
        } else if (message.startsWith("SCORES:")) {
            updateScores(message.substring(7).split(","));
        } else if (message.equals("YOUR_TURN")) {
            enableCards(true);
            logArea.append("It's your turn! Select a card to play.\n");
        } else if (message.startsWith("FINAL_SCORES:")) {
            showGameOver(message.substring(13));
        }
    }

    private void addCard(String cardData) {
        String[] parts = cardData.split(" of ");
        Card card = new Card(parts[1], parts[0]); // suit first, then value

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                out.println("PLAY:" + cardData);
                cardsPanel.remove(card);
                cardsPanel.revalidate();
                cardsPanel.repaint();
                enableCards(false);
            }
        });

        cardObjects.add(card);
        cardsPanel.add(card);
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void updateScores(String[] scores) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < scores.length; i++) {
                scoreLabels[i].setText("Player " + (i + 1) + ": " + scores[i]);
            }
        });
    }

    private void showGameOver(String finalScores) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("Game Over! Final Scores: " + finalScores + "\n");
            restartButton.setVisible(true);
        });
    }

    private void enableCards(boolean enable) {
        SwingUtilities.invokeLater(() -> {
            for (Card card : cardObjects) {
                card.setEnabled(enable);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CardGameClient::new);
    }
}
