package client.game;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.Iterator;

public class BricksGame extends Application
{

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle( "BreakBricks" );

        Group root = new Group();
        Scene theScene = new Scene( root );

        primaryStage.setScene( theScene );
        theScene.setFill(new Color(0.05, 0.05, 0.15, 1));


        Canvas canvas = new Canvas( 512, 512 );

        root.getChildren().add( canvas );
        //primaryStage.setResizable(false);
        primaryStage.setMinHeight(552);
        primaryStage.setMinWidth(528);
        primaryStage.setMaxHeight(628);
        primaryStage.setMaxWidth(528);

        ArrayList<String> input = new ArrayList<>();

        theScene.setOnKeyPressed(e -> {
            String code = e.getCode().toString();
            if ( !input.contains(code) )
                input.add( code );
        });

        theScene.setOnKeyReleased(e -> {
            String code = e.getCode().toString();
            input.remove( code );
        });

        GraphicsContext gc = canvas.getGraphicsContext2D();

        Font theFont = Font.font( "Arial", FontWeight.BOLD, 24 );
        gc.setFont( theFont );
        gc.setFill( Color.WHITE );
        //gc.setStroke( Color.BLACK );
        //gc.setLineWidth(1);

        Sprite paddle = new Sprite();
        paddle.setImage("client\\game\\paddle.png");
        paddle.setPosition(256-(paddle.getWidth()/2), 512-paddle.getHeight());
        paddle.speed = 400;

        final ArrayList<Sprite> brickList = new ArrayList<>();

        for (int i = 0; i < 16; i++)
        {
            for(int j = 4; j < 10; j++){
                Sprite brick = new Sprite();
                //int randomNum = ThreadLocalRandom.current().nextInt(0, 8 + 1);
                brick.setImage("client\\game\\brick"+(j-4)+".png");
                brick.score = (j-10)*-100;
                double px = i*32;
                double py = j*20;
                brick.setPosition(px,py);
                brickList.add( brick );
            }
        }

        Sprite ball = new Sprite();
        ball.setImage("client\\game\\ball.png");
        ball.setPosition(256, 400);
        ball.setVelocity(0, -200);
        ball.speed = 200;

        LongValue lastNanoTime = new LongValue( System.nanoTime() );
        IntValue score = new IntValue(0);

        final int paddleSpeed = 400;

        new AnimationTimer()
        {
            private boolean playing = true;
            public void handle(long currentNanoTime)
            {
                // calculate time since last update.
                double elapsedTime = (currentNanoTime - lastNanoTime.value) / 1000000000.0;
                lastNanoTime.value = currentNanoTime;

                if(!playing){
                    //game over
                    // render
                    gc.clearRect(0, 0, 512,512);
                    paddle.render( gc );
                    ball.render(gc);
                    for (Sprite brick : brickList )
                        brick.render( gc );

                    String pointsText = "SCORE: " + (score.value);
                    gc.fillText( pointsText, 36, 36 );
                    String gameOver = "GAME OVER";
                    gc.fillText(gameOver, 180, 256);
                    String info = "press up arrow to play again";
                    gc.fillText(info, 100, 276);

                    // button input
                    paddle.setVelocity(0,0);
                    if (input.contains("UP")){
                        //prepare for next game
                        score.value = 0;
                        brickList.clear();
                        for (int i = 0; i < 16; i++)
                        {
                            for(int j = 4; j < 10; j++){
                                Sprite brick = new Sprite();
                                brick.setImage("client\\game\\brick"+(j-4)+".png");
                                brick.score = (j-10)*-100;
                                double px = i*32;
                                double py = j*20;
                                brick.setPosition(px,py);
                                brickList.add( brick );
                            }
                        }
                        paddle.setPosition(256-(paddle.getWidth()/2), 512-paddle.getHeight());
                        ball.setPosition(256, 386);
                        ball.setVelocity(0, -200);
                        ball.speed = 200;
                        playing = true;
                    }


                }else{
                    // button input
                    paddle.setVelocity(0,0);
                    if (input.contains("LEFT"))
                        paddle.addVelocity(-paddleSpeed,0);
                    if (input.contains("RIGHT"))
                        paddle.addVelocity(paddleSpeed,0);


                    //paddle must stay within bounds
                    if(paddle.getBoundary().getMinX() <= 0 ){
                        //paddle.setVelocity(0, 0);
                        paddle.setPositionX(0);
                    }else if(paddle.getBoundary().getMinX() >= 512 - paddle.getWidth()){
                        paddle.setPositionX(512 - paddle.getWidth());
                    }

                    // ball - bricks collision detection
                    Iterator<Sprite> brickIter = brickList.iterator();
                    while ( brickIter.hasNext() ) {
                        Sprite brick = brickIter.next();
                        if ( ball.intersects(brick) ) {

                            brickIter.remove();
                            score.value += brick.score;
                            ball.bounce(brick);
                            ball.speed += 10;
                            break;
                        }
                    }

                    //bounce ball of paddle

                    if(ball.intersects(paddle)){//0,1s
                        ball.bounceOfPaddle(paddle);

                    }

                    //bounce ball of walls
                    if(ball.getPositionX() <= 0 || ball.getPositionX()+ball.getWidth() >= 512){
                        ball.setVelocityX(ball.getVelocityX() * -1);
                    }
                    if(ball.getPositionY()+ball.getHeight() >= 512){
                        ball.setVelocity(0, 0);
                        playing = false;

                    }
                    if(ball.getPositionY() <= 0){
                        ball.setVelocityY(ball.getVelocityY() * -1);
                    }

                    //make sure that ball is in bounds
                    if(ball.getPositionX() <= 0){
                        ball.setPositionX(0);
                    }
                    if(ball.getPositionX() + ball.getWidth() >= 512){
                        ball.setPositionX(512 - ball.getWidth());
                    }
                    if(ball.getPositionY() <= 0){
                        ball.setPositionY(0);
                    }
                    if(ball.getPositionY() + ball.getHeight() >= 512){
                        ball.setPositionY(512 - ball.getHeight());
                    }


                    // render
                    gc.clearRect(0, 0, 512,512);
                    paddle.update(elapsedTime);
                    ball.normalizeSpeed();
                    ball.update(elapsedTime);
                    paddle.render( gc );
                    ball.render(gc);
                    for (Sprite brick : brickList )
                        brick.render( gc );

                    String pointsText = "SCORE: " + (score.value);
                    gc.fillText( pointsText, 36, 36 );
                }


            }
        }.start();

        primaryStage.show();


    }
}