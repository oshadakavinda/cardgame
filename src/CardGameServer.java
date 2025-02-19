import java.io.*;
import java.net.*;
import java.util.*;

public class CardGameServer {
    private static final int PORT = 12345;
    private static final List<PlayerHandler> players = new ArrayList<>();
    private static final List<String> deck = new ArrayList<>();
    private static int currentPlayer = 0;
    private static final int[] scores = new int[3];
    private static final Map<Integer, String> currentRound = new HashMap<>();
    private static int roundCounter = 0;
    private static final int MAX_ROUNDS = 5;

    public static void main(String[] args) {
        System.out.println("Server private IP: " + getPrivateIP());
        initializeDeck();
        startGame();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);
            System.out.println("Waiting for players to connect...");

            while (players.size() < 3) {
                try {
                    Socket socket = serverSocket.accept();
                    PlayerHandler player = new PlayerHandler(socket, players.size());
                    players.add(player);
                    new Thread(player).start();
                    System.out.println("Player " + (players.size()) + " connected from " +
                            socket.getInetAddress().getHostAddress());
                } catch (IOException e) {
                    System.err.println("Error accepting player connection: " + e.getMessage());
                }
            }

            dealCards();
            broadcast("GAME_START");
            players.get(currentPlayer).sendMessage("YOUR_TURN");
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String getPrivateIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Could not determine private IP";
        }
    }

    private static void initializeDeck() {
        String[] suits = {"S", "H", "D", "C"};
        String[] ranks = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + " of " + suit);
            }
        }
    }

    private static void startGame() {
        Collections.shuffle(deck);
        Arrays.fill(scores, 0);
        roundCounter = 0;
        currentRound.clear();
    }

    private static void dealCards() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 3; j++) {
                players.get(j).sendMessage("CARD:" + deck.remove(0));
            }
        }
    }

    public static void broadcast(String message) {
        for (PlayerHandler player : players) {
            player.sendMessage(message);
        }
    }

    public static synchronized void playCard(int playerIndex, String card) {
        currentRound.put(playerIndex, card);
        broadcast("PLAYED:" + (playerIndex + 1) + ":" + card);

        if (currentRound.size() == 3) {
            evaluateRound();
            currentRound.clear();
            broadcastScores();
            roundCounter++;

            if (roundCounter >= MAX_ROUNDS) {
                broadcast("GAME_OVER");
                broadcast("FINAL_SCORES:" + scores[0] + "," + scores[1] + "," + scores[2]);
            } else {
                currentPlayer = determineNextPlayer();
                players.get(currentPlayer).sendMessage("YOUR_TURN");
            }
        } else {
            currentPlayer = (currentPlayer + 1) % 3;
            players.get(currentPlayer).sendMessage("YOUR_TURN");
        }
    }

    private static int determineNextPlayer() {
        int maxValue = -1;
        int winner = 0;
        for (Map.Entry<Integer, String> entry : currentRound.entrySet()) {
            int value = getCardValue(entry.getValue());
            if (value > maxValue) {
                maxValue = value;
                winner = entry.getKey();
            }
        }
        return winner;
    }

    private static void evaluateRound() {
        int maxValue = -1;
        int winner = -1;
        for (Map.Entry<Integer, String> entry : currentRound.entrySet()) {
            int value = getCardValue(entry.getValue());
            if (value > maxValue) {
                maxValue = value;
                winner = entry.getKey();
            } else if (value == maxValue) {
                winner = breakTie(winner, entry.getKey());
            }
        }
        scores[winner]++;
    }

    private static int breakTie(int player1, int player2) {
        String card1 = currentRound.get(player1);
        String card2 = currentRound.get(player2);
        return getSuitValue(card1) >= getSuitValue(card2) ? player1 : player2;
    }

    private static int getCardValue(String card) {
        String[] parts = card.split(" ");
        String rank = parts[0];
        return switch (rank) {
            case "Jack" -> 11;
            case "Queen" -> 12;
            case "King" -> 13;
            case "Ace" -> 14;
            default -> Integer.parseInt(rank);
        } * 10 + getSuitValue(card);
    }

    private static int getSuitValue(String card) {
        String suit = card.split(" of ")[1];
        return switch (suit) {
            case "Spades" -> 4;
            case "Hearts" -> 3;
            case "Diamonds" -> 2;
            case "Clubs" -> 1;
            default -> 0;
        };
    }

    private static void broadcastScores() {
        broadcast("SCORES:" + scores[0] + "," + scores[1] + "," + scores[2]);
    }
}

class PlayerHandler implements Runnable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final int playerIndex;

    public PlayerHandler(Socket socket, int index) throws IOException {
        this.socket = socket;
        this.playerIndex = index;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("PLAY:")) {
                    CardGameServer.playCard(playerIndex, input.substring(5));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}
