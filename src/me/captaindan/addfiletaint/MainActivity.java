package me.captaindan.addfiletaint;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Files;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	String mySharedPath = Environment.getExternalStorageDirectory().getPath()+"/Shared";
	String myDownloadPath = Environment.getExternalStorageDirectory().getPath()+"/Download";
	String myPicturesPath = Environment.getExternalStorageDirectory().getPath()+"/Pictures";
	String myDCIMCameraPath = Environment.getExternalStorageDirectory().getPath()+"/DCIM/Camera";
	Directory dir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
    	((EditText) findViewById(R.id.directory_text)).setText(mySharedPath);
		dir = new Directory(mySharedPath);
    	UpdateFileList task = new UpdateFileList();
    	task.execute(new String[]{((EditText) findViewById(R.id.directory_text)).getText().toString()});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    public void sharedFldrPress(View view){
    	((EditText) findViewById(R.id.directory_text)).setText(mySharedPath);
    	refreshPress(view);
    }
    
    public void downloadFldrPress(View view){
    	((EditText) findViewById(R.id.directory_text)).setText(myDownloadPath);
    	refreshPress(view);
    }
    
    public void picturesFldrPress(View view){
    	((EditText) findViewById(R.id.directory_text)).setText(myPicturesPath);
    	refreshPress(view);
    }
    
    public void dcimcameraFldrPress(View view){
    	((EditText) findViewById(R.id.directory_text)).setText(myDCIMCameraPath);
    	refreshPress(view);
    }
	
    public void refreshPress(View view){
    	UpdateFileList task = new UpdateFileList();
    	task.execute(new String[]{((EditText) findViewById(R.id.directory_text)).getText().toString()});
    }
    
    public void infoPress(View view){
    	GetFileInfo task = new GetFileInfo();
    	task.execute(new String[]{((EditText) findViewById(R.id.filename_text)).getText().toString()});
    }
    
    public void taintPress(View view){
    	AddFileTaint task = new AddFileTaint();
    	task.execute(new String[]{((EditText) findViewById(R.id.filename_text)).getText().toString()});
    }
    
    public void sharePress(View view){
    	ShareFile task = new ShareFile();
    	task.execute(new String[]{((EditText) findViewById(R.id.filename_text)).getText().toString()});
    }
    
    public void deletePress(View view){
    	new AlertDialog.Builder(this)
    	.setTitle("Confirmation Required.")
    	.setMessage("Do you really want to delete this file?")
    	.setIcon(android.R.drawable.ic_dialog_alert)
    	.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
    	    public void onClick(DialogInterface dialog, int whichButton) {
    	    	DeleteFile task = new DeleteFile();
    	    	task.execute(new String[]{((EditText) findViewById(R.id.filename_text)).getText().toString()});
		}})
    	 .setNegativeButton(android.R.string.no, null).show();  	
}
    
    private class UpdateFileList extends AsyncTask<String, Void, String>{
		@Override
		protected String doInBackground(String... params) {
			dir.newDirectory(params[0]);
			return dir.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			((TextView) findViewById(R.id.directorylist_text)).setText(result);
			Toast.makeText(getApplicationContext(), "Refreshed", Toast.LENGTH_SHORT).show();
			super.onPostExecute(result);
		}
    }
    
    private class GetFileInfo extends AsyncTask<String, Void, String>{
		@Override
		protected String doInBackground(String... params) {
			TaintFile tfile = new TaintFile(dir.getPath(),params[0]);
			if (!tfile.exists()) return "File does not exist";
			return tfile.getFileInfo();
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}
    }
    
    private class AddFileTaint extends AsyncTask<String, Void, String>{
		@Override
		protected String doInBackground(String... params) {
			TaintFile tfile = new TaintFile(dir.getPath(),params[0]);
			if (!tfile.exists()) return "File does not exist";
			tfile.addTaint(TaintFile.TAINT_TFTACUSTOM);
			return "Tainted File:\n"+tfile.getFileInfo();
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}
    }
    
    private class ShareFile extends AsyncTask<String, Void, String>{
		@Override
		protected String doInBackground(String... params) {
			TaintFile tfilesrc = new TaintFile(dir.getPath(),params[0]);
			if (!tfilesrc.exists()) return "File does not exist";
			TaintFile tfiledst = new TaintFile(mySharedPath,params[0]);
		    if (tfiledst.exists()) return "File already in share.";
			try{
			    InputStream in = new FileInputStream(tfilesrc);
			    OutputStream out = new FileOutputStream(tfiledst);
	
			    byte[] buf = new byte[1024];
			    int len;
			    while ((len = in.read(buf)) > 0) {
			        out.write(buf, 0, len);
			    }
			    in.close();
			    out.close();
			}catch(IOException e){
				Log.e("TaintShareFile",e.getMessage());
				return "Issue copying: "+tfilesrc.getName();
			}
		    tfiledst.addTaint(tfilesrc.getTaintInt());
			return "Copied: "+tfilesrc.getName();
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}
    }
    
    private class DeleteFile extends AsyncTask<String, Void, String>{
		@Override
		protected String doInBackground(String... params) {
			TaintFile tfile = new TaintFile(dir.getPath(),params[0]);
			if (!tfile.exists()) return "File does not exist";
			tfile.delete();
			return "Deleted: "+tfile.getName();
		}

		@Override
		protected void onPostExecute(String result) {
			Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			super.onPostExecute(result);
		}
    }
}
