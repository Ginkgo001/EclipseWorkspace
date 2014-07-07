package cn.lois.video.wsplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class LocalRecordList extends ListActivity  {

	public final static File getRecordPath() {
		File f = Environment.getExternalStorageDirectory();
		if (f == null) return null;
		f = new File(f.getPath() + "/wsplayer/record/");
		f.mkdirs();
		return f;
	}

	public final static File getSnapPath() {
		File f = Environment.getExternalStorageDirectory();
		if (f == null) return null;
		f = new File(f.getPath() + "/wsplayer/snap/");
		f.mkdirs();
		return f;
	}

	SimpleAdapter adapter = null;  
	private ArrayList<Map<String, Object>> mModelData = null;  
	File[] recordArray;

	@Override  
	protected void onListItemClick(ListView l, View v, int position, long id) 
	{  
		super.onListItemClick(l, v, position, id);  
		File f = recordArray[position];
		
		Intent i = new Intent(LocalRecordList.this, LocalPlayer.class);  
		Bundle b = new Bundle();
		b.putString("h264file", f.getPath());
		i.putExtras(b); 
		
		startActivityForResult(i,10);  
	}
	public void onCreate(Bundle savedInstanceState) 
	{ 
		super.onCreate(savedInstanceState);
       
		// No Title bar
       requestWindowFeature(Window.FEATURE_NO_TITLE);
       //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 
       
       initModelData();  

       registerForContextMenu(this.getListView());

       Intent i = new Intent();   
		this.setResult(RESULT_OK, i);   
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {   
		super.onCreateContextMenu(menu, v, menuInfo);   
		menu.add(0, 0, 0, "删除");   
		menu.add(0, 1, 0, "刷新");   
	} 
	
	public boolean onContextItemSelected(MenuItem item) {   
		  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();   
		  switch (item.getItemId()) {   
		  case 0:   
			  File f = recordArray[info.position];
			  f.delete();
			  initModelData();
			  return true;   
		  case 1:
			  initModelData();
			  return true;   
		  default:   
		    return super.onContextItemSelected(item);   
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
	
	public void initModelData()  {  
		mModelData = new ArrayList<Map<String, Object>>();  
		File f = getRecordPath();//new File("/sdcard/wsplayer/record/");
		f.mkdirs();
		recordArray = f.listFiles();
		if (recordArray == null)
		{
			recordArray = new File[0];
		}
		if (recordArray.length == 0) {
			//recordArray = new File[0];
            new AlertDialog.Builder(this)
            .setMessage("没有本地录像文件")
            .show();
		}
		for (int position=0;position<recordArray.length;position++){
			String name = recordArray[position].getName();
			name = name.substring(0, name.indexOf('.'));
			long timeMillis = Long.parseLong(name);
			Date d = new Date(timeMillis);
			Map<String, Object> item = new HashMap<String,Object>();  
			item.put("name", (1900+d.getYear()) + "年" + (1+d.getMonth()) + "月" + d.getDate() + "日"
					+ d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds());
			item.put("size", (recordArray[position].length()/1024) + "K");
			mModelData.add(item);
		}
		
       adapter = new SimpleAdapter(this, 
    		   mModelData, 
    		   android.R.layout.two_line_list_item, 
    		   new String[]{"name", "size"}, 
    		   new int[]{android.R.id.text1, android.R.id.text2});  
       this.setListAdapter(adapter);  
	}
}
