package nl.vincentketelaars.wiebetaaltwat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.ConnectionService.LocalBinder;
import nl.vincentketelaars.wiebetaaltwat.adapters.WBWListAdapter;
import nl.vincentketelaars.wiebetaaltwat.objects.MemberGroup;
import nl.vincentketelaars.wiebetaaltwat.objects.MyHtmlParser;
import nl.vincentketelaars.wiebetaaltwat.objects.Resources;
import nl.vincentketelaars.wiebetaaltwat.objects.WBWList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This activity is the main activity of the application. Here you will find the overview of lists that the WBW account has. From this activity you will be able to open new activities to
 * the actual list of expenses.
 * @author Vincent
 *
 */
public class WBWListActivity extends Activity implements android.view.View.OnClickListener{

	// Global instances
	private List<WBWList> WBWLists;
	private String MyListOfWBWListsHTML;
	private ListView listView;
	private Button refreshButton;
	private ProgressDialog progressDialog;
	protected Context mContext = this;

	// Invite
	protected String inviteName;
	protected String inviteEmail;
	private int invitePosition;

	// Service
	private boolean mBound;
	private ConnectionService mService;
	private String WBWListHtml = "/index.php";

	//Activity for result
	private final int myRequestCode = 1;

	/** 
	 * Called when the activity is first created. 
	 * The method retrieves the html page from the login activity and parses it. Then the ListView is created with the parsed content. Lastly the ConnectionService is bound. 
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT < 11) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		} 
		setContentView(R.layout.wbw_lists_view);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null)
			MyListOfWBWListsHTML = bundle.getString("MyLists");		
		if (MyListOfWBWListsHTML != null) {
			MyHtmlParser parser = new MyHtmlParser(MyListOfWBWListsHTML);
			WBWLists = parser.parseTheWBWLists();
		}
		bindToService();
		if (Build.VERSION.SDK_INT < 11) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.my_title_bar);
			refreshButton = (Button) findViewById(R.id.refresh_button);
			refreshButton.setOnClickListener(this);
		}

		progressDialog = new ProgressDialog(this);
	}

	/**
	 * This method sets the ListView. It also maintains the onItemClicklistener for the list.
	 */
	private void setListView() {
		if (WBWLists == null) {
			Log.i("WBWList", "WBWList is null");
			return;
		}			
		listView = (ListView) findViewById(R.id.list_of_wbw_lists_listview);
		WBWListAdapter adapter = new WBWListAdapter(getApplicationContext(), R.layout.wbw_list, WBWLists);
		listView.setAdapter(adapter); 
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
				if (!mService.isOnline()) {
					Resources.showToast(mContext, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
					if (WBWLists.get(position).getExpenses() != null)
						handleReturn(WBWLists.get(position), null);
				} else {
					showProgressDialog();
					new AsyncExpenses().execute(WBWLists.get(position));
				}
			}
		});
		registerForContextMenu(listView);
	}

	/**
	 * This method handles the return of the html page. If the retrieval was a success a new activity will be started, 
	 * showing the expense list in the html page. Otherwise a Toast will be shown.
	 * @param back is the html page returned by the ConnectionService.
	 */
	private void handleReturn(WBWList wbw, String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		boolean correctInput = parser.correctInputExpenses();
		if (correctInput || wbw.getExpenses() != null) {
			Intent intent = new Intent(this,ExpenseListActivity.class);
			intent.putExtra("WBWList", (Parcelable) wbw); 
			if (correctInput) {
				intent.putExtra("Old", false);
				intent.putExtra("MyList", back);
			} else {
				intent.putExtra("Old", true);
			}
			startActivityForResult(intent, myRequestCode);
		} else { 
			if (parser.loggedOut()) {
				showLoggedOutDialog();
			} else {
				Resources.showToast(this, getResources().getString(R.string.wbw_list_retrieve_failed), Gravity.CENTER, Toast.LENGTH_SHORT);		
			}
		}
	}

	/**
	 * This is a private ServiceConnection class. Only the mandatory methods are implemented. 
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
		}

		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			mBound = false;			
		}
	};

	/**
	 * This method binds this activity to the ConnectionService. It uses a new thread, to make sure the activity can run alongside.
	 */
	private void bindToService() {
		Thread t = new Thread(){
			public void run(){
				getApplicationContext().bindService(new Intent(getApplicationContext(), ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
			}
		};
		t.start();
	}

	/**
	 * This method is called each time the previously started activity has returned a result.
	 * It checks whether the expense list activity sends back errors. If so it calls handleError with it. It also checks if a WBWlist has been send back, if so it updates the local one. 
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			Bundle b = data.getExtras();
			if (resultCode==RESULT_OK && b!=null) {
				WBWList temp = b.getParcelable("WBWList");
				if (temp != null) {
					for (int i = 0; i < WBWLists.size(); i++) {
						if (temp.getLid().equals(WBWLists.get(i).getLid())) {
							WBWLists.set(i, temp);
						}
					}
				}
			}
		}
	}

	/**
	 * This method asks the server for the html page, and tries to parse it to wbwlists. If it fails the old list will remain.
	 */
	public void refresh(String html) {
		MyHtmlParser parser = new MyHtmlParser(html);
		ArrayList<WBWList> temp = parser.parseTheWBWLists();
		if (temp != null) {
			WBWLists = temp;
			Resources.showToast(this, getResources().getString(R.string.refreshed), Gravity.CENTER, Toast.LENGTH_SHORT);
			outputWBWList(Resources.privateFile, WBWLists);
		} else {				
			if (parser.loggedOut()) {
				showLoggedOutDialog();
			} else if(parser.hasErrors()) {				
				Resources.showToast(this, parser.getLastError(), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else if (!mService.isOnline()) {
				Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				Resources.showToast(this, getResources().getString(R.string.cannot_refresh), Gravity.CENTER, Toast.LENGTH_SHORT);
			}
		}
		setListView();
	}

	/**
	 * This method is called when the activity resumes.
	 */
	public void onResume() {
		super.onResume();
		if (WBWLists != null) {
			setListView();
			outputWBWList(Resources.privateFile, WBWLists);
		} else {
			wbwListIsNull();
			if (WBWLists != null)
				setListView();
		}
	}

	/**
	 * This method is called when the wbwList is null and handles it by sending a toast.
	 */
	private void wbwListIsNull() {
		boolean fileExists = false;
		for (String x : fileList()) {
			if (x.equals(Resources.privateFile)) {
				fileExists = true;
				break;
			}
		}
		if (fileExists) {
			WBWLists = inputWBWList(Resources.privateFile);
		} else {
			Resources.showToast(this, getResources().getString(R.string.wbw_list_null), Gravity.CENTER, Toast.LENGTH_SHORT);
		}
	}

	/**
	 * This method save an arraylist to a permanent file.
	 * @param file
	 * @param wbwlist
	 */
	private void outputWBWList(String file, List<WBWList> wbwlist) {
		try {
			FileOutputStream fileOut = openFileOutput(file, Context.MODE_PRIVATE);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(wbwlist);
			out.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method retrieves an arraylist from the permanent file.
	 * @param file
	 * @return
	 */
	private List<WBWList> inputWBWList(String file) {
		List<WBWList> wbwlist = null;
		try {
			FileInputStream fileIn = openFileInput(file);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			wbwlist = (List<WBWList>) in.readObject();
			in.close();
			fileIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return wbwlist;
	}

	/**
	 * If the user is not connected to the internet, only a toast is shown to notify the user. 
	 * Show dialog when the user has been logged out. If the user clicks on OK, a log in request is sent, otherwise nothing happens.
	 */
	private void showLoggedOutDialog() {
		if (!mService.isOnline()) {
			Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.logged_out_title));
		builder.setMessage(getResources().getString(R.string.logged_out));
		builder.setCancelable(true);
		builder.setNeutralButton("Cancel", null);
		builder.setPositiveButton("OK", new OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {				
				showProgressDialog();
				new AsyncLogin().execute(new String[]{"bla"});			
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Retrieve password and email from file and send request to log in.
	 */
	private void LogIn(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		ArrayList<WBWList> temp = parser.parseTheWBWLists();
		if (temp != null) {
			WBWLists = temp;
			Resources.showToast(this, getResources().getString(R.string.relogin_success), Gravity.CENTER, Toast.LENGTH_SHORT);
		} else {
			if(parser.hasErrors()) {				
				Resources.showToast(this, parser.getLastError(), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				Resources.showToast(this, getResources().getString(R.string.relogin_failed), Gravity.CENTER, Toast.LENGTH_SHORT);				
			}
		}
		setListView();
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncLogin extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			SharedPreferences settings = getSharedPreferences(Resources.inlogFile, MODE_PRIVATE);
			String email = settings.getString("Email", null);
			String password = settings.getString("Password", null);
			return mService.logOnToSite(email, password);
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			LogIn(back);
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncRefresh extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... s) {
			return mService.retrieveHtmlPage(WBWListHtml);
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			refresh(back);
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncExpenses extends AsyncTask<WBWList, Void, String> {
		WBWList wbw = null;

		protected String doInBackground(WBWList... w) {
			wbw = w[0];
			return mService.retrieveHtmlPage(wbw.getHTML());
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			handleReturn(wbw, back);
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncInvitation extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... s) {
			return mService.sendInvitation(s[0], s[1], s[2]);
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			handleSentInvitation(back);
		}		
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refresh_button:
			if (!mService.isOnline()) {
				Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				showProgressDialog();
				new AsyncRefresh().execute(new String[]{"bla"});
			}
			break;
		default:
			break;
		}		
	}

	/**
	 * Show progress dialog for when a server request is made.
	 */
	private void showProgressDialog(){
		progressDialog = new ProgressDialog(this);
		progressDialog.setMessage(getResources().getString(R.string.loading));
		progressDialog.setCancelable(false);
		progressDialog.show();
	}	

	/**
	 * This method method is called everytime the options menu is opened. It shows the menu defined in the XML file.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (Build.VERSION.SDK_INT >= 11) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.wbw_list_options_menu, menu);
		}
		return true;
	}

	/**
	 * This method defines what happens when one of the options on the option menu is clicked. In the event that Refresh is clicked. A request will be made for the html page via the 
	 * ConnectionService. This will be parsed and viewed with the appropriate methods.
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.refresh_wbw_list:
			if (!mService.isOnline()) {
				Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				showProgressDialog();
				new AsyncRefresh().execute(new String[]{"bla"});
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void sendInvitation() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.invite_participant));
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		View dialogView = inflater.inflate(R.layout.invite, null);
		builder.setView(dialogView);
		final EditText editName = (EditText) dialogView.findViewById(R.id.invite_editname);
		final EditText editEmail = (EditText) dialogView.findViewById(R.id.invite_editemail);
		builder.setPositiveButton(R.string.invite, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				inviteName = editName.getText().toString();
				inviteEmail = editEmail.getText().toString();
				WBWList wbwList = WBWLists.get(invitePosition);
				String lid = wbwList.getLid();
				showProgressDialog();        		
				new AsyncInvitation().execute(new String[]{inviteName, inviteEmail, lid});
			}
		});
		builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public void handleSentInvitation(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		MemberGroup participants = parser.getListParticipants();
		String notice = parser.statusNoticeMessage();
		String response = getResources().getString(R.string.invitation_fail);
		if (participants != null && participants.getMemberByEmail(inviteEmail) != null) {
			if (notice != null && notice.contains("Deze gebruiker is al deelnemer van deze lijst!")) {
				response = getResources().getString(R.string.invitation_already_participant);
			} else {
				if (WBWLists.get(invitePosition).getGroupMembers() != null)
					WBWLists.get(invitePosition).getGroupMembers().addMember(participants.getMember(inviteName));
				response = getResources().getString(R.string.invitation_success);
			}
		} 
		response = response.replace("PARTICIPANT", inviteName);
		Resources.showToast(this, response, Gravity.CENTER, Toast.LENGTH_SHORT);
	}

	/**
	 * Create context menu.
	 */
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {		
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.wbwlist_context_menu, menu);
	}

	/**
	 * This method is called when there is a click on the context menu items.
	 */
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.wbw_list_invite:
			invitePosition = info.position;
			sendInvitation();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}
}