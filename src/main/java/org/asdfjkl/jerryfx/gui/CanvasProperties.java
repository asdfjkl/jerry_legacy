package org.asdfjkl.jerryfx.gui;

import javafx.scene.canvas.Canvas;

public abstract class CanvasProperties extends Canvas {
    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double maxHeight(double width) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double maxWidth(double height) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public double minWidth(double height) {
        return 50D;
    }

    @Override
    public double minHeight(double width) {
        return 50D;
    }

    public void resize(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);

        updateCanvas();
    }

    public void updateCanvas() {}
}
