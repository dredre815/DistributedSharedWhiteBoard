/**
 * WhiteboardClient.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This class represents the client-side of the distributed shared whiteboard application.
 * It connects to the server, sends and receives messages, and updates the UI accordingly.
 */

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhiteboardClient extends JFrame {
    protected Socket socket;
    protected ObjectOutputStream output;
    protected ObjectInputStream input;
    protected DrawingCanvas canvas;
    protected Color currentColor = Color.BLACK;
    protected int currentEraserSize = 10;
    protected JTextField textField;
    protected JButton sendTextButton;
    protected JTextPane chatArea;
    protected JTextField chatInput;
    protected JTextPane userList;
    protected String username;
    protected ExecutorService drawingExecutor = Executors.newSingleThreadExecutor();
    protected ExecutorService messagingExecutor = Executors.newSingleThreadExecutor();

    /**
     * Constructor for the WhiteboardClient class.
     *
     * @param serverAddress The address of the server
     * @param serverPort The port number of the server
     * @param username The username of the client
     */
    public WhiteboardClient(String serverAddress, int serverPort, String username) {
        super("Distributed Shared Whiteboard: " + username);
        this.username = username;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null); // Center the window

        setupUI();

        try {
            socket = new Socket(serverAddress, serverPort);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Send username immediately after connection
            sendMessage(username);

            // Start the dispatcher thread
            Thread dispatcherThread = new Thread(this::dispatchInput);
            dispatcherThread.start();
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to connect to the server.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Sets up the UI components of the client application.
     */
    protected void setupUI() {
        canvas = new DrawingCanvas(this);
        add(canvas, BorderLayout.CENTER);

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        add(toolPanel, BorderLayout.NORTH);

        // Tool selection combo box
        toolPanel.add(new JLabel("Tools:"));
        JComboBox<String> toolSelector = new JComboBox<>(new String[]{"Free Draw", "Line", "Rectangle", "Oval",
                "Circle", "Eraser", "Text"});
        toolSelector.addActionListener(e -> {
            canvas.setCurrentTool((String) toolSelector.getSelectedItem());
            textField.setEnabled("Text".equals(toolSelector.getSelectedItem()));
            sendTextButton.setEnabled("Text".equals(toolSelector.getSelectedItem()));
        });
        toolPanel.add(toolSelector);

        // Text field for entering text
        textField = new JTextField(15);
        textField.setEnabled(false);
        toolPanel.add(new JLabel("Text:"));
        toolPanel.add(textField);

        // Button to send text to canvas
        sendTextButton = new JButton("Add Text");
        sendTextButton.setEnabled(false);
        sendTextButton.addActionListener(e -> canvas.setTextToDraw(textField.getText()));
        toolPanel.add(sendTextButton);

        // Side panel for chat and user list
        chatArea = new JTextPane();
        chatArea.setContentType("text/html");
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setPreferredSize(new Dimension(200, 400));
        JPanel sidePanel = new JPanel(new BorderLayout());
        sidePanel.add(chatScroll, BorderLayout.CENTER);

        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String message = chatInput.getText();
            if (!message.isEmpty()) {
                try {
                    sendMessage(message);
                    chatInput.setText("");
                } catch (Exception ex) {
                    System.out.println("Failed to send message: " + ex.getMessage());
                }
            }
        });
        sidePanel.add(chatInput, BorderLayout.SOUTH);

        // User list
        userList = new JTextPane();
        userList.setContentType("text/html");
        userList.setEditable(false);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 100));
        sidePanel.add(userScroll, BorderLayout.EAST);

        add(sidePanel, BorderLayout.EAST);

        // Color picker button
        JButton colorButton = new JButton("Change Color");
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(null, "Choose a color", currentColor);
            if (newColor != null) {
                currentColor = newColor;
                canvas.setCurrentColor(currentColor);
            }
        });
        toolPanel.add(colorButton);

        // Eraser size selection with label
        toolPanel.add(new JLabel("Eraser Size:"));
        JComboBox<String> sizeSelector = new JComboBox<>(new String[]{"Small (10px)", "Medium (20px)", "Large (30px)"});
        sizeSelector.addActionListener(e -> {
            switch (sizeSelector.getSelectedIndex()) {
                case 0 -> currentEraserSize = 10;
                case 1 -> currentEraserSize = 20;
                case 2 -> currentEraserSize = 30;
            }
            canvas.setEraserSize(currentEraserSize);
        });
        toolPanel.add(sizeSelector);
    }

    /**
     * Sends a message to the server.
     *
     * @param message The message to send
     */
    protected void sendMessage(String message) {
        try {
            output.writeObject(message);
            output.reset();
        } catch (IOException e) {
            appendToChatPane("Failed to send message", false);
        }
    }

    /**
     * Sends a shape to the server.
     *
     * @param shape The shape to send
     */
    public void sendShape(Shape shape) {
        try {
            output.writeObject(shape);
            output.reset();
        } catch (IOException e) {
            System.out.println("Failed to send shape: " + e.getMessage());
        }
    }

    /**
     * Dispatches input from the server and processes it accordingly.
     */
    protected void dispatchInput() {
        try {
            while (true) {
                Object object = input.readObject();
                if (object instanceof String message) {
                    // Process messages in a separate thread
                    messagingExecutor.submit(() -> processMessage(message));
                } else if (object instanceof Shape shape) {
                    // Add shapes to the canvas in the Swing thread
                    drawingExecutor.submit(() -> SwingUtilities.invokeLater(() -> canvas.addShape(shape)));
                } else if (object instanceof ClearCommand) {
                    // Clear the canvas in the Swing thread
                    drawingExecutor.submit(() -> SwingUtilities.invokeLater(canvas::clearCanvas));
                } else if (object instanceof OpenCommand openCommand) {
                    // Clear the canvas and add all shapes in the OpenCommand
                    drawingExecutor.submit(() -> SwingUtilities.invokeLater(() -> {
                        canvas.clearCanvas();
                        openCommand.getShapes().forEach(canvas::addShape);
                    }));
                } else if (object instanceof JoinResponse joinResponse) {
                    // Handle join response
                    if (joinResponse.approved()) {
                        JOptionPane.showMessageDialog(this, "Your join request has been approved.");
                    } else {
                        JOptionPane.showMessageDialog(this, "Your join request has been denied.",
                                "Access Denied", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                } else if (object instanceof ServerQuitCommand) {
                    // Handle server quit
                    handleServerQuit();
                } else if (object instanceof KickCommand cmd) {
                    // Handle kick
                    if (cmd.getUsername().equals(this.username)) {
                        handleKick();
                    }
                } else if (object instanceof UsernameTakenCommand) {
                    // Handle username taken
                    handleUsernameTaken();
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Handle exceptions
            System.out.println("Error processing input: " + e.getMessage());
        }
    }

    /**
     * Processes a message received from the server.
     *
     * @param message The message to process
     */
    protected void processMessage(String message) {
        if (message.startsWith("User List Update:")) {
            // Update user list
            updateUserList(message.substring(17));
        } else {
            // Append message to chat pane
            appendToChatPane(message, message.startsWith(username + ":"));
        }
    }

    /**
     * Appends a message to the chat pane.
     *
     * @param message The message to append
     * @param isSelf Whether the message is from the client
     */
    protected void appendToChatPane(String message, boolean isSelf) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Append message to chat pane
                HTMLDocument doc = (HTMLDocument) chatArea.getStyledDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) chatArea.getEditorKit();
                String style = isSelf ? "<div style='text-align: right; color: blue;'>" : "<div style='text-align: left; color: black;'>";
                editorKit.insertHTML(doc, doc.getLength(), style + message + "</div>", 0, 0, null);
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } catch (IOException | BadLocationException ex) {
                System.err.println("Failed to append message: " + ex.getMessage());
            }
        });
    }

    /**
     * Updates the user list with the given HTML.
     *
     * @param userListHtml The HTML to update the user list with
     */
    protected void updateUserList(String userListHtml) {
        SwingUtilities.invokeLater(() -> userList.setText(userListHtml));
    }

    /**
     * Sends a clear command to the server.
     *
     * @throws IOException If an I/O error occurs
     */
    protected void sendClearCommand() throws IOException {
        output.writeObject(new ClearCommand());
        output.reset();
    }

    /**
     * Sends a new shapes list to the server.
     *
     * @param newShapes The new shapes list to send
     * @throws IOException If an I/O error occurs
     */
    public void sendNewShapesList(ArrayList<Shape> newShapes) throws IOException {
        output.writeObject(new OpenCommand(newShapes));
        output.reset();
    }

    /**
     * Sends a join request to the server.
     *
     * @param username The username to send
     */
    public void sendJoinRequest(String username) {
        try {
            output.writeObject(new JoinRequest(username));
            output.reset();
        } catch (IOException e) {
            System.out.println("Failed to send join request: " + e.getMessage());
        }
    }

    protected String getUsername() {
        return username;
    }

    /**
     * Handles the server quitting by displaying a message and exiting the application.
     */
    private void handleServerQuit() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "The manager has quit. The application will now exit.", "Server " +
                    "Shutdown", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
    }

    /**
     * Handles the client being kicked out by displaying a message and exiting the application.
     */
    private void handleKick() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "You have been kicked out by the manager.",
                    "Kicked Out", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        });
    }

    /**
     * Handles the username being taken by displaying a message and exiting the application.
     */
    private void handleUsernameTaken() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Username is already taken. Please choose a different one.", "Username Taken", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }
}
