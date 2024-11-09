/**
 * Shape.java
 * Author: Marshall Zhang
 * Student ID: 1160040
 * These classes are used to define the shapes that can be drawn on the canvas.
 */

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

abstract class Shape implements Serializable {
    protected int startX, startY, endX, endY;
    protected Color color;

    /**
     * Constructor for the Shape class
     *
     * @param startX the x-coordinate of the starting point
     * @param startY the y-coordinate of the starting point
     * @param endX the x-coordinate of the ending point
     * @param endY the y-coordinate of the ending point
     * @param color the color of the shape
     */
    public Shape(int startX, int startY, int endX, int endY, Color color) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.color = color;
    }

    /**
     * Abstract method to draw the shape
     *
     * @param g the Graphics object
     */
    public abstract void draw(Graphics g);
}

/**
 * The Line class is used to draw a line on the canvas
 */
class Line extends Shape {
    public Line(int startX, int startY, int endX, int endY, Color color) {
        super(startX, startY, endX, endY, color);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawLine(startX, startY, endX, endY);
    }
}

/**
 * The Rectangle class is used to draw a rectangle on the canvas
 */
class Rectangle extends Shape {
    public Rectangle(int startX, int startY, int width, int height, Color color) {
        super(startX, startY, startX + width, startY + height, color);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawRect(startX, startY, endX - startX, endY - startY);
    }
}

/**
 * The Oval class is used to draw an oval on the canvas
 */
class Oval extends Shape {
    public Oval(int startX, int startY, int width, int height, Color color) {
        super(startX, startY, startX + width, startY + height, color);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawOval(startX, startY, endX - startX, endY - startY);
    }
}

/**
 * The Circle class is used to draw a circle on the canvas
 */
class Circle extends Shape {
    public Circle(int startX, int startY, int diameter, Color color) {
        super(startX, startY, startX + diameter, startY + diameter, color);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawOval(startX, startY, endX - startX, endX - startX);
    }
}

/**
 * Handles the free draw operation
 */
class FreeDraw extends Shape {
    private ArrayList<Point> points;

    public FreeDraw(Color color) {
        super(0, 0, 0, 0, color);
        this.points = new ArrayList<>();
        this.color = color;
    }

    public void addPoint(int x, int y) {
        points.add(new Point(x, y));
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        for (int i = 1; i < points.size(); i++) {
            Point p1 = points.get(i - 1);
            Point p2 = points.get(i);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }
}

/**
 * The Text class is used to draw text on the canvas
 */
class Text extends Shape {
    private String text;

    public Text(int startX, int startY, String text, Color color) {
        super(startX, startY, startX, startY, color);
        this.text = text;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.drawString(text, startX, startY);
    }
}

/**
 * The Eraser class is used to erase parts of the canvas
 */
class Eraser extends Shape {
    private int size;

    public Eraser(int startX, int startY, int size, Color backgroundColor) {
        super(startX, startY, startX, startY, backgroundColor);
        this.size = size;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(startX - size / 2, startY - size / 2, size, size);
    }
}


