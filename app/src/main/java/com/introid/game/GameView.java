package com.introid.game;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable{

    private Thread thread;
    private boolean isPlaying,isGameOver=false;
    int screenx,screeny,score=0;
    private Random random;
    public static float screenRetiox, screenRatioy;
    Paint paint;
    private int sound;
    private SharedPreferences prefs;
    private Bird[] birds;
    private List<Bullet> bullets;
    private Flight flight;
    private SoundPool soundPool;
    GameActivity activity;
    private Background background1,background2;

    public GameView(GameActivity activity, int screenx,int screeny) {
        super( activity );
        this.activity=activity;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(audioAttributes)
                    .build();

        } else
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        sound = soundPool.load(activity, R.raw.shoot, 1);

        prefs = activity.getSharedPreferences("game",Context.MODE_PRIVATE );

        background1= new Background( screenx,screeny,getResources() );
        background2 = new Background( screenx,screeny,getResources() );
        screenRetiox= 1920f/ screenx;
        screenRatioy= 1080f/screeny;
        this.screenx=screenx;
        this.screeny= screeny;

        flight = new Flight( this,screeny,getResources() );
        bullets= new ArrayList<>();
        background2.x= screenx;

        paint = new Paint();
        paint.setTextSize( 128 );
        paint.setColor( Color.WHITE );


        birds = new Bird[4];
        for (int i=0; i < 4; i++){
            Bird bird= new Bird( getResources() );
            birds[i] = bird;
        }

        random = new Random(  );
    }

    @Override
    public void run() {
        while(isPlaying){
            update();
            draw();
            sleep();
        }

    }
    private void update(){
        background1.x -= 10 * screenRetiox;
        background2.x -= 10 * screenRetiox;

        if (background1.x + background1.background.getWidth() < 0){
            background1.x = screenx;
        }
        if (background2.x + background2.background.getWidth() < 0){
            background2.x = screenx;
        }

        if (flight.isGoingUp)
            flight.y -= 30 * screenRetiox;
        else
            flight.y += 30 * screenRatioy;

        if (flight.y < 0)
            flight.y=0;

        if (flight.y >= screeny - flight.height)
            flight.y = screeny - flight.height;

        List<Bullet> trash = new ArrayList<>();

        for (Bullet bullet : bullets){
            if(bullet.x > screenx)
                trash.add(bullet);

            bullet.x +=50 * screenRetiox;

            for (Bird bird : birds){
                if (Rect.intersects( bird.getCollisionShape(),bullet.getCollisionShape() )){
                    score++;
                    bird.x= -500;
                    bullet.x= screenx + 500;
                    bird.wasShot= true;
                }
            }
        }

        for (Bullet bullet : trash)
            bullets.remove( bullet );

        for (Bird bird : birds){
            bird.x -= bird.speed;

            if (bird.x + bird.width < 0){
                if (!bird.wasShot ){
                    isGameOver =true;
                    return;
                }

                int bound = (int) (12 * screenRetiox);
                bird.speed= random.nextInt( bound);

                if (bird.speed < 10 * screenRetiox)
                    bird.speed = (int) (10 * screenRetiox);

                bird.x = screenx;
                bird.y = random.nextInt( screeny - bird.height );

                bird.wasShot=false;

            }
            if (Rect.intersects( bird.getCollisionShape(),flight.getCollisionShape() )){
                isGameOver = true;
                return;
            }

        }
    }
    private void draw(){
         if (getHolder().getSurface().isValid()){
             Canvas canvas= getHolder().lockCanvas();
             canvas.drawBitmap( background1.background,background1.x,background1.y,paint );
             canvas.drawBitmap( background2.background,background2.x,background2.y,paint );

             for (Bird bird : birds)
                 canvas.drawBitmap( bird.getBird(),bird.x,bird.y,paint  );

             canvas.drawText( score + "" , screenx /2f, 164, paint );

             if (isGameOver ){
                 isPlaying = false;
                 canvas.drawBitmap( flight.getDead(),flight.x,flight.y,paint );
                 getHolder().unlockCanvasAndPost(canvas);
                 saveIfHighScore();
                 waitBeforeExiting();
                 return;
             }

             canvas.drawBitmap( flight.getFlight(),flight.x,flight.y,paint );

             for (Bullet bullet : bullets)
                 canvas.drawBitmap( bullet.bullet,bullet.x,bullet.y,paint);

             getHolder().unlockCanvasAndPost(canvas);
         }
    }
    private void waitBeforeExiting() {
        try {
            Thread.sleep( 2000 );
            activity.startActivity(new Intent( activity,MainActivity.class ) );
            activity.finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveIfHighScore() {
        if (prefs.getInt( "highScore", 0 ) < score){
            SharedPreferences.Editor editor= prefs.edit();
            editor.putInt( "highScore", score );
            editor.apply();
        }
    }

    private void sleep() {
        try {
            Thread.sleep( 17 );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume(){
        isPlaying =true;
        thread= new Thread(this);
        thread.start();

    }
    public void pause(){
        try {
            isPlaying= false;
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < screenx / 2){
                    flight.isGoingUp = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                flight.isGoingUp=false;
                if (event.getX() > screenx /2)
                    flight.toShoot++;
                break;
        }
        return true;
    }

    public void newBullet() {
        if (!prefs.getBoolean("isMute", false))
            soundPool.play(sound, 1, 1, 0, 0, 1);

        Bullet bullet = new Bullet(getResources());
        bullet.x = flight.x + flight.width;
        bullet.y = flight.y + (flight.height / 2);
        bullets.add(bullet);
    }
}
