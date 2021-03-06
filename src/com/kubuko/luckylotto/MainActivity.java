package com.kubuko.luckylotto;

import java.util.Arrays;
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
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
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
	
	private AudioManager audioManager;
	private SoundPool soundPool;
	private int ballLandedSoundID;
	private float streamVolume;

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
				Integer[] numbers = pickNumbers();
				Log.d(LOG_TAG, "Picked numbers " + arrayToString(numbers));
				int imageHeight = getImageHeight();
				dropBalls(numbers, 0 - imageHeight);
			}
		};
		dropButton.setOnClickListener(dropListener);
		
		sortButton = (Button)this.findViewById(R.id.sort_button);
		OnClickListener sortListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (chosenNumbers != null) {
					Log.d(LOG_TAG, "Sorting numbers " 
							+ arrayToString(chosenNumbers));
					Integer[] numbers = sortBalls(chosenNumbers);
					dropBalls(numbers, 
							getYDestination() - ( getImageHeight() / 2) );
				}
			}
		};
		sortButton.setOnClickListener(sortListener);
	}
	
	@Override
	protected void onResume() {
		 super.onResume();
		 
		 audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		 streamVolume = 
			(float) audioManager
				 .getStreamVolume(AudioManager.STREAM_MUSIC)
				 / audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		 soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
		 soundPool.setOnLoadCompleteListener( new OnLoadCompleteListener() {
			 @Override
			 public void onLoadComplete(SoundPool soundPool, 
					 					int sampleId,
					 					int status) {
				 Log.d(LOG_TAG, "SoundPool has finished loading sound " 
					 					+ sampleId + " (status = " 
					 					+ status + ")");
			 }
		 });
		 ballLandedSoundID = soundPool.load(this, R.raw.blop, 1);
	}
	
	@Override
	protected void onPause() {

		Log.d(LOG_TAG, "In onPause()...");
		soundPool.release();
		soundPool = null;
		
		super.onPause();
	}
	
	private int getImageHeight() {
		LotteryBall exampleBall = lotteryBalls[lotteryBalls.length - 1];
		return exampleBall.getScaledBitmap().getHeight();
	}
	
	private Integer[] pickNumbers() {
		
		Integer[] luckyNumbers = new Integer[NUMBER_OF_BALLS_TO_PICK];

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
	
	private void dropBalls(Integer[] numbers, int startingY) {
		
		clearOldImages();
		chosenNumbers = new LotteryBall[numbers.length];
		
		int margins = frame.getPaddingLeft() + frame.getPaddingRight();
		int frameMidpoint = (frame.getWidth() - margins) / 2;
		// assume all lottery ball images are the same size
		int imageSize = 
			lotteryBalls[lotteryBalls.length - 1].getScaledBitmap().getWidth();
		int xPosition = 
			frameMidpoint - ( (numbers.length / 2) * imageSize );
		for (int i = 0; i < numbers.length; i++) {
			int ballNumber = numbers[i];
			Log.d(LOG_TAG, "Starting x pos is " + xPosition 
					+ " for ball number " + ballNumber);
			LotteryBall ball = this.lotteryBalls[ballNumber];
			ball.setXPos(xPosition);
			ball.setYPos(startingY);
			chosenNumbers[i] = ball;
			if (ball.getParent() == null) {
				frame.addView(ball);
			}
			ball.startMoving(i);
			xPosition += imageSize;
		}
	}

	private void clearOldImages() {
		
		if (chosenNumbers != null) {
			for (int i = 0; i < chosenNumbers.length; i++) {
				View lotteryBallView = chosenNumbers[i];
				ViewGroup parent = (ViewGroup)lotteryBallView.getParent();
				parent.removeView(lotteryBallView);
			}
		}
	}
	
	private Integer[] sortBalls(LotteryBall[] chosenNumbers) {

		Integer[] numbers = new Integer[chosenNumbers.length];
		for (int i = 0; i < chosenNumbers.length; i++) {
			numbers[i] = chosenNumbers[i].getNumber();
		}
		Arrays.sort(numbers);
		
		return numbers;
	}
	
	private String arrayToString(Object[] array) {
		
		String arrayString = "[";
		for (int i = 0; i < array.length; i++) {
			arrayString += array[i];
			if (i != array.length - 1) {
				arrayString += ",";
			}
		}
		arrayString += "]";
		
		return arrayString;
	}
	
	private int getYDestination() {
		return frame.getHeight() / 2;
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
		
		public void startMoving(int numberOfPauses) {
			
			long pauseTime = numberOfPauses * PAUSE_INCREMENT;
			ScheduledExecutorService executor = 
				Executors.newScheduledThreadPool(1);

			moverFuture = executor.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {

					if ( stillMoving() ) {
						frame.post( new Runnable() {
							@Override
							public void run() {
								LotteryBall.this.invalidate();
							}
						});
					} else {
						stopMoving();
						frame.post( new Runnable() {
							@Override
							public void run() {
								soundPool.play(ballLandedSoundID, 
										streamVolume, 
										streamVolume, 
										0, 
										0, 
										1);
							}
						});
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
			
			int destinationY = getYDestination();
			if ( yPos < destinationY ) {
				return true;
			} else {
				// level out all images at the same y position
				yPos = destinationY;
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
		
		private Bitmap getScaledBitmap() {
			return scaledBitmap;
		}
		
		public int getNumber() {
			return number;
		}
	}
}
