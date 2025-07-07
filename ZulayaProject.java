import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ZulayaProject extends Application {
    //Window size
    public static double WIDTH = 1500;
    public static double HEIGHT = 768;

    //Character coordinates
    private double characterCurrentPosX = 10;
    private double characterCurrentPosY = HEIGHT - 130;
    private double previousCharacterPosY = HEIGHT - 130;

    //Gravity
    private double velocity = 0;
    private final double gravity = 0.5;
    private final double jumpStrength = -12;

    //HashMap for smooth movement
    Map<KeyCode, Boolean> pressedKeys = new HashMap<>();

    //Jump
    private boolean jumpKeyPressed = false;  // true while jump key is held down
    private boolean jumpKeyConsumed = false; // true once jump triggered for

    @Override
    public void start(Stage stage) throws Exception {
        //Window
        Pane pane = new Pane();
        pane.setPrefSize(WIDTH, HEIGHT);
        Polygon background = new Polygon(0, 0, WIDTH, 0, WIDTH, WIDTH, 0, WIDTH);
        background.setFill(Color.WHITE);
        pane.getChildren().add(background);

        //Character
        Polygon character = new Polygon(0, 0, 50, 0, 50, 80, 0, 80);
        character.setTranslateX(characterCurrentPosX);
        character.setTranslateY(characterCurrentPosY);
        character.setFill(Color.BLACK);
        pane.getChildren().add(character);
        //Scaling for regular look, but this can be changed for ducking ec.
        Scale scale = new Scale(1, 1, 25, 80);
        character.getTransforms().add(scale);

        //Ground
        Polygon ground = new Polygon(0, 0, WIDTH, 0, WIDTH, 50, 0, 50);
        ground.setTranslateY(HEIGHT - 50);
        ground.setFill(Color.GREEN);
        pane.getChildren().add(ground);

        //Platforms
        Platform platform1 = new Platform(300, 50, 150, HEIGHT - 150, Color.GRAY);
        pane.getChildren().add(platform1);
        //-------Platform2
        Platform platform2 = new Platform(200, 50, 600, HEIGHT - 250, Color.DARKGRAY);
        pane.getChildren().add(platform2);

        //Platform list
        List<Platform> platforms = Arrays.asList(platform1, platform2);

        Scene scene = new Scene(pane);

        //-----Character movement
        scene.setOnKeyPressed(event -> {
            pressedKeys.put(event.getCode(), Boolean.TRUE);
        });

        scene.setOnKeyReleased(event -> {
            pressedKeys.put(event.getCode(), Boolean.FALSE);
        });

        scene.setOnKeyPressed(event -> {
            pressedKeys.put(event.getCode(), true);

            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.SPACE) {
                if (!jumpKeyPressed) {
                    jumpKeyPressed = true;
                    jumpKeyConsumed = false;  // reset consumption on new press
                }
            }
        });

        scene.setOnKeyReleased(event -> {
            pressedKeys.put(event.getCode(), false);

            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.SPACE) {
                jumpKeyPressed = false;
                jumpKeyConsumed = false;  // allow new jump on next press
            }
        });

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double moveSpeed = 5;
                final double charWidth = 50;
                final double charHeight = 80;
                final double charDuckHeightModifier = 0.6;

                double currentScaleY = scale.getY();
                double currentCharHeight = charHeight * currentScaleY;
                double duckOffsetY = charHeight - currentCharHeight;  // offset to adjust collision box top

                double targetScaleY = 1.0; // default standing

                double nextPosX = characterCurrentPosX;
                double nextPosY = characterCurrentPosY + velocity;

                // Calculate proposed horizontal movement
                if (pressedKeys.getOrDefault(KeyCode.LEFT, false)) {
                    nextPosX = Math.max(0, characterCurrentPosX - moveSpeed);
                }
                if (pressedKeys.getOrDefault(KeyCode.RIGHT, false)) {
                    nextPosX = Math.min(WIDTH - 50, characterCurrentPosX + moveSpeed);
                }

                //Ducking
                if (pressedKeys.getOrDefault(KeyCode.DOWN, false)) {
                    targetScaleY = charDuckHeightModifier; // ducking
                } else {
                    // Check if standing up is possible (no platform above)
                    boolean canStandUp = true;
                    double standUpHeight = charHeight; // full height

                    // Calculate the character's bounding box if standing up at current X,Y
                    double collisionPosYIfStanding = characterCurrentPosY; // no offset when standing

                    // Check all platforms if any overlaps with the space above current ducked character
                    for (Platform platform : platforms) {
                        boolean horizontallyAligned = (characterCurrentPosX + charWidth > platform.getLeft()) &&
                                (characterCurrentPosX < platform.getRight());

                        // The top of the standing character would be at collisionPosYIfStanding
                        // The bottom of the platform is platform.getBottom()
                        // If platform is above current character and overlaps horizontally and vertically with standing height, block standing up
                        if (horizontallyAligned) {
                            // Platform is above current character if platform bottom is less than current character bottom
                            double platformBottom = platform.getBottom();
                            double characterBottomIfStanding = collisionPosYIfStanding + standUpHeight;

                            // Check if platform bottom is above character bottom and below character top (overlapping space)
                            if (platformBottom <= characterBottomIfStanding && platformBottom > collisionPosYIfStanding) {
                                canStandUp = false;
                                break;
                            }
                        }
                    }

                    if (canStandUp) {
                        targetScaleY = 1.0; // safe to stand up
                    } else {
                        targetScaleY = charDuckHeightModifier; // keep ducked to avoid clipping
                    }
                }

                // Apply the target scale Y
                scale.setY(targetScaleY);

                //Sprint
                if (pressedKeys.getOrDefault(KeyCode.SHIFT, false) && pressedKeys.getOrDefault(KeyCode.LEFT, false)) {
                    nextPosX = Math.max(0, characterCurrentPosX - moveSpeed * 2);
                } else if (pressedKeys.getOrDefault(KeyCode.SHIFT, false) && pressedKeys.getOrDefault(KeyCode.RIGHT, false)) {
                    nextPosX = Math.min(WIDTH - 50, characterCurrentPosX + moveSpeed * 2);
                }

                // Apply gravity
                velocity += gravity;
                nextPosY += velocity;

                // Head bump collision check (moving up)
                if (velocity < 0) {
                    for (Platform platform : platforms) {
                        double platformBottom = platform.getBottom();
                        double platformLeft = platform.getLeft();
                        double platformRight = platform.getRight();

                        double characterTopNext = nextPosY + duckOffsetY;
                        double characterLeftNext = nextPosX;
                        double characterRightNext = nextPosX + charWidth;

                        boolean horizontallyAligned = (characterRightNext > platformLeft) && (characterLeftNext < platformRight);
                        boolean crossedPlatformBottom = (previousCharacterPosY + duckOffsetY >= platformBottom) && (characterTopNext <= platformBottom);

                        if (horizontallyAligned && crossedPlatformBottom) {
                            nextPosY = platformBottom - duckOffsetY;
                            velocity = 0;
                            break;
                        }
                    }
                }

                // Horizontal collision check
                for (Platform platform : platforms) {
                    boolean verticalOverlap = (nextPosY + duckOffsetY + currentCharHeight > platform.getTop()) && (nextPosY + duckOffsetY < platform.getBottom());

                    // Collision from right
                    if (pressedKeys.getOrDefault(KeyCode.RIGHT, false) && verticalOverlap) {
                        if (nextPosX + charWidth > platform.getLeft() && characterCurrentPosX + charWidth <= platform.getLeft()) {
                            nextPosX = platform.getLeft() - charWidth;
                        }
                    }
                    // Collision from left
                    if (pressedKeys.getOrDefault(KeyCode.LEFT, false) && verticalOverlap) {
                        if (nextPosX < platform.getRight() && characterCurrentPosX >= platform.getRight()) {
                            nextPosX = platform.getRight();
                        }
                    }
                }

                // Vertical collision check (landing)
                boolean landed = false;
                for (Platform platform : platforms) {
                    if (velocity > 0 && platform.isHorizontallyAligned(nextPosX, nextPosX + charWidth)
                            && platform.isStandingOn(nextPosY + duckOffsetY + currentCharHeight, previousCharacterPosY + duckOffsetY + currentCharHeight)) {
                        nextPosY = platform.getTop() - currentCharHeight - duckOffsetY;
                        velocity = 0;
                        landed = true;
                        break;
                    }
                }

                // Ground collision
                double groundLevel = HEIGHT - 130;
                if (nextPosY > groundLevel) {
                    nextPosY = groundLevel;
                    velocity = 0;
                    landed = true;
                }

                // Update positions (no offset here, polygon scaling handles visual)
                characterCurrentPosX = nextPosX;
                characterCurrentPosY = nextPosY;
                character.setTranslateX(characterCurrentPosX);
                character.setTranslateY(characterCurrentPosY);
                previousCharacterPosY = characterCurrentPosY;

                // Jumping logic remains the same, but use landed flag or check onGround/onPlatform accordingly
                boolean onGround = characterCurrentPosY >= groundLevel;
                boolean onPlatform = landed && !onGround;

                //Jumping reset
                if (jumpKeyPressed && !jumpKeyConsumed && (onGround || onPlatform)) {
                    velocity = jumpStrength;
                    jumpKeyConsumed = true;
                }
            }
        }.start();

        stage.setScene(scene);
        stage.setTitle("Zulaya project");
        stage.show();
        }

        //Collision
    public static boolean collide(Polygon character, Polygon platform) {
        Shape collisionArea = Shape.intersect(character, platform);
        return collisionArea.getBoundsInLocal().getWidth() != -1;
    }


    public static void main(String[] args) {

        launch(ZulayaProject.class);
    }
}
