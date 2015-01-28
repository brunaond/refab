package cz.uceeb.refab;


import java.io.File;
import java.io.FilenameFilter;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ListFilesActivity extends ListActivity {
	static String logFiles[];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		//String file_wav;
		String extState = Environment.getExternalStorageState();
		File f;
		
		if(extState.equals(Environment.MEDIA_MOUNTED)){
			f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
		} else {
			Log.d("PLR", "Media not mounted");
			f = null;
		}
						
		FilenameFilter filterWav = new FilenameFilter() {		
			@Override
			public boolean accept(File dir, String fileName) {
				if (fileName.matches("^rec.*wav$")) {
					return true;
				} else {
					return false;					
				}
			}};
			
		logFiles = f.list(filterWav);

		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 
				logFiles));		
		
		Log.d("PLR", "New Intent List.");
		Log.d("Files", "Size: "+ logFiles.length);
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Intent intent;
		Log.d("PLR","Soubor " + logFiles[position]);
		intent = new Intent(this, MeasureActivity.class);
		intent.putExtra("cz.uceeb.refab.logFiles", logFiles[position]);
		startActivity(intent);
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_activity_list_files, menu);
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
}

