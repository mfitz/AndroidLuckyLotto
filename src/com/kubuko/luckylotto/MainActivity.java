package com.kubuko.luckylotto;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

public class MainActivity extends ActionBarActivity {
	
	private static final String LOG_TAG = "LuckyLotto";
	// TODO - make these configurable via settings menu
	private static final int NUMBER_OF_BALLS_TO_PICK = 6;
	private static final int TOTAL_NUMBER_OF_BALLS = 49;
	private static final int DROP_SPEED = 7;
	
	private Button dropButton, sortButton;
	private LotteryBall[] lotteryBalls;
	private LotteryBall[] chosenNumbers;
	private RelativeLayout frame;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		frame = (RelativeLayout) findViewById(R.id.frame);
		
		lotteryBalls = new LotteryBall[TOTAL_NUMBER_OF_BALLS + 1];
		for (int i = 1; i < TOTAL_NUMBER_OF_BALLS + 1; i++) {
			String imageName = "num" + i;
			String imageUri = "drawable/" + imageName;
			int imageId = 
				getResources().getIdentifier( imageUri, null, getPackageName() );
			Drawable ballImage = getResources().getDrawable(imageId);
			Log.d(LOG_TAG, "Loaded drawable for " + imageName);
			LotteryBall ball = 
				new LotteryBall(this, i, ballImage, -50, -50, DROP_SPEED);
			lotteryBalls[i] = ball;
		}
		
		dropButton = (Button)this.findViewById(R.id.pick_button);
		OnClickListener dropListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				int[] numbers = pickNumbers();
				Log.d(LOG_TAG, "Picked numbers " + arrayToString(numbers));
				dropBalls(numbers);
			}
		};
		dropButton.setOnClickListener(dropListener);
		
		sortButton = (Button)this.findViewById(R.id.sort_button);
	}
	
	private int[] pickNumbers() {
		
		int[] luckyNumbers = new int[NUMBER_OF_BALLS_TO_PICK];

        for (int i = 0; i < NUMBER_OF_BALLS_TO_PICK; i++) {
            boolean alreadyIn = true;
            int newNumber = 0;

            while (alreadyIn) {
                alreadyIn = false;
                newNumber = (int) (Math.random() * TOTAL_NUMBER_OF_BALLS) + 1;

                for (int j = 0; j < i; j++) {
                    if (luckyNumbers[j] == newNumber) {
                        alreadyIn = true;
                        break;
                    }
                }
            }

            luckyNumbers[i] = newNumber;
        }
        
        return luckyNumbers;
	}
	
	private void dropBalls(int[] numbers) {
		
		if (chosenNumbers != null) {
			for (int i = 0; i < chosenNumbers.length; i++) {
				View lotteryBallView = chosenNumbers[i];
				ViewGroup parent = (ViewGroup)lotteryBallView.getParent();
				parent.removeView(lotteryBallView);
			}
		}
//		frame.invalidate();
		
		chosenNumbers = new LotteryBall[numbers.length];
		int frameMidpoint = frame.getWidth() / 2;
		// assume all lottery ball images are the same size
		int imageSize = 
			lotteryBalls[lotteryBalls.length - 1].getScaledBitmap().getWidth();
		int xPosition = 
			frameMidpoint - ( (numbers.length / 2) * imageSize);
		for (int i = 0; i < numbers.length; i++) {
			int ballNumber = numbers[i];
			LotteryBall ball = this.lotteryBalls[ballNumber];
			ball.setXPos(xPosition);
			// start off screen
			ball.setYPos(0 - imageSize);
			chosenNumbers[i] = ball;
			if (ball.getParent() == null) {
				frame.addView(ball);
			}
			ball.startMoving(i);
			xPosition += imageSize;
		}
	}
	
	private String arrayToString(int[] numbers) {
		
		String numbersString = "[";
		for (int i = 0; i < numbers.length; i++) {
			numbersString += numbers[i];
			if (i != numbers.length - 1) {
				numbersString += ",";
			}
		}
		numbersString += "]";
		
		return numbersString;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private class LotteryBall extends View {
		
		private static final int REFRESH_RATE = 10;
		private static final long PAUSE_INCREMENT = 500;
		
		private final Paint mPainter = new Paint();
		
		private int number;
		private Drawable image;
		private Bitmap scaledBitmap;
		private int xPos, yPos;
		private int dropSpeed;
		private ScheduledFuture<?> moverFuture;
		
		public LotteryBall(Context context,
						int number, 
						Drawable image, 
						int startXPos, 
						int startYPos,
						int dropSpeed) {
			super(context);
			this.number = number;
			this.image = image;
			this.xPos = startXPos;
			this.yPos = startYPos;
			this.dropSpeed = dropSpeed;
			this.scaledBitmap = scaleImage(image);
		}
		
		public void startMoving(int pause) {
			
			long pauseTime = pause * PAUSE_INCREMENT;
			ScheduledExecutorService executor = 
				Executors.newScheduledThreadPool(1);

			moverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {

					if ( stillMoving() ) {
						frame.post(new Runnable() {
							@Override
							public void run() {
								LotteryBall.this.invalidate();
							}
						});
					} else {
						stopMoving();
					}
				}
			}, pauseTime, REFRESH_RATE, TimeUnit.MILLISECONDS);
		}
		@Override
		protected synchronized void onDraw(Canvas canvas) {
			
			canvas.save();
			canvas.drawBitmap(scaledBitmap, xPos, yPos, mPainter);
			canvas.restore();
		}
		
		private synchronized boolean stillMoving() {

			yPos += dropSpeed;
			
			return reachedDestination();
		}

		private boolean reachedDestination() {
			
			int screenThreeQuarterHeight = frame.getHeight() / 2;
			if ( yPos < screenThreeQuarterHeight ) {
				return true;
			} else {
				// level out all images at the same y position
				yPos = screenThreeQuarterHeight;
				return false;
			}
		}
		
		private void stopMoving() {
			
			Log.d(LOG_TAG, "Stopping ball " + LotteryBall.this);

			if (null != moverFuture) {

				if (moverFuture.isDone() ==  false) {
					moverFuture.cancel(true);
				}
			}
		}

		private Bitmap scaleImage(Drawable drawable) {
			
			Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
			// TODO scale the image according to the width of the frame 
			// and number of lottery balls to display
//			return Bitmap.createScaledBitmap(bitmap, 50, 50, true);
			return bitmap;
		}

		public void setXPos(int xPos) {
			this.xPos = xPos;
		}

		public void setYPos(int yPos) {
			this.yPos = yPos;
		}
		
		public Bitmap getScaledBitmap() {
			return scaledBitmap;
		}
	}
}
