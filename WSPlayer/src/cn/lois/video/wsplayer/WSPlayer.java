package cn.lois.video.wsplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings; 
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebViewClient;

/**
 * Demonstrates how to embed a WebView in your activity. Also demonstrates how
 * to have javascript in the WebView call into the activity, and how the activity 
 * can invoke javascript.
 * <p>
 * In this example, clicking on the android in the WebView will result in a call into
 * the activities code in {@link DemoJavaScriptInterface#clickOnAndroid()}. This code
 * will turn around and invoke javascript using the {@link WebView#loadUrl(String)}
 * method.
 * <p>
 * Obviously all of this could have been accomplished without calling into the activity
 * and then back into javascript, but this code is intended to show how to set up the 
 * code paths for this sort of communication.
 *
 */
public class WSPlayer extends Activity {
	private static final double CURRENT_VIRSION = 0.8; // 当前版本号
	private static final String USER_NAMES_SETTINGS = "ws_users"; // 存储用户证书信息（当前仅存储密码）
	private static final String APP_SETTINGS = "ws_app"; // 存储程序配置信息
	private static final String DEFALUT_HOST = "222.45.224.53";//"video.dacas.cn";
	private TextView mStatusText = null;
	private WebView mWebView = null;
	private GridView mToolsView;
	private Handler mHandler = new Handler(); 
	private DemoJavaScriptInterface mJsInterface = null;
	
	public void onCreate(Bundle savedInstanceState) 
	{ 
		super.onCreate(savedInstanceState);
       
		// No Title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
       
		setContentView(R.layout.main);
		mStatusText = (TextView) findViewById(R.id.statusText);
		
		mToolsView = (GridView) findViewById(R.id.toolsView);

		mWebView = (WebView) findViewById(R.id.webView);
		
		WebSettings webSettings = mWebView.getSettings();
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSupportZoom(true);
		webSettings.setDefaultZoom(ZoomDensity.CLOSE);
		
		mWebView.setWebChromeClient(new MyWebChromeClient());
		mWebView.setWebViewClient(new WebViewClient() {      
		     @Override      
		     public boolean shouldOverrideUrlLoading(WebView view, String url)      
		      {      
		        view.loadUrl(url);      
		        return true;      
		      }   
		     @Override
		     public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		     {
		    	 if (errorCode == WebViewClient.ERROR_CONNECT){
		    		 mWebView.loadUrl("file:///android_asset/demo.html");
		    	 }
		     }
		     //@SuppressWarnings("unused")
		     public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error){   
		          //handler.cancel(); 默认的处理方式，WebView变成空白页   
		          handler.proceed();//接受证书   
		          //handleMessage(Message msg); 其他处理   
		      } 
		});    
		mJsInterface = new DemoJavaScriptInterface();
		mWebView.addJavascriptInterface(mJsInterface, "demo");
		
		SharedPreferences settings = getSharedPreferences(APP_SETTINGS, 0);
		mWebView.loadUrl("https://" + settings.getString("host", DEFALUT_HOST) + "/Mobile/Default.aspx?android=android"); 

		mToolsView.setAdapter(new ImageListAdapter(this, 
														new String[] {"关闭", "后退", "刷新", "切换"},
														new int[] {R.drawable.close, R.drawable.back, R.drawable.home, R.drawable.options}));
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
					WSPlayer.this.finish();
					break;
				case 1:
					if (mWebView.canGoBack()) {
					    mWebView.goBack();
					}
					break;
				case 2:
					mWebView.reload();
					break;
				case 3:
					mJsInterface.setHost();
					break;
				}
			}   
		}); 
	}
	
	protected void onAcitivityResult(int requestCode, int resultCode, Intent data) {
		switch(resultCode) {
		case RESULT_OK:
			break;
		}
	}

	int mId; int mTicket; String mIp; int mPort; String mSkey; int mRight; 
	String mRecordId;
	final class DemoJavaScriptInterface {

        DemoJavaScriptInterface() {
        }

        public double version() {
        	return CURRENT_VIRSION;
        }
        public void update(String path) {
        	SharedPreferences settings = getSharedPreferences(APP_SETTINGS, 0);
        	Uri uri = Uri.parse("https://" + settings.getString("host", DEFALUT_HOST) + path);
        	Intent web = new Intent(Intent.ACTION_VIEW, uri);
        	startActivity(web);
        }
        public void load(String path) {
        	SharedPreferences settings = getSharedPreferences(APP_SETTINGS, 0);
        	final String uri = "https://" + settings.getString("host", DEFALUT_HOST) + path;
        	
            mHandler.post(new Runnable() {
                public void run() {
                    mWebView.loadUrl(uri);
                }
            });
        }
        public void setHost() {
        	mHandler.post(new Runnable() {
        		public void run() {
        			SharedPreferences settings = getSharedPreferences(APP_SETTINGS, 0);
        			
					final EditText input = new EditText(WSPlayer.this);
					input.setText(settings.getString("host", DEFALUT_HOST));
					
					new AlertDialog.Builder(WSPlayer.this)
					.setTitle("请输入服务地址")
					.setView(input)
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Editable text = input.getText();
		        			SharedPreferences settings = getSharedPreferences(APP_SETTINGS, 0);
		        			SharedPreferences.Editor editor = settings.edit();
		        			editor.putString("host", text.toString());
		        			editor.commit();
							load("/Mobile/Default.aspx?android=android");
						}
					})
					.setOnCancelListener(new DialogInterface.OnCancelListener() {
						
						public void onCancel(DialogInterface arg0) {
							
						}
					})
					.show();
        		}
        	});
        }
        public void playLocal() {
        	mHandler.post(new Runnable() {
                public void run() {
		    		Intent i = new Intent(WSPlayer.this, LocalRecordList.class);
		    		startActivityForResult(i, 10);
                }
            });
        }
        public void playRealtime(int id, int ticket, String ip, int port, String skey, int right){
        	mId = id;
        	mTicket = ticket;
        	mIp = ip;
        	mPort = port;
        	mSkey = skey;
        	mRight = right;
        	mHandler.post(new Runnable() {
                public void run() {
            		mWebView.goBack();
		    		new Thread(new Runnable(){
		    			public void run() { 
				    		Intent i = new Intent(WSPlayer.this, OnlinePlayer.class);
							Bundle b = new Bundle();
							b.putInt("id", mId);
							b.putInt("ticket", mTicket);
							b.putString("ip", mIp);
							b.putInt("port", mPort);
							b.putString("skey", mSkey);
							b.putInt("right", mRight);
							i.putExtras(b); 
				    		startActivityForResult(i, 10);
		    			}
		    		}).start();
                }
            });
        }
        public void playRecord(String ip, int port, String recordId, String skey){
        	mIp = ip;
        	mPort = port;
        	mRecordId = recordId;
        	mSkey = skey;
        	mHandler.post(new Runnable() {
                public void run() {
            		mWebView.goBack();
		    		new Thread(new Runnable(){
		    			public void run() { 
				    		Intent i = new Intent(WSPlayer.this, RecordPlayer.class);
							Bundle b = new Bundle();
							b.putString("ip", mIp);
							b.putInt("port", mPort);
							b.putString("recordId", mRecordId);
							b.putString("skey", mSkey);
							i.putExtras(b); 
				    		startActivityForResult(i, 10);
		    			}
		    		}).start();
                }
            });
        }
       public void exit() {
            mHandler.post(new Runnable() {
                public void run() {
                	finish();
                }
            });
        }
    }
	
	ProgressDialog _progress = null;
    /**
     * Provides a hook for calling "alert" from javascript. Useful for
     * debugging your javascript.
     */
    final class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            new AlertDialog.Builder(WSPlayer.this)
            .setTitle("alert")
            .setMessage(message)
            .show();
            
            result.confirm();
            return true;
        }
        @Override
        public boolean onCreateWindow(android.webkit.WebView view, boolean dialog, boolean userGesture, android.os.Message resultMsg)
        {
        	return true;
        }
       @Override
       public void onProgressChanged(WebView view, int newProgress) {
    	   if(newProgress==100){
    		   if (_progress != null) _progress.cancel();
    		   _progress = null;
    	   }
    	   else {
    		   if (_progress == null) {
    	    	   _progress = ProgressDialog.show(WSPlayer.this, // context     
    	    			   "", // title     
    	    			   "正在加载页面...", // message     
    	    			   false); //进度是否是不确定的，这只和创建进度条有关 
    	    	   _progress.setCancelable(true);
    		   }
    		   _progress.setProgress(newProgress);
    	   }
       }
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
  
	
	protected void onDestroy() 
	{
		super.onDestroy();
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
			else if (mWebView.canGoBack()) {
			    mWebView.goBack();
			    return true;
			}
			else
			{
				new AlertDialog.Builder(WSPlayer.this)
					.setTitle("退出")
					.setMessage("要退出观星视频监控系统吗？")
					.setIcon(R.drawable.icon)
					.setPositiveButton("是", new DialogInterface.OnClickListener() { 
						public void onClick(DialogInterface dialog, int whichButton) {
							setResult(RESULT_OK);
							finish();
						}
					}).setNegativeButton("否", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							
						} 
					}).show(); 
				return true;
			}
		 }
		else if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			if (mToolsView.getVisibility() == View.VISIBLE) {
				mToolsView.setVisibility(View.INVISIBLE);
			}
			else {
				mToolsView.setVisibility(View.VISIBLE);
			}
		    return true;
		}
		return super.onKeyDown(keyCode, event);
    }
}
