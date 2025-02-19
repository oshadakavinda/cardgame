import javax.swing.*;
import java.awt.*;

public class CardUI extends JFrame {

    public CardUI() {
        setTitle("Card UI Example");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(2, 2, 15, 15)); // 2 rows, 2 columns with spacing

        // Add multiple cards
        add(createCard("Card 1", "This is the first card"));
        add(createCard("Card 2", "This is the second card"));
        add(createCard("Card 3", "This is the third card"));
        add(createCard("Card 4", "This is the fourth card"));

        setVisible(true);
    }

    private JPanel createCard(String title, String description) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3, true));
        card.setBackground(new Color(200, 200, 200)); // Light gray background

        JLabel titleLabel = new JLabel(title, JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JLabel descLabel = new JLabel(description, JLabel.CENTER);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        // Add components to the card
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);

        return card;
    }

    public static void main(String[] args) {
        new CardUI();
    }
}
