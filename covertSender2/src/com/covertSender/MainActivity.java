package com.covertSender;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import com.example.sidechannel2.R;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class MainActivity extends Activity implements SurfaceHolder.Callback {
	private final static int SLEEP_TIME = 0;
	private ImageView iv_image;
	private SurfaceView sv;
	private Bitmap bmp; //a bitmap to display the captured image
	private SurfaceHolder sHolder;
	private Camera mCamera; //a variable to control the camera
	private Parameters parameters; //the camera parameters
	private byte imageOut[];
	private ProgressBar mProgress;
	private int mProgressStatus = 0;
	private AudioManager am;
	private String dataB = "";
	private ToggleButton tb;
	private TextView tvSize;
	private int i=1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mProgress = (ProgressBar) findViewById(R.id.progressBar);
		am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		tb = (ToggleButton) findViewById(R.id.toggleButton1);
		iv_image = (ImageView) findViewById(R.id.imageView);
		sv = (SurfaceView) findViewById(R.id.surfaceView);
		tvSize = (TextView) findViewById(R.id.textViewSize);
		sHolder = sv.getHolder();
		sHolder.addCallback(this);
		Button send = (Button)findViewById(R.id.sendButton);

		BroadcastReceiver receiver=new BroadcastReceiver() {
			AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			int mode; //stop 120, start 021
			boolean first=true;
			@Override

			public void onReceive(Context context, Intent intent) {
				mode = am.getRingerMode();
				if(first)
					first=false;
				else if(!first && mode==2 && i<dataB.length()) { //got ack 
					//Log.e("ack","ack");
					send(am,Character.getNumericValue(dataB.toCharArray()[i]));
					//Log.e("send",Integer.toString(Character.getNumericValue(dataB.toCharArray()[i])));
					i++;
					//update progress bar
					mProgressStatus=(int) (((double)i/(double)dataB.length())*100);
					mProgress.setProgress(mProgressStatus);
				}
			}
		};
		IntentFilter filter=new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
		registerReceiver(receiver,filter);

		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { 
				if(tb.isChecked())
					sendImage(); //checked, we send the photo
				else
					sendTextData(); //not checked, we send the data from the box
			}
			public void sendTextData() {
				AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				EditText input = (EditText)findViewById(R.id.dataField);
				String data;
				if(input.getText().toString().compareTo("")!=0)
					data=input.getText().toString();
				else
					data="42";
				int dataN=Integer.parseInt(data);
				dataB = Integer.toBinaryString(dataN);
				//Log.e("rawsend",dataB);
				//Log.e("send",Integer.toString(Character.getNumericValue(dataB.toCharArray()[0])));
				send(am,Character.getNumericValue(dataB.toCharArray()[0])); //send the first one to get it rolling
			}
		});
	}
	public static void trysleep() {
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private void send(AudioManager am, int b) {
		am.setRingerMode(b);
		trysleep();
	}
	public void sendImage() {
		new Thread(new Runnable() { //send data in a new thread
			@Override
			public 	void run() {
				am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				String oneByte="";
				for(int i=0;i<imageOut.length;i++) {
					oneByte=oneByte+String.format("%8s", Integer.toBinaryString(imageOut[i] & 0xFF)).replace(' ', '0');
				}
				//Log.e("data to send",oneByte);
				dataB = oneByte;
				send(am,Character.getNumericValue(dataB.toCharArray()[0])); //send the first one to get it rolling
			}
		}).start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) { //camera function 
		//get camera parameters
		parameters = mCamera.getParameters();
		parameters.getSupportedPictureSizes();
		//parameters.setPictureSize(512, 384);
		//set camera parameters
		mCamera.setParameters(parameters);
		mCamera.startPreview();

		//sets what code should be executed after the picture is taken
		Camera.PictureCallback mCall = new Camera.PictureCallback() {
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				//decode the data obtained by the camera into a Bitmap
				BitmapFactory.Options opt = new BitmapFactory.Options();
				opt.inMutable = true;
				bmp = BitmapFactory.decodeByteArray(data, 0, data.length,opt);
				Matrix matrix = new Matrix();
				// rotate the Bitmap
				matrix.postRotate(90);
				// recreate the new Bitmap
				bmp = Bitmap.createBitmap(bmp,0,0,320,240, matrix, true);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.WEBP, 10, out);
				bmp = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.toByteArray().length,opt);
				double s=out.size()/1000.0;
				tvSize.setText("Image Size: " + Double.toString(s) + "kb");
				imageOut=out.toByteArray();
				iv_image.setImageBitmap(bmp);
			}
		};
		mCamera.takePicture(null, null, mCall);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) { 
		// The Surface has been created, acquire the camera and tell it where to draw the preview.
		mCamera = Camera.open(0); //0 back camera, 1 front camera on Nexus 4
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			mCamera.release();
			mCamera = null;
		}
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder)  { 
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}
}
