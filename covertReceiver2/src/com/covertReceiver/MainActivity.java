package com.covertReceiver;


import java.io.ByteArrayOutputStream;

import com.covertReceiver.R;

import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
public class MainActivity extends Activity {
	int i=0;
	ByteArrayOutputStream inImage;
	private ImageView iv_image;
	private Bitmap bmp;
	private TextView text;
	int g;
	String bas="";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		inImage = new ByteArrayOutputStream(); 
		iv_image = (ImageView) findViewById(R.id.imageView1);
		Button show = (Button)findViewById(R.id.button1); 
		text = (TextView)findViewById(R.id.textView);
		
		show.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(bas.length()>100)
					printData();
				else
					printData2();
			}
		});
		
		BroadcastReceiver receiver=new BroadcastReceiver() {
			AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			int mode;
			@Override
			public void onReceive(Context context, Intent intent) {
				mode = am.getRingerMode();
				if(mode==0) {
					bas=bas+0;
					am.setRingerMode(2); //Send ack
					//Log.e("got","0");
				} else if(mode==1) {
					bas=bas+1;
					am.setRingerMode(2); //Send ack
					//Log.e("got","1");
				}
				
			}

		};
		IntentFilter filter=new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
		registerReceiver(receiver,filter);
	}
	public void printData() { //print image
		bas = bas.substring(0, bas.length()-2);
		//Log.e("data",bas);
		for(int i=0;i<bas.length()-8;i+=8) {
			inImage.write((byte)Integer.parseInt(bas.substring(i, i+8), 2));	
		}
		bas="";
		bmp = BitmapFactory.decodeByteArray(inImage.toByteArray() , 0, inImage.toByteArray().length);
		iv_image.setImageBitmap(bmp);
	}
	public void printData2() { //print data sent from text field
		String data = Integer.toString(Integer.parseInt(bas,2));
		//Log.e("Data received",data);
		text.setText("Received: " + data);
	}
}
