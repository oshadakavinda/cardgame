import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

public class Card extends JPanel {

    private static final int ARC_SIZE = 15;
    private static final Map<String, ImageIcon> suitIcons = new HashMap<>();

    private final String cardType;
    private final String cardValue;

    public Card(String cardType, String cardValue) {
        this.cardType = cardType;
        this.cardValue = cardValue;

        setPreferredSize(new Dimension(150, 220)); // Increased card size
        setLayout(new BorderLayout());

        loadSuitIcons();

        // Value Label (Center)
        JLabel leftValueLabel = new JLabel(cardValue);
        leftValueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        leftValueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        add(leftValueLabel, BorderLayout.NORTH);

        JLabel rightValueLabel = new JLabel(cardValue);
        rightValueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        rightValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        add(rightValueLabel, BorderLayout.SOUTH);

        // Suit Icon (North)
        JLabel suitLabelNorth = new JLabel(suitIcons.get(cardType));
        if (suitLabelNorth != null) {
            suitLabelNorth.setHorizontalAlignment(SwingConstants.CENTER);
            add(suitLabelNorth, BorderLayout.CENTER);
        } else {
            System.err.println("Suit icon is null for type: " + cardType);
        }



        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10) // Adjust border padding if needed
        ));
        setBackground(Color.WHITE);
    }

    private void loadSuitIcons() {
        if (suitIcons.isEmpty()) {
            suitIcons.put("S", createScaledIcon("/card_images/S.jpeg", 40, 60));
            suitIcons.put("H", createScaledIcon("/card_images/h.png", 40, 60));
            suitIcons.put("C", createScaledIcon("/card_images/c.png", 40, 60));
            suitIcons.put("D", createScaledIcon("/card_images/d.png", 40, 60));
        }
    }

    private ImageIcon createScaledIcon(String path, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(getClass().getResource(path));
        if (originalIcon == null) {
            System.err.println("Error loading image: " + path);
            return null;
        }
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
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

    public static void main(String[] args) {
        JFrame frame = new JFrame("Card Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());

        frame.add(new Card("S", "A"));
        frame.add(new Card("H", "K"));
        frame.add(new Card("C", "10"));
        frame.add(new Card("D", "2"));

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}