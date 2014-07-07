package cn.lois.video.wsplayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
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

public class RecordPlayer extends Activity {

	private VideoView mVideoView = null;
	private TextView mStatusText = null;
	private GridView mToolsView;
	private Handler mHandler = new Handler(); 
	
	OutputStream outstream;
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
       
		setContentView(R.layout.record);
		
		mVideoView = (VideoView) findViewById(R.id.videoView);
		mStatusText = (TextView) findViewById(R.id.statusText);
		
		mToolsView = (GridView) findViewById(R.id.toolsView);
		
		Intent i = this.getIntent();   
        
       Bundle b = i.getExtras(); 
       mIp = b.getString("ip");
       mPort = b.getInt("port");
       mRecordId = b.getString("recordId");
       mSkey = b.getString("skey");
       
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
				

		this.setResult(RESULT_OK, new Intent());   
		
		mVideoView.InitVideoView(mStatusText);
		mPlaying = true;
		mPause = false;
		new Thread(new Runnable(){
			public void run() { 
				StartPlayRecord();
			}
		}).start();
	}
	String mIp; int mPort; String mRecordId; String mSkey;

	private byte[] head = new byte[4];

	
	private boolean RecvOnce(InputStream stream, byte[] b, int pos, int count) throws IOException
	{
		if (count == 0) return true;
		int total = 0;
		int r = 0;
		while((r = stream.read(b, pos + total, count - total)) > 0)
		{
			total += r;
			if (total == count) return true;
		}
		return false;
	}
	private boolean ReadOneRecord(InputStream stream, ByteBuffer buffer)
	{
		buffer.position(0);
		byte[] b = buffer.array();
		try
		{
			while(mPlaying)
			{
				if (RecvOnce(stream, head, 0, 4))
				{
					int len = (( 0xFF & head[3]) * 256 + (0xFF & head[2]));
					len = len * 4 + head[1];
					if (len > 0 && RecvOnce(stream, b, 0, len))
					{
						if (head[0] == 255)
						{
							return false;
						}
						else if (head[0] == 0) // video
						{
							buffer.position(len);
							return true;
						}
						else if (head[0] == 1) // audio
						{
							buffer.position(len);
							mVideoView.PutAudio(buffer);
						}
						continue;
					}
				}
				break;
			}
		}
		catch (Exception e1)
		{ 
			e1.printStackTrace();
		}
		return false;
	}

	private void StartPlayRecord() {
		Socket socket = null;
		mPlaying = true;
		try 
		{
			socket = new Socket(mIp, mPort);
			h264stream = socket.getInputStream();
			outstream = socket.getOutputStream();
			
			String setup = "Setup!:";
			setup += mRecordId;
			setup += "\r\nStart\r\n";
			byte[] send = setup.getBytes();
			outstream.write(send);
			outstream.flush();
			
			ByteBuffer pInBuffer = ByteBuffer.allocate(51200);
			
			long playTime = System.currentTimeMillis() - 40;
			byte[] in = pInBuffer.array();
			while (mPlaying && ReadOneRecord(h264stream, pInBuffer)) 
			{ 
				mVideoView.Decode(pInBuffer);
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
				socket.shutdownInput();
				socket.shutdownOutput();
				socket.close();
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
			if (mStatusText.getText().length() > 0) {
				mStatusText.setText("");
				return true;
			}
			else if (mToolsView.getVisibility() == View.VISIBLE) {
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
