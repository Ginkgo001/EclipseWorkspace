package cn.lois.video.wsplayer;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.widget.SimpleAdapter;


public class ImageListAdapter extends SimpleAdapter  {
	//construct
	public ImageListAdapter(Context ctx, String[] names, int[] icons )  {
		super(ctx, //没什么解释   
				GetImageItems(names, icons),//数据来源
				R.layout.button,//button的XML实现  
				//动态数组与ImageItem对应的子项      
				new String[] {"ItemImage","ItemText"},  
				//ImageItem的XML文件里面的一个ImageView,两个TextView ID
				new int[] {R.id.ItemImage,R.id.ItemText});
	}
	
	private static ArrayList<HashMap<String, Object>> GetImageItems(String[] names, int[] icons) {
		ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();
		for (int i=0; i<names.length; i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemImage", icons[i]);//添加图像资源的ID
			map.put("ItemText", names[i]);//按序号做ItemText
			lstImageItem.add(map);  
		}
		return lstImageItem;
	}
}
