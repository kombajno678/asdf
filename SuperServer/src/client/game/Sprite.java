package client.game;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.geometry.Rectangle2D;

import static java.lang.Math.abs;

public class Sprite
{
    private Image image;
    private double positionX;
    private double positionY;
    private double velocityX;
    private double velocityY;
    private double width;
    private double height;

    public double speed = 100;

    public int score;

    public void normalizeSpeed(){
        //find sum of all
        double sum = abs(velocityX) + abs(velocityY);
        velocityX /= sum;
        velocityY /= sum;
        velocityX *= speed;
        velocityY *= speed;
    }

    public void bounce(Sprite obj){
        //left/right side
        if(this.getPositionY() > obj.getPositionY() && this.getPositionY()+this.getHeight() < obj.getPositionY() + obj.getHeight()){
            this.setVelocityX(this.getVelocityX() * -1);

        }else{
        //top/bottom side
            this.setVelocityY(this.getVelocityY() * -1);
        }
    }
    public void bounceOfPaddle(Sprite obj){

        //top side
        if(this.getPositionY()+this.getHeight() > obj.getPositionY()){
            this.setVelocityX(5*(this.getPositionX() - obj.getPositionX() - (obj.getWidth()/2)));
            this.setVelocityY(this.getVelocityY() * -1);
            this.setPositionY(this.getPositionY() - 2);
        }else
        //left/right side
        if(this.getPositionX() <= obj.getPositionX()+obj.getWidth() || this.getPositionX()+this.getWidth() > obj.getPositionX()){
            this.setVelocityX(this.getPositionX()*-1);
            this.setVelocityY(this.speed);
        }

    }

    public double getPositionX() {
        return positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Sprite()
    {
        positionX = 0;
        positionY = 0;
        velocityX = 0;
        velocityY = 0;
    }

    public void setImage(Image i)
    {
        image = i;
        width = i.getWidth();
        height = i.getHeight();
    }

    public void setImage(String filename)
    {
        Image i = new Image(filename);
        setImage(i);
    }

    public void setPositionX(double x) {
        positionX = x;
    }

    public void setPosition(double x, double y)
    {
        positionX = x;
        positionY = y;
    }


    public void setVelocity(double x, double y)
    {
        velocityX = x;
        velocityY = y;
    }

    public void addVelocity(double x, double y)
    {
        velocityX += x;
        velocityY += y;
    }

    public void update(double time)
    {
        positionX += velocityX * time;
        positionY += velocityY * time;
    }

    public void render(GraphicsContext gc)
    {
        gc.drawImage( image, positionX, positionY );
    }

    public Rectangle2D getBoundary()
    {
        return new Rectangle2D(positionX,positionY,width,height);
    }

    public boolean intersects(Sprite s)
    {
        return s.getBoundary().intersects( this.getBoundary() );
    }

    public String toString()
    {
        return " Position: [" + positionX + "," + positionY + "]"
                + " Velocity: [" + velocityX + "," + velocityY + "]";
    }
}