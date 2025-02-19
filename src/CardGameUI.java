import javax.swing.*;
import java.awt.*;

public class CardGameUI extends JFrame {
    public CardGameUI() {
        setTitle("Card Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 400);
        setLayout(new FlowLayout());

        // Creating Card objects
        Card card1 = new Card("S", "10");  // 10 of Spades
        Card card2 = new Card("H", "A");   // Ace of Hearts
        Card card3 = new Card("D", "K");   // King of Diamonds
        Card card4 = new Card("C", "5");   // 5 of Clubs

        // Add cards to the frame
        add(card1);
        add(card2);
        add(card3);
        add(card4);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CardGameUI::new);
    }
}
