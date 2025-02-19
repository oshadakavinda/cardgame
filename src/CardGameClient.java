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
    private JLabel[] scoreLabels = new JLabel[3];
    private JButton restartButton;

    public CardGameClient(String serverAddress, int port) {
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
            cardButtons.clear();
            cardsPanel.revalidate();
            cardsPanel.repaint();
        });
        add(restartButton, BorderLayout.EAST);

        // Connect to Server
        try {
            Socket socket = new Socket(serverAddress, port);
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

    private void updateScores(String[] scores) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 3; i++) {
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


        private void processMessage(String message) {
            if (message.startsWith("CARD:")) {
                String card = message.substring(5);
                addCardButton(card);
            } else if (message.startsWith("PLAYED:")) {
                String[] parts = message.split(":");
                logArea.append("Player " + parts[1] + " played " + parts[2] + "\n");
                enableCards(false);
                scorePanel.setBorder(BorderFactory.createEmptyBorder());
            } else if (message.startsWith("SCORES:")) {
                String[] scores = message.substring(7).split(",");
                updateScores(scores);
            } else if (message.equals("YOUR_TURN")) {
                enableCards(true);
                logArea.append("It's your turn! Select a card to play.\n");
                scorePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            } else if (message.startsWith("FINAL_SCORES:")) {
                showGameOver(message.substring(13));
            }
        }
    }

    private class CardButton extends JButton {
        private static final int ARC_SIZE = 15;
        private final String card;
        private ImageIcon cardIcon;

        public CardButton(String card) {
            this.card = card;
            loadCardIcon(card);  // Load the icon based on the card name
            setIcon(cardIcon);
            setPreferredSize(new Dimension(80, 120)); // Card size
            setFont(new Font("Arial", Font.BOLD, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.BLACK, 2),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setBackground(Color.WHITE);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_SIZE, ARC_SIZE);

            if (getBorder() != null) {
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, ARC_SIZE, ARC_SIZE);
            }

            super.paintComponent(g2);
            g2.dispose();
        }

        private void loadCardIcon(String card) {
            // Load a specific card image based on the card value
            String imageName = getCardImageFileName(card);
            cardIcon = new ImageIcon(getClass().getResource("/card_images/" + imageName));
        }

        private String getCardImageFileName(String card) {
            // Extract value and suit from card string
            String value = card.substring(0, card.length() - 1);
            String suit = card.substring(card.length() - 1);

            // Map the suit to its corresponding character
            switch (suit) {
                case "S": return value + "S.jpeg"; // Spades
                case "C": return value + "c.png"; // Clubs
                case "H": return value + "h.png"; // Hearts
                case "D": return value + "d.png"; // Diamonds
                default: return value + "s.jpeg"; // Default to Spades
            }
        }
    }


    private void addCardButton(String card) {
        // Create a Card object instead of a JButton
        Card cardPanel = new Card(card.substring(card.length() - 1), card.substring(0, card.length() - 1)); // Suit, Value
        cardPanel.setPreferredSize(new Dimension(80, 120)); // Set preferred size

        cardPanel.addMouseListener(new MouseAdapter() { // Add mouse listener for clicks
            @Override
            public void mouseClicked(MouseEvent e) {
                if (cardPanel.isEnabled()) { // Only allow clicks if enabled
                    out.println("PLAY:" + card);
                    cardsPanel.remove(cardPanel); // Remove the Card panel
                    cardsPanel.revalidate();
                    cardsPanel.repaint();
                    enableCards(false); // Disable cards after playing
                }
            }
        });

        cardPanel.setEnabled(false); // Initially disabled

        cardsPanel.add(cardPanel); // Add the Card panel
        cardsPanel.revalidate();
    }

    private void enableCards(boolean enable) {
        SwingUtilities.invokeLater(() -> {
            for (Component comp : cardsPanel.getComponents()) { // Iterate through components
                if (comp instanceof Card) { // Check if it's a Card
                    comp.setEnabled(enable);
                    comp.setBackground(enable ? new Color(220, 255, 220) : Color.WHITE);
                }
            }
        });
    }


    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog("Enter server IP address:");
        if (serverAddress == null || serverAddress.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Server address required.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> new CardGameClient(serverAddress, 12345));
    }
}
