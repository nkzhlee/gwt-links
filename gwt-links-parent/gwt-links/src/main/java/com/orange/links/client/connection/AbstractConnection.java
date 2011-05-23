package com.orange.links.client.connection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.orange.links.client.DiagramController;
import com.orange.links.client.canvas.DiagramCanvas;
import com.orange.links.client.event.UntieLinkEvent;
import com.orange.links.client.exception.DiagramViewNotDisplayedException;
import com.orange.links.client.menu.ContextMenu;
import com.orange.links.client.shapes.DecorationShape;
import com.orange.links.client.shapes.FunctionShape;
import com.orange.links.client.shapes.Point;
import com.orange.links.client.shapes.Shape;
import com.orange.links.client.utils.ConnectionUtils;
import com.orange.links.client.utils.MovablePoint;
import com.orange.links.client.utils.Segment;
import com.orange.links.client.utils.SegmentPath;

public abstract class AbstractConnection implements Connection {

    protected Shape startShape;
    protected Shape endShape;
    protected Set<Segment> segmentSet;
    protected DiagramController controller;
    protected DiagramCanvas canvas;
    protected DecorationShape decoration;

    public static CssColor defaultConnectionColor = CssColor.make("#000000");
    protected CssColor connectionColor = defaultConnectionColor;
    protected CssColor highlightPointColor = CssColor.make("#cccccc 1");

    protected Point highlightPoint;
    protected Segment highlightSegment;
    protected SegmentPath segmentPath;

    protected ContextMenu menu;
    private boolean sync;
    private boolean allowSync;

    public AbstractConnection(DiagramController controller, Shape startShape, Shape endShape) throws DiagramViewNotDisplayedException {
        this(startShape, endShape);
        setController(controller);
    }

    public AbstractConnection(Shape startShape, Shape endShape) throws DiagramViewNotDisplayedException {
        this.startShape = startShape;
        this.endShape = endShape;
        this.segmentSet = new HashSet<Segment>();

        // Build Path
        this.segmentPath = new SegmentPath(startShape, endShape);
        highlightSegment = this.segmentPath.asStraightPath();

        initMenu();
    }

    protected void initMenu() {
        menu = new ContextMenu();
        menu.addItem(new MenuItem("Delete", true, new Command() {
            public void execute() {
                // fireEvent
                FunctionShape startShape = (FunctionShape) getStartShape();
                FunctionShape endShape = (FunctionShape) getEndShape();

                Widget startWidget = startShape.asWidget();
                Widget endWidget = endShape.asWidget();
                controller.fireEvent(new UntieLinkEvent(startWidget, endWidget, AbstractConnection.this));
                controller.deleteConnection(AbstractConnection.this);
                startShape.removeConnection(AbstractConnection.this);
                endShape.removeConnection(AbstractConnection.this);
                menu.hide();
            }
        }));

        menu.addItem(new MenuItem("Straighten", true, new Command() {
            public void execute() {
                setStraight();
                menu.hide();
            }
        }));
    }

    protected abstract void draw(Point p1, Point p2, boolean lastPoint);

    protected abstract void draw(List<Point> pointList);

    public boolean isSynchronized() {
        return sync;
    }

    public void setSynchronized(boolean sync) {
        if (allowSync) {
            this.sync = sync;
        }
    }

    public void draw() {
        // Reset the segments
        segmentSet = new HashSet<Segment>();

        // Draw each segment
        segmentPath.update();
        List<Point> pointList = new ArrayList<Point>();
        Point startPoint = segmentPath.getFirstPoint();
        pointList.add(startPoint);
        for (Point p : segmentPath.getPathWithoutExtremities()) {
            Point endPoint = p;
            // draw(startPoint, endPoint, false);
            pointList.add(endPoint);
            segmentSet.add(new Segment(startPoint, endPoint));
            startPoint = endPoint;
        }
        // Draw last segment
        Point lastPoint = segmentPath.getLastPoint();
        pointList.add(lastPoint);
        segmentSet.add(new Segment(startPoint, lastPoint));
        // draw(startPoint, lastPoint,true);

        // Draw All the register point
        draw(pointList);

        updateDecoration();
        setSynchronized(true);
    }

    private void updateDecoration() {
        if (decoration != null) {
            Segment decoratedSegment = segmentPath.getMiddleSegment();
            Point decorationCenter = decoratedSegment.middle();
            int width = decoration.getWidth();
            int height = decoration.getHeight();
            decoration.asWidget().getElement().getStyle().setTop(decorationCenter.getTop() - height / 2, Unit.PX);
            decoration.asWidget().getElement().getStyle().setLeft(decorationCenter.getLeft() - width / 2, Unit.PX);
        }
    }

    public MovablePoint addMovablePoint(Point p) {
        Point startSegmentPoint = highlightSegment.getP1();
        Point endSegmentPoint = highlightSegment.getP2();
        MovablePoint movablePoint = new MovablePoint(p);
        segmentPath.add(movablePoint, startSegmentPoint, endSegmentPoint);
        return movablePoint;
    }

    private Point findHighlightPoint(Point p) {
        for (Segment s : segmentSet) {
            if (ConnectionUtils.distanceToSegment(s, p) < DiagramController.minDistanceToSegment) {
                Point hPoint = ConnectionUtils.projectionOnSegment(s, p);
                highlightSegment = s;
                highlightPoint = hPoint;
                return highlightPoint;
            }
        }
        return null;
    }

    public Point highlightMovablePoint(Point p) {
        Point hPoint = findHighlightPoint(p);
        /*
         * if(hPoint != null){
         * DiagramCanvas canvas = controller.getDiagramCanvas();
         * canvas.beginPath();
         * canvas.arc(hPoint.getLeft(), hPoint.getTop(), 5, 0, Math.PI*2, false);
         * canvas.setStrokeStyle(highlightPointColor);
         * canvas.stroke();
         * canvas.setFillStyle(highlightPointColor);
         * canvas.fill();
         * canvas.closePath();
         * }
         */
        setHighlightPoint(hPoint);
        return hPoint;
    }

    public List<Point> getMovablePoints() {
        return segmentPath.getPathWithoutExtremities();
    }

    public void removeDecoration() {
        decoration = null;
    }

    public void setStraight() {
        segmentPath.straightPath();
    }

    public Shape getStartShape() {
        return startShape;
    }

    public Shape getEndShape() {
        return endShape;
    }

    public boolean isMouseNearConnection(Point p) {
        for (Segment s : segmentSet) {
            if (!s.getP1().equals(s.getP2()) && ConnectionUtils.distanceToSegment(s, p) < DiagramController.minDistanceToSegment) {
                return true;
            }
        }
        return false;
    }

    public Point getHighlightPoint() {
        return highlightPoint;
    }

    public void setHighlightPoint(Point highlightPoint) {
        this.highlightPoint = highlightPoint;
    }

    public void setDecoration(DecorationShape decoration) {
        this.decoration = decoration;
    }

    public DecorationShape getDecoration() {
        return decoration;
    }

    @Override
    public ContextMenu getContextMenu() {
        return menu;
    }

    public void setController(DiagramController controller) {
        this.controller = controller;
        this.canvas = controller.getDiagramCanvas();
    }

    @Override
    public void drawHighlight() {
    }

    @Override
    public void allowSynchronized(boolean allowSynchronized) {
        this.allowSync = allowSynchronized;
    }

    
    
}