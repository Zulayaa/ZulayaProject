import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

    public class Platform extends Pane {
        private Polygon shape;
        private double width;
        private double height;

        public Platform(double width, double height, double x, double y, Color color) {
            this.width = width;
            this.height = height;

            shape = new Polygon(0, 0, width, 0, width, height, 0, height);
            shape.setFill(color);

            this.getChildren().add(shape);
            this.setTranslateX(x);
            this.setTranslateY(y);
        }

        public double getLeft() {
            return this.getTranslateX();
        }

        public double getRight() {
            return this.getTranslateX() + width;
        }

        public double getTop() {
            return this.getTranslateY();
        }

        public double getBottom() {
            return this.getTranslateY() + height;
        }

        // Check horizontal collision with a character bounding box
        public boolean isHorizontallyAligned(double charLeft, double charRight) {
            return charRight > getLeft() && charLeft < getRight();
        }

        // Check vertical collision (standing on top)
        public boolean isStandingOn(double charBottom, double prevCharBottom) {
            return prevCharBottom <= getTop() && charBottom >= getTop();
        }
    }
