/**
 * WhiteboardManager.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This class represents the manager side of the whiteboard application. It extends
 * the WhiteboardClient class and adds additional functionality for managing the whiteboard
 * and connected clients.
 */

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class WhiteboardManager extends WhiteboardClient{
    /**
     * Constructor for the WhiteboardManager class. Creates a new WhiteboardManager
     * and sets up the menu bar for the whiteboard application.
     *
     * @param serverAddress server address
     * @param serverPort server port
     * @param username username
     */
    public WhiteboardManager(String serverAddress, int serverPort, String username) {
        super(serverAddress, serverPort, username);
        setupMenuBar();
    }

    /**
     * Sets up the menu bar for the whiteboard application. The menu bar contains
     * options for creating a new whiteboard, opening an existing whiteboard, saving
     * the current whiteboard, saving the current whiteboard as a new file, and closing
     * the application.
     */
    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.addActionListener(e -> canvas.clearCanvas());
        fileMenu.add(newItem);

        // Open a file chooser dialog to select a file to open
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                // Load the shapes from the selected file
                File selectedFile = fileChooser.getSelectedFile();
                canvas.loadShapesFromFile(selectedFile);
            }
        });
        // Add the "Open" menu item to the "File" menu
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                // Save the shapes to the selected file
                File file = fileChooser.getSelectedFile();
                canvas.saveShapesToFile(file);
            }
        });
        // Add the "Save" menu item to the "File" menu
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                // Save the shapes to the selected file
                File file = fileChooser.getSelectedFile();
                canvas.saveShapesToFile(file);
            }
        });
        // Add the "Save As" menu item to the "File" menu
        fileMenu.add(saveAsItem);

        // Add a "Close" menu item to the "File" menu
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> System.exit(0));
        fileMenu.add(closeItem);

        // Input for username to kick
        JTextField usernameField = new JTextField(10);
        JButton kickButton = new JButton("Kick");

        // Send a kick command when the "Kick" button is clicked
        kickButton.addActionListener(e -> {
            String usernameToKick = usernameField.getText();
            if (!usernameToKick.isEmpty()) {
                sendKickCommand(usernameToKick);
            }
        });

        menuBar.add(fileMenu);
        menuBar.add(new JLabel("Username to kick:"));
        menuBar.add(usernameField);
        menuBar.add(kickButton);
        setJMenuBar(menuBar);
    }

    /**
     * Notifies the manager that a client has requested to join the whiteboard.
     * Displays a dialog box asking the manager to approve or deny the request.
     *
     * @param username username of the client requesting to join
     * @return true if the manager approves the request, false otherwise
     */
    public boolean notifyJoinRequest(String username) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] approval = new boolean[1];

        SwingUtilities.invokeLater(() -> {
            try {
                // Display a dialog box asking the manager to approve or deny the request
                int result = JOptionPane.showConfirmDialog(this,
                        username + " wants to share your whiteboard. Approve?",
                        "Join Request",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                approval[0] = (result == JOptionPane.YES_OPTION);
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();  // Wait for the dialog to be dismissed
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();  // Restore interrupt status
        }
        // Return the manager's approval decision
        return approval[0];
    }

    /**
     * Sends a kick command to the server to kick a client from the whiteboard.
     *
     * @param username username of the client to kick
     */
    public void sendKickCommand(String username) {
        try {
            output.writeObject(new KickCommand(username));
            output.reset();
        } catch (IOException e) {
            System.err.println("Failed to send kick command: " + e.getMessage());
        }
    }
}
