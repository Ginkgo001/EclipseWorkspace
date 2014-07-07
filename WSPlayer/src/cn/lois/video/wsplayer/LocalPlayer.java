package cn.lois.video.wsplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class LocalPlayer extends Activity {

	private VideoView mVideoView = null;
	private TextView mStatusText = null;
	private GridView mToolsView;
	private Handler mHandler = new Handler(); 
	
	InputStream h264stream;
	boolean mPlaying;
	boolean mPause;

	public void onCreate(Bundle savedInstanceState) 
	{ 
		super.onCreate(savedInstanceState);
       
		// No Title bar
       requestWindowFeature(Window.FEATURE_NO_TITLE);
       getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
       getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      
		setContentView(R.layout.local);
		
		mVideoView = (VideoView) findViewById(R.id.videoView);
		mStatusText = (TextView) findViewById(R.id.statusText);
		
		mToolsView = (GridView) findViewById(R.id.toolsView);
		
		String[] names = new String[] {"停止", "暂停", "截图", "录像"};
		int[] icons = new int[] { R.drawable.back, R.drawable.pause, R.drawable.cut, R.drawable.movie};

		mToolsView.setAdapter(new ImageListAdapter(this, names, icons));
		mToolsView.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> adapter,//The AdapterView where the click happened   
					View view,//The view within the AdapterView that was clicked
					int position,//The position of the view in the adapter   
					long rowid//The row id of the item that was clicked   
					) 
			{
				switch(position)
				{
				case 0:
					mPlaying = false;
					break;
				case 1:
					mPause = !mPause;
					if (mPause) {
						view.setBackgroundColor(0xFFF7F6F3);
					} else {
						view.setBackgroundColor(Color.TRANSPARENT);
					}
					break;
				case 2: // 截图
					mVideoView.Snap();
					break;
				case 3: // 录像
					if (mVideoView.Record()) {
						view.setBackgroundColor(0xFFF7F6F3);
					} else {
						view.setBackgroundColor(Color.TRANSPARENT);
					}
					break;
				}
			}   
		}); 
		Intent i = new Intent();   
		this.setResult(RESULT_OK, i);   
		
		mVideoView.InitVideoView(mStatusText);
		i = this.getIntent();   
        
       Bundle b = i.getExtras(); 
		h264file = new File(b.getString("h264file"));
		mPlaying = true;
		mPause = false;
		new Thread(new Runnable(){
			public void run() { 
				StartDecode();
			}
		}).start();
	}

	private byte[] head = new byte[4];
	private boolean ReadOne(InputStream stream, ByteBuffer buffer)
	{
		buffer.position(0);
		byte[] b = buffer.array();
		try
		{
			if (stream.read(head) == 4)
			{
				int len = head[1] + (( 0xFF & head[3]) * 256 + (0xFF & head[2])) * 4;
				if (stream.read(b, 0, len) == len)
				{
					buffer.position(len);
					return true;
				}
			}
		}
		catch (Exception e1)
		{ 
			e1.printStackTrace();
		}
		return false;
	}
	
	File h264file;
	private void StartDecode()
	{ 
		long playTime = System.currentTimeMillis() - 40;
		try 
		{
			h264stream = new FileInputStream(h264file);
			ByteBuffer pInBuffer = ByteBuffer.allocate(51200);
			byte[] in = pInBuffer.array();
			while (mPlaying && ReadOne(h264stream, pInBuffer)) 
			{ 
				if (head[0] == 0) { // video
					mVideoView.Decode(pInBuffer);
				} else if (head[0] == 1) { // audio
					mVideoView.PutAudio(pInBuffer);
				}
				playTime += ( 0xFF & in[1]) * 256 + (0xFF & in[0]);
				long sleep = playTime - System.currentTimeMillis();
				if (sleep > 0)
				{
					Thread.sleep(sleep);
				}
				else
				{
					playTime -= sleep;
				}
				while(mPause && mPlaying) {
					Thread.sleep(100);
				}
			}
		}
		catch (Exception e1)
		{ 
			e1.printStackTrace();
		}
		finally
		{
			mPlaying = false;
			try
			{
				h264stream.close();
			}
			catch(Exception e)
			{
				
			}
	    	mHandler.post(new Runnable() {
	            public void run() {
		    		finish();
	            }
	        });
		}
	}
	
	protected void onDestroy() 
	{
		super.onDestroy();
		mVideoView.Cleanup();
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // land do nothing is ok
            } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    // port do nothing is ok
            }
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mToolsView.getVisibility() == View.VISIBLE) {
				mToolsView.setVisibility(View.INVISIBLE);
				return true;
			}
			else if (mPlaying){
				mPlaying = false;
			    return true;
			}
			else
			{
				finish();
				return true;
			}
		 }
		else if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			if (mToolsView.getVisibility() == View.VISIBLE) {
				mToolsView.setVisibility(View.INVISIBLE);
				return true;
			}
			else {
				mToolsView.setVisibility(View.VISIBLE);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
   }
}
