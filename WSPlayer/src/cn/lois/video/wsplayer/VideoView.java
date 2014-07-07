package cn.lois.video.wsplayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView; 

public class VideoView extends  SurfaceView implements SurfaceHolder.Callback, OnGestureListener {
	private final Paint mPaint = new Paint();
	private SurfaceHolder mHolder = null; 	
	private H264decode Decode = null;
	private G726AudioPlayer audio = null;
	private ByteBuffer bmp = ByteBuffer.allocate(352 * 288 * 4 + 14 + 40);
	
	private GestureDetector gestureScanner;
    /**
     * mStatusText: text shows to the user in some run states
     */
    private TextView mStatusText;

   /**
    * Constructs a SnakeView based on inflation from XML
     * 
    * @param context
    * @param attrs
    */
   public VideoView(Context context, AttributeSet attrs) {
    	super(context, attrs);
    }

   public VideoView(Context context, AttributeSet attrs, int defStyle) {
    	super(context, attrs, defStyle);
    }

   public void InitVideoView(TextView statusView) {
		mStatusText = statusView;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        if (mHolder == null) {
        	mHolder = getHolder();
        	mHolder.addCallback(this);
        	// mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); 
        }
        if (Decode != null)
        {
        	Decode.Cleanup();
        	Decode = null;
        }
        Decode = new H264decode();

        if (gestureScanner == null)
        {
	        gestureScanner = new GestureDetector(this);   
	        gestureScanner.setOnDoubleTapListener(new GestureDetector.OnDoubleTapListener(){  
	          public boolean onDoubleTap(MotionEvent e) {  
	            //双击时产生一次
	            Log.v("test", "onDoubleTap");
	            realsize = !realsize;
	            return false;  
	          }
	          public boolean onDoubleTapEvent(MotionEvent e) {  
	            //双击时产生两次
	            Log.v("test", "onDoubleTapEvent");
	            return false;
	          }  
	          public boolean onSingleTapConfirmed(MotionEvent e) {  
	            //短快的点击算一次单击
	            Log.v("test", "onSingleTapConfirmed");
	            return false;  
	          }  
	        });   
        }
   }
   
   public void surfaceCreated(SurfaceHolder holder) {
       // The Surface has been created, acquire the camera and tell it where
       // to draw.
   }

   public void surfaceDestroyed(SurfaceHolder holder) {
       // Surface will be destroyed when we return, so stop the preview.
       // Because the CameraDevice object is not a shared resource, it's very
       // important to release it when the activity is paused.
   }

   public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
       // Now that the size is known, set up the camera parameters and begin
       // the preview.
   } 
   
   public void StartAudio() {
	   if (audio == null) {
		   audio = new G726AudioPlayer(Decode);
	   }
   }
   
   public void StopAudio() {
	   if (audio != null) {
		   audio.Stop();
		   audio = null;
	   }
   }
   
   public void PutAudio(ByteBuffer pInBuffer) {
	   if (audio == null) {
		   StartAudio();
	   }
	   audio.Decode(pInBuffer);
   }
   
   public boolean IsAudioOUt() {
	   return audio != null;
   }
   
   FileOutputStream recordStream = null;
   boolean hasSPS = false;
   private boolean realsize = false;
   private Rect lastDraw = new Rect();
   public boolean Decode(ByteBuffer pInBuffer) throws InterruptedException
   {
	   bmp.position(0);
	   byte[] input = pInBuffer.array();
	   if (recordFile != null || recordStream != null){
		   try {
			   if (!hasSPS && recordStream != null) {
				   recordStream.close();
				   recordStream = null;
			   }
			   if (recordFile != null && recordStream == null) {
				   if ((input[4] & 0x1f) == 7) //判断是否关键帧
				   {
					   hasSPS = true;
					   recordStream = new FileOutputStream(recordFile);
				   }
			   }
			   if (recordStream != null && hasSPS) {
				   int len = pInBuffer.position();
				   int padding = (len % 4) != 0 ? 4 - (len % 4) : 0;
				   int qlen = (len + padding) / 4;
				   recordStream.write(0);
				   recordStream.write(padding);
				   recordStream.write(qlen & 0xFF);
				   recordStream.write((qlen >> 8) & 0xFF);
				   recordStream.write(input, 0, len);
				   for(int i=0;i<padding;i++)
					   recordStream.write(0);
			   }
			} catch (IOException e) {
				e.printStackTrace();
				recordFile = null;
			}
	   }
	   input[0] = 0;
	   input[1] = 0;
	   input[2] = 0;
	   input[3] = 1;
		int DecodeLength = Decode.DecodeOneFrame(pInBuffer, bmp);
		//如果解码成功，把解码出来的图片显示出来 
		if(DecodeLength > 0 && bmp.position() > 0)
		{ 
			//转换RGB字节为BMP  
			byte[] buffer = bmp.array();
			if (snapFile != null) {
				try {
					FileOutputStream fos = new FileOutputStream (snapFile);
					fos.write(buffer, 0, bmp.position());
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} 
				snapFile = null;
			}
        	Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, bmp.position());
			Rect src = new Rect();
			src.left = 0;
			src.top = 0;
			src.right = bitmap.getWidth();
			src.bottom = bitmap.getHeight();
			Rect dist = new Rect();
			dist.left = 0;
			dist.top = 0;
			dist.right = getWidth();
			dist.bottom = getHeight();
			double zoom = 1.0 * dist.right / src.right;
			double zoom2 = 1.0 * dist.bottom / src.bottom;
			if (zoom2<zoom)
			{
				zoom = zoom2;
				dist.left = (int) (dist.right / 2 - src.right * zoom / 2);
				dist.right = dist.left + (int) (src.right * zoom);
			}
			else
			{
				dist.top = (int) (dist.bottom / 2 - src.bottom * zoom / 2);
				dist.bottom = dist.top + (int) (src.bottom * zoom);
			}
			if (realsize && zoom > 1) //按实际大小显示
			{
				dist.left = (getWidth() - src.right) / 2;
				dist.top = (getHeight() - src.bottom) / 2;
				dist.right = dist.left + src.right;
				dist.bottom = dist.top + src.bottom;
			}
        	Rect lockRect = dist;
        	
        	if (lastDraw != null && !lastDraw.equals(dist)) // 如果显示区域改变，则需要锁定的区域包含上次显示的部分，用于清除上次的图像
        	{
	        	lockRect = new Rect();
	        	lockRect.left = dist.left < lastDraw.left ? dist.left : lastDraw.left;
	        	lockRect.top = dist.top < lastDraw.top ? dist.top : lastDraw.top;
	        	lockRect.right = dist.right > lastDraw.right ? dist.right : lastDraw.right;
	        	lockRect.bottom = dist.bottom > lastDraw.bottom ? dist.left : lastDraw.bottom;
	        	if (lockRect.right < getWidth()) // 消除大图像缩小后右面和底部的一个像素的残留
	        		lockRect.right += 1;
	        	if (lockRect.bottom < getHeight())
	        		lockRect.bottom += 1;
        	}
        	Canvas c = null;
        	c = mHolder.lockCanvas(lockRect);
        	if (c == null) {
        		Thread.sleep(1);
        		c = mHolder.lockCanvas(lockRect);
            	if (c == null) {
            		return false;
            	}
        	}
        	// 锁定整个画布，在内存要求比较高的情况下，建议参数不要为null
        	try { 
        		synchronized(mHolder) {  
        			if (lastDraw != null && !lastDraw.equals(dist))
        			{
        				c.drawARGB(255, 0, 0, 0);
        			}
        			
        			c.drawBitmap(bitmap, src, dist, mPaint);
        			lastDraw = dist;
        		}             
        	} catch(Exception ex) {
        		Log.e("player", ex.getMessage());
        		ex.printStackTrace();
        	}    
        	finally {      
        		mHolder.unlockCanvasAndPost(c);     
        		//更新屏幕显示内容           
        	}

			return true;
		}
		return false;
   }
   
   private File snapFile = null;
   public void Snap() {
	   File dir = LocalRecordList.getSnapPath();//new File("/sdcard/wsplayer/snap/");
	   dir.mkdirs();
	   if (dir.exists()) {
		   snapFile = new File(dir, System.currentTimeMillis() + ".bmp");
		   Toast.makeText(this.getContext(), snapFile.getPath(), Toast.LENGTH_LONG).show();
	   }
	   else
		   mStatusText.setText("没有存储卡或者无法在存储卡上创建文件");
   }
   
   private File recordFile = null;
   public boolean Record() {
	   if (recordFile != null) {
		   recordFile = null;
	   }
	   else {
		   File dir = LocalRecordList.getRecordPath();//new File("/sdcard/wsplayer/record/");
		   dir.mkdirs();
		   hasSPS = false;
		   if (dir.exists())
			   recordFile = new File(dir, System.currentTimeMillis() + ".media");
		   else
			   mStatusText.setText("没有存储卡或者无法在存储卡上创建文件");
	   }
	   return recordFile != null;
   }
   
   public void Cleanup()
   {
	   if (Decode != null)
	   {
		   Decode.Cleanup();
		   Decode = null;
	   }
	   recordFile = null;
	   snapFile = null;
	   if (recordStream != null) {
		   try {
				recordStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		   recordStream = null;
	   }
	   StopAudio();
   }
   
   @Override  
   public boolean onTouchEvent(MotionEvent me) {   
     return gestureScanner.onTouchEvent(me);   
   }  
   
	public boolean onDown(MotionEvent e) {
		return true;
	}
	
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		Log.v("test", "onFling "+e1.getX()+" "+e2.getX());
		return true;
	}
	
	public void onLongPress(MotionEvent e) {
		Log.v("test", "onLongPress");	
	}
	
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		Log.v("test", "onScroll "+e1.getX()+" "+e2.getX());
		return true;
	}
	
	public void onShowPress(MotionEvent e) {
		Log.v("test", "onShowPress");		
	}
	
	public boolean onSingleTapUp(MotionEvent e) {
		Log.v("test", "onSingleTapUp");
		return true;
	}
}
