/**
 * DrawingCanvas.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * This file contains the DrawingCanvas class which is a JPanel that allows the user to draw shapes on it.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

public class DrawingCanvas extends JPanel {
    private int startX = -1, startY = -1;
    private Color currentColor = Color.BLACK;
    private String currentTool = "Free Draw";
    private int eraserSize = 10;
    private String textToDraw = "";
    private FreeDraw freeDraw;
    private Text text;
    private WhiteboardClient client;
    private ArrayList<Shape> shapes = new ArrayList<>();

    /**
     * Constructor for the DrawingCanvas class.
     *
     * @param client The WhiteboardClient object that this DrawingCanvas is associated with.
     */
    public DrawingCanvas(WhiteboardClient client) {
        this.client = client;
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        addMouseListeners();
    }

    /**
     * Adds mouse listeners to the DrawingCanvas.
     */
    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            /**
             * Invoked when a mouse button has been pressed on a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();

                // Create a new shape based on the current tool
                switch (currentTool) {
                    case "Eraser" -> {
                        Eraser eraser = new Eraser(startX, startY, eraserSize, Color.WHITE);
                        shapes.add(eraser);
                        repaint();
                        client.sendShape(eraser);
                    }
                    case "Text" -> {
                        text = new Text(startX, startY, textToDraw, currentColor);
                        shapes.add(text);
                        repaint();
                        client.sendShape(text);
                        text = null;
                    }
                    case "Free Draw" -> {
                        freeDraw = new FreeDraw(currentColor);
                        freeDraw.addPoint(startX, startY);
                        shapes.add(freeDraw);
                    }
                }
            }

            /**
             * Invoked when a mouse button has been released on a component.
             *
             * @param e the event to be processed
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool.equals("Free Draw") && freeDraw != null) {
                    // Finish the free draw shape
                    freeDraw.addPoint(e.getX(), e.getY());
                    repaint();
                    client.sendShape(freeDraw);
                    freeDraw = null;
                } else if (!currentTool.equals("Eraser") && !currentTool.equals("Text")) {
                    // Create a shape based on the current tool and add it to the list of shapes
                    Shape shape = createShape(startX, startY, e.getX(), e.getY());
                    if (shape != null) {
                        shapes.add(shape);
                        repaint();
                        client.sendShape(shape);
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentTool.equals("Free Draw") && freeDraw != null) {
                    // Continue adding points to the free draw shape
                    freeDraw.addPoint(e.getX(), e.getY());
                    repaint();
                    client.sendShape(freeDraw);
                } else if (currentTool.equals("Eraser")) {
                    // Create an eraser shape and add it to the list of shapes
                    Eraser eraser = new Eraser(e.getX(), e.getY(), eraserSize, Color.WHITE);
                    shapes.add(eraser);
                    repaint();
                    client.sendShape(eraser);
                }
            }
        });
    }

    /**
     * Creates a shape based on the current tool and the given coordinates.
     *
     * @param x1 the x-coordinate of the starting point
     * @param y1 the y-coordinate of the starting point
     * @param x2 the x-coordinate of the ending point
     * @param y2 the y-coordinate of the ending point
     * @return the created shape
     */
    private Shape createShape(int x1, int y1, int x2, int y2) {
        switch (currentTool) {
            case "Line":
                return new Line(x1, y1, x2, y2, currentColor);
            case "Rectangle":
                return new Rectangle(x1, y1, x2 - x1, y2 - y1, currentColor);
            case "Oval":
                return new Oval(x1, y1, x2 - x1, y2 - y1, currentColor);
            case "Circle":
                int diameter = (int) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                return new Circle(x1, y1, diameter, currentColor);
            default:
                return null;
        }
    }

    public void setCurrentColor(Color color) {
        this.currentColor = color;
    }

    public void setCurrentTool(String tool) {
        this.currentTool = tool;
    }

    public void setEraserSize(int size) {
        this.eraserSize = size;
    }

    public void setTextToDraw(String text) {
        this.textToDraw = text;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Shape shape : shapes) {
            shape.draw(g);
        }
    }

    public void addShape(Shape shape) {
        shapes.add(shape);
        repaint();
    }

    /**
     * Clears the canvas by removing all shapes and repainting it.
     */
    public void clearCanvas() {
        shapes.clear();
        repaint();

        try {
            client.sendClearCommand();
        } catch (IOException e) {
            System.err.println("Failed to send clear command: " + e.getMessage());
        }
    }

    /**
     * Saves the shapes to a file.
     *
     * @param file the file to save the shapes to
     */
    public void saveShapesToFile(File file) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(shapes);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Loads shapes from a file and repaints the canvas with the new shapes.
     *
     * @param file the file to load the shapes from
     */
    @SuppressWarnings("unchecked")
    public void loadShapesFromFile(File file) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            ArrayList<Shape> newShapes = (ArrayList<Shape>) in.readObject();
            shapes.clear();
            shapes.addAll(newShapes);
            repaint();
            client.sendNewShapesList(newShapes);
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error loading shapes: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
