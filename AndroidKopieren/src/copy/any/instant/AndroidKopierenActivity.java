package copy.any.instant;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * This activity is the main actitivty of the project. It is the interface for the user where clipboard text contents can be send to the server. 
 * Also text and images can be retrieved from the server. Text will be put on the clipboard, images cannot. The contents on the server can be deleted. 
 * This activity also provides links to the FriendsManagerActivity.
 * @author Vincent
 *
 */
public class AndroidKopierenActivity extends Activity implements OnClickListener{

	private Button setCopy;
	private Button getCopy;
	private Button delete;

	private EditText copiedString;
	private ImageView imageView;

	private Spinner spinner;
	private String spinnerSelection;

	private String username;
	private String dataString;

	private String ServerResponseKey = "Response";	
	private String sendURL = "http://www.maxhovens.nl/copies/scripts/jsonStoreCopyString.php";
	private String directoryURL = "http://www.maxhovens.nl/copies/scripts/jsonDirList.php";
	private String deleteContentURL = "http://www.maxhovens.nl/copies/scripts/jsonDeleteContent.php";

	private HashMap<String,String> directoryMap;
	private ServerContentRetriever scr;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		setInitialStuff();
	}
	
	/**
	 * This method initializes all the views in the activity. In addition the username is retrieved and the ServerContentRetriever is initialized.
	 */
	private void setInitialStuff() {
		username = getIntent().getExtras().getString("Username");
		scr = new ServerContentRetriever(username,getResources());

		spinner = (Spinner) findViewById(R.id.spinner);
		imageView = (ImageView) findViewById(R.id.imageView);
		copiedString = (EditText) findViewById(R.id.copiedString);
		setCopy = (Button) findViewById(R.id.setCopy);
		setCopy.setOnClickListener(this);
		getCopy = (Button) findViewById(R.id.getCopy);
		getCopy.setOnClickListener(this);
		delete = (Button) findViewById(R.id.delete);
		delete.setOnClickListener(this);
		
		// Set the itemlistener for the spinner.
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				spinnerSelection = (String) parent.getItemAtPosition(pos);
				if (spinnerSelection!=null && spinnerSelection.length()>0 && scr!=null) {
					retrieveContents();
				}
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}
	
	/**
	 * This method handles a click handled by the OnClickListener, which is used by the buttons.
	 */
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.setCopy:
			dataString = getCopiedContents();
			showCopiedContents(dataString);
			new SendTextAsyncTask().execute(new String[] {"niks"});
			break;
		case R.id.getCopy:
			new DirectoryAsyncTask().execute(new String[] {"niks"});
			break;
		case R.id.delete:
			new DeleteAsyncTask().execute(new String[] {"niks"});
			break;
		}
	}

	/**
	 * This method hides the imageview and shows a string on the textarea.
	 * @param s
	 */
	private void showCopiedContents(String s) {
		imageView.setVisibility(View.GONE);
		copiedString.setText(s);
		copiedString.setVisibility(View.VISIBLE);
	}
	
	/**
	 * This method decides which kind of data is to retrieved, using the length of the file, and if there, the extension.
	 */
	private void retrieveContents() {
		String x = directoryMap.get(spinnerSelection);
		if (x!=null) {
			if (x.length()==18 && x.substring(15).equals("txt")) {
				new RetrieveTextAsyncTask().execute(new String[] {x});
			} else if (x.length()==18 && (x.substring(15).equals("jpg") || x.substring(15).equals("png"))) {
				new RetrieveImageAsyncTask().execute(new String[] {x});
			} else if (x.length()==14) {
				filesCannotBeRetrieved();
			}
		}	
	}
	
	/**
	 * This method hides the textarea and shows a toast, to notify that the number on the spinner references to a directory containing files, 
	 * which cannot be recovered on a android phone.
	 */
	private void filesCannotBeRetrieved() {
		copiedString.setVisibility(View.GONE);
		Toast toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);	
		toast.setText("Unfortunately, you can not retrieve files.");
		toast.show();
	}
	
	/**
	 * This method send the username together with the string from the clipboard to the server, where it will be put in a new text file. 
	 * @return the string response of the server in json.
	 */
	private String sendTextContents() {
		JSONObject j = new JSONObject();
		try {
			j.put("User", username);
			j.put("Data", dataString);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Connection con = new Connection(sendURL,j,getResources());
		String response = con.unprotectedConnection();
		return response;
	}
	
	/**
	 * This method uses the clipboardmanager to send text to the clipboard. For users with versions prior to 3.0 (11), the deprecated clipboardmanager is used.
	 * @param s, is the string that will be put on the clipboard.
	 */
	@SuppressWarnings("deprecation")
	private void setCopiedContents(String s) {
		if (Build.VERSION.SDK_INT >= 11) {
			ClipboardManager cbm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData.Item item = new ClipData.Item(s);
			ClipDescription plainTextDescription = new ClipDescription(null,new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });
			ClipData cd = new ClipData(plainTextDescription, item);
			cbm.setPrimaryClip(cd);
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			clipboard.setText(s);
		}
	}
	
	/**
	 * This method uses the clipboardmanager to retrieve text from the clipboard. For users with versions prior to 3.0 (11), the deprecated clipboardmanager is used.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private String getCopiedContents() {
		String text = "";
		if (Build.VERSION.SDK_INT >= 11) {
			ClipboardManager cbm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			if (cbm.hasPrimaryClip()) {
				if (cbm.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
					text = cbm.getPrimaryClip().getItemAt(0).getText().toString();
				}
			}
		} else {
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			if (clipboard.hasText()) {
				text = (String) clipboard.getText();
			}
		}
		return text;
	}

	/**
	 * This private class creates a new thread where a connection with the server is made.
	 * Text will be send over this connection.
	 * @author Vincent
	 *
	 */
	private class SendTextAsyncTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			return sendTextContents();
		}

		protected void onPostExecute(String result) {
			showCopiedContents(result);
		}		
	}

	/**
	 * This private class creates a new thread where a connection with the server is made.
	 * Text will be retrieved over this connection.
	 * @author Vincent
	 *
	 */
	private class RetrieveTextAsyncTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			return scr.getString(s[0]);
		}

		protected void onPostExecute(String result) {
			if (result!=null) {
				setCopiedContents(result);
				showCopiedContents(result);
			} else {
				System.out.println("Result is null");
			}
		}		
	}

	/**
	 * This private class creates a new thread where a connection with the server is made.
	 * An image will be retrieved over this connection.
	 * @author Vincent
	 *
	 */
	private class RetrieveImageAsyncTask extends AsyncTask<String, Void, Bitmap> {
		protected Bitmap doInBackground(String... s) {
			return scr.getImage(s[0]);
		}

		protected void onPostExecute(Bitmap result) {
			System.out.println(result);
			setImageView(result);
		}		
	}

	/**
	 * This private class creates a new thread where a connection with the server is made.
	 * A directory list will be retrieved in jsonformat.
	 * @author Vincent
	 *
	 */
	private class DirectoryAsyncTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			return retrieveDirectoryList();
		}

		protected void onPostExecute(String result) {
			directoryMap = jsonToHashMap(result);
			setSpinner(directoryMap.size());
		}		
	}
	
	/**
	 * This private class creates a new thread where a connection with the server is made.
	 * A file will be deleted.
	 * @author Vincent
	 *
	 */
	private class DeleteAsyncTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			return deleteContent();
		}

		protected void onPostExecute(String result) {
			handleDeleteContentResponse(result);
			new DirectoryAsyncTask().execute(new String[] {"niks"});
		}		
	}

	/**
	 * This method creates a new connection and asks for the list of files.
	 * @return a list of filenames.
	 */
	public String retrieveDirectoryList() {
		JSONObject j = new JSONObject();
		try {
			j.put("User", username);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Connection con = new Connection(directoryURL,j,getResources());
		String response = con.unprotectedConnection();
		return response;
	}

	public void handleDeleteContentResponse(String result) {
		boolean deleted = false;
		try {
			JSONObject json = new JSONObject(result);
			json = json.getJSONObject(ServerResponseKey);
			if (json.has("deleted")) {
				deleted = json.getBoolean("deleted");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Toast toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);		
		if (deleted) {
			toast.setText("The deletion was succesful!");
		} else {
			toast.setText("The deletion failed.");
		}
		toast.show();
	}

	public String deleteContent() {
		String file = directoryMap.get(spinner.getSelectedItem().toString());
		JSONObject j = new JSONObject();
		try {
			j.put("User", username);
			j.put("File", file);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Connection con = new Connection(deleteContentURL,j,getResources());
		String response = con.unprotectedConnection();
		return response;
	}

	public void setImageView(Bitmap bitmap) {
		copiedString.setVisibility(View.GONE);
		Toast toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);	
		toast.setText("Images can not be delivered to the clipboard.");
		imageView.setImageBitmap(bitmap);
		imageView.setVisibility(View.VISIBLE);
		toast.show();
	}

	public HashMap<String,String> jsonToHashMap(String s) {
		HashMap<String,String> hm = new HashMap<String, String>();
		try {
			JSONObject obj = new JSONObject(s);
			obj = (JSONObject) obj.get(ServerResponseKey);
			int counter = 1;
			while (obj.has(counter+"")) {
				hm.put(counter+"",(String) obj.get(counter+""));
				counter++;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hm;
	}

	public void setSpinner(int length) {
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 1; i <= length; i++) list.add(i+"");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(dataAdapter);
		spinner.setVisibility(View.VISIBLE);
		delete.setVisibility(View.VISIBLE);
	}
}