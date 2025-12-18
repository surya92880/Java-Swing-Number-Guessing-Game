import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class SwingNumberGuessingGame extends JFrame implements ActionListener {

    // --- File Persistence ---
    private static final String HIGH_SCORE_FILE = "high_score.txt";
    private int currentHighScore = loadHighScore(); // Fewest guesses

    // --- GUI Components ---
    private JLabel hintLabel;
    private JLabel statsLabel;
    private JLabel timerLabel;
    private JTextField guessField;
    private JButton guessButton;
    private JButton startButton;
    private JComboBox<String> difficultyComboBox;
    private Timer timer; // Swing's timer

    // --- Game State Variables ---
    private final int MAX_TIME = 60; // Max time per round (60 seconds)
    private int timeLeft;
    
    private int targetNumber;
    private int minRange;
    private int maxRange;
    private int maxGuesses;
    
    private int guessCount;
    private int previousGuess = -1; 
    
    private int totalRounds = 0;
    private int totalGuesses = 0;

    public SwingNumberGuessingGame() {
        // --- Frame Setup ---
        setTitle("Swing Number Guessing Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10)); // Use BorderLayout for the main frame
        
        // --- Initialize Components ---
        hintLabel = new JLabel("Select Difficulty and press 'Start Game'!", SwingConstants.CENTER);
        timerLabel = new JLabel("Time: 60s | Guesses: 0/0", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statsLabel = new JLabel("High Score: " + (currentHighScore == Integer.MAX_VALUE ? "N/A" : currentHighScore) + " (fewest guesses)", SwingConstants.CENTER);
        
        guessField = new JTextField(10);
        guessField.setToolTipText("Enter your guess...");
        guessField.setEnabled(false);
        
        guessButton = new JButton("Guess");
        guessButton.setEnabled(false);
        guessButton.addActionListener(this); // Register for clicks
        
        startButton = new JButton("Start Game");
        startButton.addActionListener(this);
        
        difficultyComboBox = new JComboBox<>();
        difficultyComboBox.addItem("Easy (1-50, 7 Guesses)");
        difficultyComboBox.addItem("Medium (1-100, 10 Guesses)");
        difficultyComboBox.addItem("Hard (1-1000, 15 Guesses)");
        difficultyComboBox.setSelectedItem("Medium (1-100, 10 Guesses)");

        // --- Layout Panels ---
        
        // Top Panel for labels
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.add(new JLabel("--- NUMBER GUESSING GAME ---", SwingConstants.CENTER));
        topPanel.add(statsLabel);
        topPanel.add(timerLabel);
        
        // Center Panel for controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controlsPanel.add(new JLabel("Difficulty:"));
        controlsPanel.add(difficultyComboBox);
        controlsPanel.add(new JLabel("Guess:"));
        controlsPanel.add(guessField);
        controlsPanel.add(guessButton);

        // --- Add Panels to Frame ---
        add(topPanel, BorderLayout.NORTH);
        add(hintLabel, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlsPanel, BorderLayout.CENTER);
        bottomPanel.add(startButton, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // --- Finalize Frame ---
        setupTimer();
        pack(); // Sizes the frame based on component sizes
        setLocationRelativeTo(null); // Center the window
        setVisible(true);
    }
    
    // ====================================================================
    // --- TIMER LOGIC (Swing specific) ---
    // ====================================================================

    private void setupTimer() {
        timeLeft = MAX_TIME;
        // Swing Timer runs in the Event Dispatch Thread (EDT)
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeLeft--;
                updateTimerLabel();

                if (timeLeft <= 0) {
                    timer.stop();
                    hintLabel.setText("***TIME'S UP*** The number was " + targetNumber + "!");
                    endGame();
                }
            }
        });
    }
    
    private void updateTimerLabel() {
         timerLabel.setText("Time: " + timeLeft + "s | Guesses: " + guessCount + "/" + maxGuesses);
    }

    // ====================================================================
    // --- ACTION LISTENER (Handles Button Clicks) ---
    // ====================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            handleStartGame();
        } else if (e.getSource() == guessButton) {
            handleGuess();
        }
    }

    // ====================================================================
    // --- GAME CONTROL LOGIC (Mostly the same as JavaFX) ---
    // ====================================================================

    private void handleStartGame() {
        // 1. Get configuration based on difficulty
        String selection = (String)difficultyComboBox.getSelectedItem();
        GameConfig config = getConfigFromSelection(selection);
        minRange = config.min;
        maxRange = config.max;
        maxGuesses = config.maxGuesses;

        // 2. Reset state
        Random rand = new Random();
        targetNumber = rand.nextInt(maxRange - minRange + 1) + minRange;
        guessCount = 0;
        previousGuess = -1;
        
        // 3. Reset and start timer
        timer.stop();
        timeLeft = MAX_TIME;
        timer.start();

        // 4. Enable GUI and set hints
        guessField.setEnabled(true);
        guessButton.setEnabled(true);
        startButton.setText("Restart Game");
        guessField.requestFocus();
        
        hintLabel.setText("I've picked a number between " + minRange + " and " + maxRange + ". Start guessing!");
        updateTimerLabel();
    }

    private void handleGuess() {
        if (!guessButton.isEnabled() || guessCount >= maxGuesses) {
            return;
        }

        int playerGuess;
        try {
            playerGuess = Integer.parseInt(guessField.getText().trim());

            if (playerGuess < minRange || playerGuess > maxRange) {
                hintLabel.setText("Invalid range! Enter a number between " + minRange + " and " + maxRange + ".");
                return;
            }
        } catch (NumberFormatException e) {
            hintLabel.setText("Please enter a valid whole number.");
            return;
        }
        
        guessCount++;
        totalGuesses++;
        updateTimerLabel();

        // --- Core Game Logic ---
        if (playerGuess == targetNumber) {
            timer.stop();
            totalRounds++;
            
            // High Score Check
            if (guessCount < currentHighScore) {
                currentHighScore = guessCount;
                saveHighScore(currentHighScore);
                statsLabel.setText("High Score: " + currentHighScore + " (NEW RECORD!)");
            }
            
            hintLabel.setText("***WINNER*** Correct! You won in " + guessCount + " guesses!");
            endGame();
        } else {
            // Provide Warmer/Colder Hint
            if (previousGuess != -1) {
                provideWarmerColderHint(playerGuess);
            }
            
            // Append Higher/Lower Hint
            String highLowHint = (targetNumber > playerGuess) ? " The number is HIGHER." : " The number is LOWER.";
            hintLabel.setText(hintLabel.getText() + highLowHint);
            
            previousGuess = playerGuess;
            
            // Check for game over (out of guesses)
            if (guessCount >= maxGuesses) {
                timer.stop();
                totalRounds++;
                hintLabel.setText("***DEFEAT*** Ran out of guesses! The number was " + targetNumber + ".");
                endGame();
            }
        }
        
        guessField.setText("");
    }
    
    private void provideWarmerColderHint(int currentGuess) {
        int currentDiff = Math.abs(targetNumber - currentGuess);
        int previousDiff = Math.abs(targetNumber - previousGuess);

        if (currentDiff < previousDiff) {
            hintLabel.setText(">> WARMER! You are getting closer.");
        } else if (currentDiff > previousDiff) {
            hintLabel.setText(">> COLDER! You moved further away.");
        } else {
            hintLabel.setText(">> Same distance.");
        }
    }

    private void endGame() {
        guessField.setEnabled(false);
        guessButton.setEnabled(false);
        startButton.setText("Play Again");
        
        // Display final session stats
        statsLabel.setText(String.format("Rounds: %d | Total Guesses: %d | Avg: %.2f | High Score: %s",
                            totalRounds, totalGuesses, 
                            totalRounds > 0 ? (double)totalGuesses / totalRounds : 0.0,
                            currentHighScore == Integer.MAX_VALUE ? "N/A" : currentHighScore));

        // Use a standard confirmation dialog
        int choice = JOptionPane.showConfirmDialog(this, "The round has ended.\nDo you want to start a new game?", 
                                               "Round Finished", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            handleStartGame();
        }
    }
    
    // ====================================================================
    // --- HELPER METHODS (Config & Persistence) ---
    // ====================================================================
    
    private static class GameConfig {
        int min;
        int max;
        int maxGuesses;
        public GameConfig(int min, int max, int maxGuesses) {
            this.min = min;
            this.max = max;
            this.maxGuesses = maxGuesses;
        }
    }
    
    private GameConfig getConfigFromSelection(String selection) {
        if (selection == null) return new GameConfig(1, 100, 10); // Default fallback
        
        switch (selection) {
            case "Easy (1-50, 7 Guesses)":
                return new GameConfig(1, 50, 7);
            case "Medium (1-100, 10 Guesses)":
            default:
                return new GameConfig(1, 100, 10);
            case "Hard (1-1000, 15 Guesses)":
                return new GameConfig(1, 1000, 15);
        }
    }
    
    private int loadHighScore() {
        try (Scanner fileScan = new Scanner(new File(HIGH_SCORE_FILE))) {
            if (fileScan.hasNextInt()) {
                return fileScan.nextInt();
            }
        } catch (FileNotFoundException e) {
            return Integer.MAX_VALUE;
        }
        return Integer.MAX_VALUE;
    }
    
    private void saveHighScore(int newScore) {
        try (FileWriter writer = new FileWriter(HIGH_SCORE_FILE)) {
            writer.write(String.valueOf(newScore));
        } catch (IOException e) {
            System.err.println("Error saving high score: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        // Swing components must be created and updated on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new SwingNumberGuessingGame());
    }
}