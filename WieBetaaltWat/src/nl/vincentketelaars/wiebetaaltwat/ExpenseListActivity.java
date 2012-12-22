package nl.vincentketelaars.wiebetaaltwat;

import java.util.ArrayList;

import nl.vincentketelaars.wiebetaaltwat.ConnectionService.LocalBinder;
import nl.vincentketelaars.wiebetaaltwat.adapters.ExpenseListAdapter;
import nl.vincentketelaars.wiebetaaltwat.adapters.ExpenseListAdapter.FilterStyle;
import nl.vincentketelaars.wiebetaaltwat.adapters.MemberAdapter;
import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MyHtmlParser;
import nl.vincentketelaars.wiebetaaltwat.objects.Resources;
import nl.vincentketelaars.wiebetaaltwat.objects.WBWList;
import nl.vincentketelaars.wiebetaaltwat.views.HorizontalListView;
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
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * This activity shows a list of expenses. 
 * @author Vincent
 *
 */
public class ExpenseListActivity extends Activity implements android.view.View.OnClickListener{

	// Global instances
	private String WBWListHTML;
	private WBWList wbwList;
	private ListView expenseListView;
	private AdapterView<ListAdapter> memberListView;
	private Button refreshButton;
	private final int addExpenseRequestCode = 2;
	private int numPage = 1;
	private ProgressDialog progressDialog;
	private ExpenseListAdapter expenseListViewAdapter;
	private Toast toast;

	// Service
	private boolean mBound;
	private ConnectionService mService;

	/** Called when the activity is first created. 
	 * First the html page is retrieved from the previous activity. This is parsed and processed in the ListView.
	 * In addition will the expenseList be send back to the previous activity.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		if (Build.VERSION.SDK_INT < 11) {
			requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		}
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			setContentView(R.layout.expense_list_view_landscape);
		else
			setContentView(R.layout.expense_list_view);
		initializeWBWList();
		bindToService();
		
		if (Build.VERSION.SDK_INT < 11) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.my_title_bar);
			refreshButton = (Button) findViewById(R.id.refresh_button);
			refreshButton.setOnClickListener(this);
		}
		progressDialog = new ProgressDialog(this);	
	}

	/**
	 * Retrieve the WBWList from the previous activity. If it is null, finish this activity. Otherwise, use the provided html page, to parse additional data.
	 */
	private void initializeWBWList() {
		wbwList = getIntent().getExtras().getParcelable("WBWList");
		if (wbwList == null)
			failedToProvideWBWList();
		if (!getIntent().getExtras().getBoolean("Old"))
			WBWListHTML = getIntent().getExtras().getString("MyList");	
		if (WBWListHTML != null) {
			MyHtmlParser parser = new MyHtmlParser(WBWListHTML);
			ArrayList<Expense> temp = parser.parseTheListOfExpenses();
			if (temp != null)
				wbwList.setExpenses(temp);
			ArrayList<Member> members = parser.parseGroupMembers();
			if (members != null)
				wbwList.setGroupMembers(members);
			ArrayList<Integer> resultsPerPage = parser.getResultsPerPage();
			if (resultsPerPage != null) {
				wbwList.setNumResults(resultsPerPage.remove(0));
				// The duplicate element is removed
				wbwList.setResultsPerPage(resultsPerPage);
			}
			wbwList.setPages(parser.getNumPages());			
		}
	}

	/**
	 * This intent is send back to update the WBWList.
	 */
	private void setResultIntent() {		
		Intent resultIntent = new Intent();
		resultIntent.putExtra("WBWList", (Parcelable) wbwList);
		setResult(Activity.RESULT_OK, resultIntent);
	}

	/**
	 * If the wbwList retrieved from the previous activity is null, this activity will finish and send an error back.
	 */
	private void failedToProvideWBWList() {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("Error", "WBWList is null");
		setResult(Activity.RESULT_OK, resultIntent);
		Log.i("ExpenseList", "WBWList is null upon create");
		finish();	
	}

	/**
	 * This method creates the list of expenses. It also contains the onItemClickListener for the ListView.
	 */
	private void setExpenseListView() {
		if (wbwList == null || wbwList.getExpenses() == null) {
			Log.i("ExpenseList", "ExpenseList is null");
			return;
		}
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			expenseListView = (ListView) findViewById(R.id.expense_list_view_landscape);
		} else {
			expenseListView = (ListView) findViewById(R.id.expense_list_view);
		}
		expenseListViewAdapter = new ExpenseListAdapter(getApplicationContext(), R.layout.expense_list, wbwList.getExpenses());
		expenseListView.setAdapter(expenseListViewAdapter); 
		registerForContextMenu(expenseListView);
	}

	/**
	 * This method creates the horizontal list of members. It also contains the onItemClickListener for the ListView.
	 */
	private void setMemberListView() {
		if (wbwList == null || wbwList.getGroupMembers() == null) {
			Log.i("ExpenseList", "Groupmembers is null");
			return;
		}
		// Differentiate between Landscape and Portrait
		MemberAdapter myMemberAdapter = new MemberAdapter(getApplicationContext(), R.layout.member_button_view , wbwList.getGroupMembers());
		OnItemClickListener mOnItemClickListener = new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
				expenseListViewAdapter.setFilterName(wbwList.getGroupMembers().get(arg2).getMember());
				showFilteredMessage();
			}
		};
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			memberListView = (ListView) findViewById(R.id.member_list_view_landscape);
			memberListView.setAdapter(myMemberAdapter); 	
			// These need to be set, in order for the onItemClickListener to work! This is only necessary for the vertical ListView. The horizontal ListView has this implemented.
			myMemberAdapter.setParentListView(memberListView);
			myMemberAdapter.setOnItemClickListener(mOnItemClickListener);
		} else {
			memberListView = (HorizontalListView) findViewById(R.id.member_list_view);  
			memberListView.setAdapter(myMemberAdapter); 
		}		
		memberListView.setOnItemClickListener(mOnItemClickListener);		
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
	 * This method method is called everytime the options menu is opened. It shows the menu defined in the XML file.
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.expense_list_options_menu, menu);
		return true;
	}

	/**
	 * This method defines what happens when one of the options on the option menu is clicked. In the event that Refresh is clicked. A request will be made for the html page via the 
	 * ConnectionService. This will be parsed and viewed with the appropriate methods.
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.add_expense_list:
			if (!membersHaveId(wbwList.getGroupMembers())) {
				if (!mService.isOnline()) {
					showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
				} else {
					showProgressDialog();
					new AsyncAddExpense().execute(new Integer[]{0, 0});
				}
			} else {
				addExpense(null);
			}
			return true;
		case R.id.refresh_expense_list:
			if (!mService.isOnline()) {
				Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				showProgressDialog();
				new AsyncRefresh().execute(new String[]{"bla"});
			}
			return true;
		case R.id.page_expense_list:
			onPageClicked();
			return true;
		case R.id.results_expense_list:
			onResultsClicked();
			return true;
		case R.id.filter_expense_list:
			onFilterClicked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * This method is called when the filter menu option is clicked. It shows a dialog from which you can choose what to filter on: none, spender, participants, both.
	 * It calls the adapter to change the filter style. 
	 */
	private void onFilterClicked() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.filter));
		final ArrayList<String> results = new ArrayList<String>();
		for (FilterStyle f : FilterStyle.values()) 
			results.add(f.getName());
		final CharSequence[] items = results.toArray(new CharSequence[results.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				expenseListViewAdapter.setFilterStyle(FilterStyle.values()[item]);				
				showFilteredMessage();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();		
	}

	/**
	 * This method shows a toast that describes the kind of filter applied to the list.
	 */
	private void showFilteredMessage() {
		FilterStyle style = expenseListViewAdapter.getFilterStyle();
		if (expenseListViewAdapter.getFilterName() == null)
			expenseListViewAdapter.setFilterName(wbwList.getMe().getMember());
		String filteredOn = getResources().getString(R.string.filtered_on);
		filteredOn = filteredOn.replace("NAME", expenseListViewAdapter.getFilterName());
		switch (style) {
		case NONE:
			filteredOn = getResources().getString(R.string.filtered_none);
			break;
		case SPENDER:
			filteredOn = filteredOn.substring(0, filteredOn.indexOf("FILTER1")+7) + filteredOn.substring(filteredOn.indexOf("FILTER2")+7);
			filteredOn = filteredOn.replace("FILTER1", style.getName());						
			break;
		case PARTICIPANTS:
			filteredOn = filteredOn.substring(0, filteredOn.indexOf("FILTER1")+7) + filteredOn.substring(filteredOn.indexOf("FILTER2")+7);
			filteredOn = filteredOn.replace("FILTER1", style.getName());		
			break;
		case BOTH:
			filteredOn = filteredOn.replace("FILTER1", FilterStyle.SPENDER.getName());		
			filteredOn = filteredOn.replace("FILTER2", FilterStyle.PARTICIPANTS.getName());
			break;
		default:
			filteredOn = getResources().getString(R.string.filtered_none);
			break;
		}							
		showToast(filteredOn, Gravity.BOTTOM, Toast.LENGTH_SHORT);
	}

	private void onResultsClicked() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.results));
		final ArrayList<String> results = new ArrayList<String>();
		for (int x : wbwList.getResultsPerPage()) 
			results.add(Integer.toString(x));
		CharSequence[] items = results.toArray(new CharSequence[results.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				wbwList.setNumResults(wbwList.getResultsPerPage().get(item));
				numPage = 1;
				if (!mService.isOnline()) {
					showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
				} else {
					showProgressDialog();
					new AsyncChangeExpenses().execute(new String[]{"bla"});
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void onPageClicked() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.show_page));
		final ArrayList<String> pages = new ArrayList<String>(wbwList.getPages());
		for (int i = 1; i <= wbwList.getPages(); i++) 
			pages.add(Integer.toString(i));
		CharSequence[] items = pages.toArray(new CharSequence[pages.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				numPage = Integer.parseInt(pages.get(item));
				if (!mService.isOnline()) {
					showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
				} else {
					showProgressDialog();
					new AsyncChangeExpenses().execute(new String[]{"bla"});
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void changeExpenseList(String back) { 
		MyHtmlParser parser = new MyHtmlParser(back);
		ArrayList<Expense> eTemp = parser.parseTheListOfExpenses();
		if (eTemp != null) {
			wbwList.setExpenses(eTemp);
			wbwList.setPages(parser.getNumPages());	
			showToast(getResources().getString(R.string.expense_list_retrieved), Gravity.CENTER, Toast.LENGTH_SHORT);
		} else {
			if (parser.loggedOut()) {
				showLoggedOutDialog();
			} else {
				showToast(getResources().getString(R.string.wbw_list_retrieve_failed), Gravity.CENTER, Toast.LENGTH_SHORT);	
			}
		}
		setExpenseListView();
	}

	/**
	 * This method calls the AddExpense Activity and sends the list of groupmembers along.
	 */
	private void addExpense(String back) {
		if (back != null) {
			MyHtmlParser parser = new MyHtmlParser(back);
			ArrayList<Member> tempMembers = parser.parseAddExpense(wbwList.getGroupMembers());
			if (tempMembers != null)
				wbwList.setGroupMembers(tempMembers);
		}
		Intent intent = new Intent(this,AddExpenseActivity.class);
		intent.putExtra("lid", wbwList.getLid()); 
		intent.putExtra("Members", wbwList.getGroupMembers()); 
		intent.putExtra("ExpenseList", wbwList.getExpenses());
		intent.putExtra("Modify", false);
		startActivityForResult(intent, addExpenseRequestCode);		
	}

	/**
	 * Retrieve the html page and parse it to a new expense list. If it is null, keep the old version. Then set the list view.
	 */
	private void refresh(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		ArrayList<Expense> eTemp = parser.parseTheListOfExpenses();
		ArrayList<Member> mTemp = parser.parseGroupMembers();
		if (eTemp != null) {
			wbwList.setExpenses(eTemp);
			if (mTemp != null) {
				wbwList.setGroupMembers(mTemp);
				showToast(getResources().getString(R.string.refreshed), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				showToast(getResources().getString(R.string.member_list_retrieve_failed), Gravity.CENTER, Toast.LENGTH_SHORT);
			}
			resetMeHighLow();
			setResultIntent();
		} else {
			if (parser.loggedOut()) {
				showLoggedOutDialog();
			} else if (!mService.isOnline()) {
				showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				showToast(getResources().getString(R.string.cannot_refresh), Gravity.CENTER, Toast.LENGTH_SHORT);
			}
		}
		setExpenseListView();
		setMemberListView();
	}

	/**
	 * This method sets the balances of me, high and low.
	 */
	private void resetMeHighLow() {
		wbwList.setMe(wbwList.getGroupMembers().get(0));
		Member high = wbwList.getHighestMember();
		Member low = wbwList.getLowestMember();
		for (Member m : wbwList.getGroupMembers()) {
			if (high.getBalance() < m.getBalance())
				high = m;
			else if (low.getBalance() > m.getBalance())
				low = m;
		}
		wbwList.setHighestMember(high);
		wbwList.setLowestMember(low);
	}

	/**
	 * This method is called when the activity resumes.
	 */
	public void onResume() {
		super.onResume();
		if (wbwList != null) {
			setResultIntent();
			setMemberListView();
			setExpenseListView();
			if (wbwList.getExpenses() == null) {	
				expenseListIsNull();	
			}
		} else {
			failedToProvideWBWList();
		}
	}

	/**
	 * This method is called when the wbwList is null and handles it by sending a toast.
	 */
	private void expenseListIsNull() {	
		showToast(getResources().getString(R.string.expense_list_null), Gravity.CENTER, Toast.LENGTH_SHORT);
	}

	/**
	 * This method is called each time the previously started activity has returned a result. 
	 * It checks if adding a new expense has been successful. If true it calls refresh.
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null) {
			Bundle b = data.getExtras();
			if (resultCode==RESULT_OK && b!=null) {
				Boolean success = b.getBoolean("Success");				
				if (success != null && success) {
					ArrayList<Expense> expenses = b.getParcelableArrayList("ExpenseList");
					ArrayList<Member> members = b.getParcelableArrayList("MemberList");
					if (expenses != null && members != null) {
						wbwList.setExpenses(expenses);
						wbwList.setGroupMembers(members);
					}
					resetMeHighLow();
				}
			}
		}
	}

	/**
	 * Create context menu.
	 */
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {		
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.expense_list_context_menu, menu);
	}

	/**
	 * This method is called when there is a click on the context menu items.
	 */
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.expense_list_modify:
			if (!membersHaveId(wbwList.getGroupMembers())) {
				if (!mService.isOnline()) {
					showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
				} else {
					showProgressDialog();
					new AsyncAddExpense().execute(new Integer[]{1, info.position});
				}
			} else {
				modifyExpense(info.position, null);
			}
			return true;
		case R.id.expense_list_delete:
			showProgressDialog();
			new AsyncDeleteExpense().execute(new Integer[]{info.position});
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	/**
	 * This method opens a new AddExpenseActivity, sending along the details of the expense that needs to be modified.
	 */
	private void modifyExpense(int pos, String back) {
		if (back != null) {
			MyHtmlParser parser = new MyHtmlParser(back);
			ArrayList<Member> tempList = parser.parseAddExpense(wbwList.getGroupMembers());
			if (tempList != null)
				wbwList.setGroupMembers(tempList);
		}
		Intent intent = new Intent(this,AddExpenseActivity.class);
		intent.putExtra("lid", wbwList.getLid()); 
		intent.putExtra("Members", wbwList.getGroupMembers());
		intent.putExtra("ExpenseList", wbwList.getExpenses());
		intent.putExtra("Modify", true);
		intent.putExtra("ExpensePosition", pos);
		startActivityForResult(intent, addExpenseRequestCode);			
	}

	/**
	 * This method sends a request to delete an expense from the list. If it succeeds, it is also removed from the expense list, and the verticallist is reviewed.
	 * Else a toast shows the error given by the server.
	 * @param position of the expense.
	 */
	private void deleteExpense(int pos, String back) {		
		MyHtmlParser parser = new MyHtmlParser(back);		
		ArrayList<Expense> eTemp = parser.parseTheListOfExpenses();
		ArrayList<Member> mTemp = parser.parseGroupMembers();
		if (eTemp != null) {
			boolean tidGone = true;
			for (Expense e : eTemp)
				if (e.getTid().equals(wbwList.getExpenses().get(pos).getTid()))
					tidGone = false;
			if (tidGone) {
				wbwList.setExpenses(eTemp);
				wbwList.setGroupMembers(mTemp);
				showToast(getResources().getString(R.string.deletion_success), Gravity.CENTER, Toast.LENGTH_SHORT);
				setExpenseListView();
				setResultIntent();
			} else {				
				String statusNoticeMessage = parser.statusNoticeMessage();
				if (statusNoticeMessage == null || statusNoticeMessage.length() == 0)
					showToast(getResources().getString(R.string.delete_expense_unknown), Gravity.CENTER, Toast.LENGTH_LONG);
				else
					showToast(parser.statusNoticeMessage(), Gravity.CENTER, Toast.LENGTH_SHORT);
			}
		}
	}

	/**
	 * If the user is not connected to the internet, only a toast is shown to notify the user. 
	 * Show dialog when the user has been logged out. If the user clicks on OK, a log in request is sent, otherwise nothing happens.
	 */
	private void showLoggedOutDialog() {
		if (!mService.isOnline()) {
			showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
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
		if (parser.loggedOut())
			showToast(getResources().getString(R.string.relogin_failed), Gravity.CENTER, Toast.LENGTH_SHORT);
		else 
			showToast(getResources().getString(R.string.relogin_success), Gravity.CENTER, Toast.LENGTH_SHORT);
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
			return mService.retrieveHtmlPage(wbwList.getHTML());
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
	private class AsyncChangeExpenses extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... s) {
			return mService.changeExpenseList(wbwList.getLid(), Integer.toString(numPage), Integer.toString(wbwList.getNumResults()));	
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			changeExpenseList(back);
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncAddExpense extends AsyncTask<Integer, Void, String> {
		int add = 0;
		int pos = 0;

		protected String doInBackground(Integer... i) {
			add = i[0];
			pos = i[1];
			return mService.retrieveHtmlPage(wbwList.getHTML().replace("balance","transaction&type=add"));
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			if (add == 0)
				addExpense(back);
			else if (add == 1)
				modifyExpense(pos, back);
		}		
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncDeleteExpense extends AsyncTask<Integer, Void, String> {
		int pos = 0;

		protected String doInBackground(Integer... i) {
			pos = i[0];
			return mService.retrieveHtmlPage(wbwList.getExpenses().get(pos).getDelete());
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			deleteExpense(pos, back);
		}		
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.refresh_button:
			if (!mService.isOnline()) {
				showToast(getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
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
	 * This method checks whether each Member in the list has an id.
	 * @return true if all members have an id, otherwise false
	 */
	private boolean membersHaveId(ArrayList<Member> memberList) {
		for (Member m : memberList) {
			if (m.getId() == null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Create and show a Toast.
	 * @param context
	 * @param text
	 * @param position
	 * @param duration
	 */
	public void showToast(String text, int position, int duration) {
		if (toast == null) 
			toast = Toast.makeText(this, text, duration);
		if (toast.getView().isShown())
			toast.cancel();
		toast.setText(text);
		toast.setGravity(position, 0, 0);
		toast.show();
	}
}