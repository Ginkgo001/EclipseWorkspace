package cn.lois.video.wsplayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class OnlinePlayer extends Activity {

	private VideoView mVideoView = null;
	private TextView mStatusText = null;
	private GridView mToolsView;
	private Handler mHandler = new Handler(); 
	
	OutputStream outstream;
	InputStream h264stream;
	boolean mPlaying;

	public void onCreate(Bundle savedInstanceState) 
	{ 
		super.onCreate(savedInstanceState);
       
		// No Title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
       
		setContentView(R.layout.online);
		
		mVideoView = (VideoView) findViewById(R.id.videoView);
		mStatusText = (TextView) findViewById(R.id.statusText);
		
		mToolsView = (GridView) findViewById(R.id.toolsView);
		
		Intent i = this.getIntent();   
        
		Bundle b = i.getExtras(); 
		mId = b.getInt("id");
		mTicket = b.getInt("ticket");
		mIp = b.getString("ip");
		mPort = b.getInt("port");
		mSkey = b.getString("skey");
		mRight = b.getInt("right");

		if ((mRight & 0x408) != 0) { // 具有云台操作的权限
			mToolsView.setAdapter(new ImageListAdapter(this, new String[] {
					"返回", "音频", "截图", "录像",
					"上旋", "下旋", "左旋", "右旋", 
					"放大", "缩小", "巡逻", "停止"},
					new int[] {
					R.drawable.back, R.drawable.mute, R.drawable.cut, R.drawable.movie,
					R.drawable.ptz_0, R.drawable.ptz_1, R.drawable.ptz_2, R.drawable.ptz_3, 
					R.drawable.ptz_4, R.drawable.ptz_5, R.drawable.ptz_9, R.drawable.ptz_10
				}));
		       registerForContextMenu(mToolsView);
		}
		else {
			mToolsView.setAdapter(new ImageListAdapter(this, new String[] {
					"返回", "音频", "截图", "录像"
				}, new int[] {
						R.drawable.back, R.drawable.mute, R.drawable.cut, R.drawable.movie }
			));
		}
		mToolsView.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> adapter,//The AdapterView where the click happened   
					View view,//The view within the AdapterView that was clicked
					int position,//The position of the view in the adapter   
					long rowid//The row id of the item that was clicked   
					) 
			{
				switch(position)
				{
				case 0: // 返回
					mPlaying = false;
					break;
				case 1: // 音频
					startMedia[0] = 1;
					if (mVideoView.IsAudioOUt()) {
						mVideoView.StopAudio();
					} else {
						mVideoView.StartAudio();
						startMedia[0] += 8;
					}
					if (mVideoView.IsAudioOUt()) {
						view.setBackgroundColor(0xFFF7F6F3);
					} else {
						view.setBackgroundColor(Color.TRANSPARENT);
					}
					try {
						SendPacket((byte)2, (byte)250, startMedia, 4);
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				case 2: // 截图
					mVideoView.Snap();
					break;
				case 3: // 录像
					if (mVideoView.Record()) {
						try {
							SendPacket((byte)2, (byte)250, startMedia, 4);
						} catch (IOException e) {
							e.printStackTrace();
						}
						view.setBackgroundColor(0xFFF7F6F3);
					} else {
						view.setBackgroundColor(Color.TRANSPARENT);
					}
					break;

				case 4:
					sendCmd(0);
					break;
				case 5:
					sendCmd(1);
					break;
				case 6:
					sendCmd(2);
					break;
				case 7:
					sendCmd(3);
					break;
				case 8:
					sendCmd(4);
					break;
				case 9:
					sendCmd(5);
					break;
				case 10:
					sendCmd(9);
					break;
				case 11:
					sendCmd(10);
					break;
				}
			}   
			
		}); 

		this.setResult(RESULT_OK, new Intent());   
		
		mVideoView.InitVideoView(mStatusText);
		mPlaying = true;
		new Thread(new Runnable(){
			public void run() { 
				StartOnline();
			}
		}).start();
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {   
		super.onCreateContextMenu(menu, v, menuInfo);   
		menu.add(0, 0, 0, "步进模式").setCheckable(true).setChecked(stepMode);   
	} 
	
	public boolean onContextItemSelected(MenuItem item) {   
		  switch (item.getItemId()) {   
		  case 0:   
			  stepMode = !stepMode;
			  item.setChecked(stepMode);
			  return true;   
		  default:   
		    return super.onContextItemSelected(item);   
		  }   
		}   

   
    boolean stepMode = false;
    private void sendCmd(int num) {
    	String cmd = "ptzctr -act1 ";
    	cmd += num;
    	if (stepMode) cmd += " -act2 10";
    	cmd += "\r\n";
    	byte[] p = cmd.getBytes();
    	try {
			SendPacket((byte)1, (byte)254, p, p.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	private byte[] head = new byte[4];
	private boolean RecvOnce(InputStream stream, byte[] b, int pos, int count) throws IOException
	{
		if (count == 0) return true;
		int total = 0;
		int r = 0;
		while((r = stream.read(b, pos + total, count - total)) >= 0)
		{
			total += r;
			if (total == count) return true;
		}
		if (total == count)
			return true;
		else
			return false;
	}
	private void SendPacket(byte chnl, byte pt, byte[] p, int length) throws IOException
	{
		byte[] packet = new byte[4 + length];
		packet[0] = pt;
		packet[1] = chnl;
		packet[2] = (byte)(length & 0xFF);
		packet[3] = (byte)((length>>8) & 0xFF);
		System.arraycopy(p, 0, packet, 4, length);
		outstream.write(packet, 0, 4+length);
		outstream.flush();
	}
	private byte[] startMedia = new byte[4];
	private boolean ReadOneTcp(InputStream stream, ByteBuffer buffer)
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
					if (RecvOnce(stream, b, 0, len))
					{
						if (head[0] == 0)
						{
							startMedia[0] = 1;
							startMedia[1] = 0;
							startMedia[2] = 0;
							startMedia[3] = 0;
							SendPacket((byte)2, (byte)250, startMedia, 4);
						}
						else if (head[0] == 22) // video
						{
							buffer.position(len);
							return true;
						}
						else if (head[0] == 23 && mVideoView.IsAudioOUt()) // audio
						{
							buffer.position(len);
							mVideoView.PutAudio(buffer);
						}
						else if (head[0] == 21) // alert
						{
							ReportAlert(b, len);
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
			Log.v("test", e1.toString());
		}Log.v("test", "exit");
		return false;
	}
	
	String msg = "";
	private void ReportAlert(byte[] b, int len) {
		if (len > 20) {
			byte[] bb = new byte[len - 20];
			System.arraycopy(b, 20, bb, 0, len-20);
			try {
				msg = new String(bb, "gb2312");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if (msg.length() > 0) {
			
		}
		else if (b[12] == 31) msg = "移动侦测";
		else if (b[12] == 32) msg = "输入告警";
		else if (b[12] == 33) msg = "视频丢失";
		else if (b[12] == 34) msg = "视频覆盖";
		else msg = "报警设备";
        mHandler.post(new Runnable() {
            public void run() {
            	mStatusText.setText(msg);
            }
        });
	}
	int mId; int mTicket; String mIp; int mPort; String mSkey; int mRight;
	private void StartOnline() {
		Socket socket = null;
		mPlaying = true;
		try 
		{
			socket = new Socket(mIp, mPort);
			h264stream = socket.getInputStream();
			outstream = socket.getOutputStream();
			
			String setup = "Setup:";
			setup += mId;
			setup += ":";
			setup += mTicket;
			if (mSkey.length() > 2)
			{
				setup += ":";
				setup += mSkey;
			}
			setup += "\r\n";
			byte[] send = setup.getBytes();
			outstream.write(send);
			outstream.flush();
			
			ByteBuffer pInBuffer = ByteBuffer.allocate(51200);
			
			while (mPlaying && ReadOneTcp(h264stream, pInBuffer)) 
			{ 
				mVideoView.Decode(pInBuffer);
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
