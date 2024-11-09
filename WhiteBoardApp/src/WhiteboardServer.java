/**
 * WhiteboardServer.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This class represents the server side of the whiteboard application. It listens
 * for incoming client connections and creates a new ClientHandler thread for each
 * client. It also manages the list of connected clients and shapes on the whiteboard.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class WhiteboardServer {
    private ServerSocket serverSocket;
    private static final int NUM_THREADS = 32;
    private static final int TIMEOUT = 60;
    private ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    private ArrayList<ClientHandler> clients = new ArrayList<>();
    private ArrayList<Shape> shapes = new ArrayList<>();
    private WhiteboardManager manager;

    /**
     * Constructor for the WhiteboardServer class. Creates a new server socket
     * listening on the specified port.
     *
     * @param port port number to listen on
     */
    public WhiteboardServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error creating server socket: " + e.getMessage());
        }
    }

    /**
     * Listens for incoming client connections and creates a new ClientHandler
     * thread for each client.
     */
    public void listenForClients() {
        try {
            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    pool.execute(clientHandler);

                    // Add shutdown hook to gracefully shutdown server
                    Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.out.println("Error accepting client connection: " + e.getMessage());
                    } else {
                        System.out.println("Server socket was closed. Stopping client acceptance.");
                    }
                }
            }
        } finally {
            pool.shutdown();
            try {
                // Wait for all tasks to complete before shutting down the thread pool
                if (!pool.awaitTermination(TIMEOUT, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
    }

    /**
     * Adds a new shape to the whiteboard and broadcasts it to all connected clients.
     *
     * @param shape shape to be added
     * @param senderStream ObjectOutputStream of the client that sent the shape
     */
    public synchronized void addShape(Shape shape, ObjectOutputStream senderStream) {
        shapes.add(shape);
        broadcastShape(shape, senderStream);
    }

    /**
     * Broadcasts a shape to all connected clients except the client that sent the shape.
     *
     * @param shape shape to be broadcasted
     * @param senderStream ObjectOutputStream of the client that sent the shape
     */
    public synchronized void broadcastShape(Shape shape, ObjectOutputStream senderStream) {
        for (ClientHandler client : clients) {
            if (client.getOut() != senderStream) {
                client.sendShape(shape);
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message message to be broadcasted
     */
    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    /**
     * Updates the user list displayed on the whiteboard for all connected clients.
     */
    public synchronized void updateUserList() {
        StringBuilder userList = new StringBuilder("<html>");
        for (ClientHandler client : clients) {
            userList.append(client.getUsername()).append("<br>");
        }
        userList.append("</html>");
        broadcastMessage("User List Update:" + userList);
    }

    public List<ClientHandler> getClients() {
        return clients;
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    /**
     * Clears all shapes on the whiteboard and broadcasts the clear command to all
     *
     * @param sender ObjectOutputStream of the client that sent the clear command
     */
    public synchronized void clearAllShapes(ObjectOutputStream sender) {
        shapes.clear();
        broadcastClearAll(sender);
    }

    /**
     * Broadcasts a clear command to all connected clients except the client that sent the command.
     *
     * @param sender ObjectOutputStream of the client that sent the clear command
     */
    private synchronized void broadcastClearAll(ObjectOutputStream sender) {
        for (ClientHandler client : clients) {
            if (client.getOut() != sender) {
                client.sendClearCommand();
            }
        }
    }

    /**
     * Opens a new whiteboard for all connected clients except the client that sent the command.
     *
     * @param sender ObjectOutputStream of the client that sent the open command
     */
    public synchronized void openNewBoard(ObjectOutputStream sender) {
        for (ClientHandler client : clients) {
            if (client.getOut() != sender) {
                client.sendOpenCommand(shapes);
            }
        }
    }

    public void setManager(WhiteboardManager manager) {
        this.manager = manager;
    }

    public synchronized boolean notifyManager(JoinRequest request) {
        return manager.notifyJoinRequest(request.getUsername());
    }

    /**
     * Shuts down the server by closing the server socket and notifying all connected clients.
     */
    public void shutdown() {
        System.out.println("Shutting down the server...");
        try {
            // First, make sure no new clients are accepted
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Notify existing clients of the server shutdown
            broadcastServerShutdown();

            for (ClientHandler client : clients) {
                client.closeConnection();
            }
            clients.clear();

            // Ensure all tasks are completed before shutting down the thread pool
            pool.shutdown();
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }

            System.out.println("Server shutdown successfully.");

        } catch (IOException | InterruptedException e) {
            System.err.println("Error while shutting down the server: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a server shutdown command to all connected clients.
     */
    private synchronized void broadcastServerShutdown() {
        ServerQuitCommand quitCommand = new ServerQuitCommand();

        // Notify all clients except the manager
        Iterator<ClientHandler> it = clients.iterator();
        while (it.hasNext()) {
            ClientHandler client = it.next();
            try {
                if (!Objects.equals(client.getUsername(), manager.getUsername()) && client.getSocket().isConnected()) {
                    client.getOut().writeObject(quitCommand);
                    client.getOut().reset();
                }
            } catch (IOException e) {
                System.err.println("Error notifying client of shutdown: " + e.getMessage());
                it.remove();
            }
        }
    }

    /**
     * Kicks out a user from the whiteboard by sending a kick command to the client.
     *
     * @param username username of the user to be kicked out
     */
    public synchronized void kickUser(String username) {
        Iterator<ClientHandler> it = clients.iterator();
        while (it.hasNext()) {
            ClientHandler client = it.next();
            if (client.getUsername().equals(username)) {
                try {
                    client.getOut().writeObject(new KickCommand(username));
                    client.getOut().reset();
                    client.getOut().close();
                    it.remove(); // Remove from the client list
                } catch (IOException e) {
                    System.err.println("Error kicking out user: " + e.getMessage());
                }
                break;
            }
        }
    }

    /**
     * Checks if a username is already taken by another client.
     *
     * @param username username to check
     * @param askingClientSocket socket of the client that is asking
     * @return true if the username is taken, false otherwise
     */
    public synchronized boolean isUsernameTaken(String username, Socket askingClientSocket) {
        return clients.stream()
                .anyMatch(c -> !c.getSocket().equals(askingClientSocket) && c.getUsername().equals(username));
    }
}
