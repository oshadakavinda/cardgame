import javax.swing.*;
import java.awt.*;
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
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Score panel
        scorePanel = new JPanel(new GridLayout(1, 3));
        for (int i = 0; i < 3; i++) {
            scoreLabels[i] = new JLabel("Player " + (i + 1) + " score: 0", SwingConstants.CENTER);
            scorePanel.add(scoreLabels[i]);
        }
        scorePanel.setOpaque(true);
        scorePanel.setBackground(new Color(240, 240, 240));
        add(scorePanel, BorderLayout.NORTH);

        // Cards panel
        cardsPanel = new JPanel(new FlowLayout());
        add(new JScrollPane(cardsPanel), BorderLayout.CENTER);

        // Log area
        logArea = new JTextArea(5, 20);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        // Restart button (initially hidden)
        restartButton = new JButton("Restart Game");
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
                    if (message.startsWith("CARD:")) {
                        String card = message.substring(5);
                        addCardButton(card);
                    } else if (message.startsWith("PLAYED:")) {
                        String[] parts = message.split(":");
                        logArea.append("Player " + parts[1] + " played " + parts[2] + "\n");
                        enableCards(false);
                        SwingUtilities.invokeLater(() -> {
                            scorePanel.setBorder(null);
                        });
                    } else if (message.startsWith("SCORES:")) {
                        String[] scores = message.substring(7).split(",");
                        updateScores(scores);
                    } else if (message.equals("YOUR_TURN")) {
                        enableCards(true);
                        logArea.append("It's your turn! Select a card to play.\n");
                        SwingUtilities.invokeLater(() -> {
                            scorePanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
                        });
                    } else if (message.startsWith("FINAL_SCORES:")) {
                        String finalScores = message.substring(13);
                        showGameOver(finalScores);
                    }
                }
            } catch (IOException e) {
                logArea.append("Connection lost: " + e.getMessage() + "\n");
            }
        }
    }

    private class CardButton extends JButton {
        private static final int ARC_SIZE = 15;
        
        public CardButton(String card) {
            super(getCardEmoji(card));
            setPreferredSize(new Dimension(80, 120));
            setFont(new Font("Dialog", Font.BOLD, 16));
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
            
            // Draw rounded rectangle background
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC_SIZE, ARC_SIZE);
            
            // Draw the border
            if (getBorder() != null) {
                g2.setColor(Color.BLACK);
                g2.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, ARC_SIZE, ARC_SIZE);
            }
            
            super.paintComponent(g2);
            g2.dispose();
        }
        
        private static String getCardEmoji(String card) {
            // Convert card string to emoji representation
            String suit = card.substring(card.length() - 1);
            String value = card.substring(0, card.length() - 1);
            
            String suitEmoji = switch(suit) {
                case "\u2660" -> "\u2660\uFE0F";
                case "\u2663" -> "\u2663\uFE0F";
                case "\u2665" -> "\u2665\uFE0F";
                case "\u2666" -> "\u2666\uFE0F";

                default -> suit;
            };
            
            return String.format("<html><center>%s<br>%s</center></html>", value, suitEmoji);
        }
    }

    private void addCardButton(String card) {
        JButton button = new CardButton(card);
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
                if (enable) {
                    button.setBackground(new Color(220, 255, 220)); // Light green for active turn
                } else {
                    button.setBackground(null); // Reset to default when not active
                }
            }
        });
    }

    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog(
            null,
            "Enter server IP address:",
            "Connect to Server",
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (serverAddress == null || serverAddress.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "Server address is required to connect.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new CardGameClient(serverAddress, 12345));
    }
}
