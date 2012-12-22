package nl.vincentketelaars.wiebetaaltwat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.vincentketelaars.wiebetaaltwat.ConnectionService.LocalBinder;
import nl.vincentketelaars.wiebetaaltwat.adapters.AddMemberListAdapter;
import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MyHtmlParser;
import nl.vincentketelaars.wiebetaaltwat.objects.Resources;
import nl.vincentketelaars.wiebetaaltwat.views.Calculator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class AddExpenseActivity extends Activity implements OnClickListener, OnDateSetListener {

	// Global
	private ArrayList<Member> members;
	private ArrayList<Expense> expenseList;
	private String lid;
	private String description;
	private String date;
	private String inputAmount;
	private boolean modifyExpenseBool;
	private int modifyExpensePosition;
	private Expense modifyExpense;
	private Expense justSend;

	// Views and adapters
	private ListView listView;
	private Spinner memberSpinner;
	private EditText amountInputView;
	private Button calculatorButton;
	private EditText descriptionView;
	private Button cancelButton;
	private Button submitButton;
	private Button modifyButton;
	private Button dateInputView;
	private Button allTo1Button;
	private Button allTo0Button;
	private AddMemberListAdapter adapter;
	private ProgressDialog progressDialog;
	protected Context mContext = this;

	// Integers to distinguish send operation.
	private int EXPENSE_ADD = 0;
	private int EXPENSE_MODIFY = 1;

	// Calculator
	private Calculator calc;
	private Button bUse;

	// Connection Service
	private ConnectionService mService;
	private boolean mBound;

	/**
	 * This method is called upon create of the activity
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense_view);
		members = getIntent().getExtras().getParcelableArrayList("Members");
		expenseList = getIntent().getExtras().getParcelableArrayList("ExpenseList");
		if (members == null) {
			Log.i("AddExpense", "Members is null");
			onCancelClicked();
		}	
		lid = getIntent().getExtras().getString("lid");
		modifyExpenseBool = getIntent().getExtras().getBoolean("Modify");
		modifyExpensePosition = getIntent().getExtras().getInt("ExpensePosition");
		if (expenseList == null) {
			Log.i("AddExpense", "expenseList is null");
			onCancelClicked();
		} else {
			if (modifyExpenseBool)
				modifyExpense = expenseList.get(modifyExpensePosition);
		}			
		bindToService();
		initializeView();
		setListView();
		setMembersSpinner();
		if (modifyExpense != null) {
			setModifyExpenseValues();			
		}

		progressDialog = new ProgressDialog(this);
	}

	/**
	 * If there is an Expense that needs to be modified. This method sets the values.
	 */
	private void setModifyExpenseValues() {
		for (Member p : modifyExpense.getParticipants()) {
			for (Member m : members) {
				if (m.getMember().equals(p.getMember())) {
					m.setCount(p.getCount());
				}
			}
		}
		amountInputView.setText(Double.toString(modifyExpense.getAmount()));
		adapter.setAmount(modifyExpense.getAmount());
		int index = 0;
		for (int i = 0; i < members.size(); i++) {
			if (modifyExpense.getSpender().equals(members.get(i).getMember()))
				index = i;
		}
		memberSpinner.setSelection(index, true);
		dateInputView.setText(modifyExpense.getDate());
		descriptionView.setText(modifyExpense.getDescription());
		adapter.setMembers(members);
		submitButton.setVisibility(View.GONE);
		modifyButton.setVisibility(View.VISIBLE);
	}

	/**
	 * Set the initial view stuff.
	 */
	private void initializeView() {
		memberSpinner = (Spinner) findViewById(R.id.spinner_members);
		dateInputView = (Button) findViewById(R.id.add_expense_date_input);
		dateInputView.setOnClickListener(this);
		amountInputView = (EditText) findViewById(R.id.add_expense_amount_input);
		amountInputView.addTextChangedListener(new TextWatcher() {			
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String input = s.toString();
				int index = input.indexOf('.');
				if (index >= 0) {
					// Only two numbers are allowed after the dot.
					if (input.length() - index > 2 + 1) {
						String oldInput = input.substring(0,start)+input.substring(start+count);
						amountInputView.setText(oldInput);
						amountInputView.setSelection(oldInput.length());
					}
				}	
			}			
			
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
			}
			
			public void afterTextChanged(Editable s) {
				checkAmountInput();				
			}
		});
		OnFocusChangeListener myOnFocusChangeListener = new OnFocusChangeListener() {

			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
				}				
			}
		};
		amountInputView.setOnFocusChangeListener(myOnFocusChangeListener);
		descriptionView = (EditText) findViewById(R.id.add_description_input);
		descriptionView.setOnFocusChangeListener(myOnFocusChangeListener);
		cancelButton = (Button) findViewById(R.id.add_expense_cancel_button);
		submitButton = (Button)	findViewById(R.id.add_expense_submit_button);
		modifyButton = (Button)	findViewById(R.id.add_expense_modify_button);
		allTo1Button = (Button) findViewById(R.id.add_expense_add_all_button);
		allTo0Button = (Button)	findViewById(R.id.add_expense_clear_all_button);
		cancelButton.setOnClickListener(this);
		submitButton.setOnClickListener(this);
		modifyButton.setOnClickListener(this);
		allTo1Button.setOnClickListener(this);
		allTo0Button.setOnClickListener(this);		

		calculatorButton = (Button) findViewById(R.id.calculator_button);
		calculatorButton.setOnClickListener(this);

		setDateToday();
	}

	/**
	 * This method sets the date input view to today.
	 */
	private void setDateToday() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date date = new Date();
		dateInputView.setText(dateFormat.format(date));
	}

	/**
	 * Create a listView containing all the members of the WBWList.
	 */
	private void setListView() {		
		listView = (ListView) findViewById(R.id.add_members_view);
		adapter = new AddMemberListAdapter(this, R.layout.add_expense_member_list, members);
		listView.setAdapter(adapter); 
	}

	/**
	 * Create a spinner which has all the members of the WBWList to select, with the account owner set initially.
	 */
	private void setMembersSpinner() {
		ArrayList<String> names = new ArrayList<String>();
		for (Member m : members) {
			names.add(m.getMember());
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, names);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		memberSpinner.setAdapter(dataAdapter);
	}

	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.add_expense_cancel_button:
			onCancelClicked();
			break;
		case R.id.add_expense_submit_button:
			onAddClicked();
			break;
		case R.id.add_expense_modify_button:
			onModifyClicked();
			break;
		case R.id.add_expense_add_all_button:
			adapter.setAllCount(1);
			break;
		case R.id.add_expense_clear_all_button:
			adapter.setAllCount(0);
			break;
		case R.id.add_expense_date_input:
			showTicker();
			break;
		case R.id.calc_use:
			onCalcUseClicked();
			break;
		case R.id.calculator_button:
			showCalculator();
			break;
		}
	}

	/**
	 * This method creates a Calculator dialog, shows it, and sets the bUse button.
	 */
	private void showCalculator() {
		calc = new Calculator(this);
		calc.show();
		bUse = calc.getUseButton();
		bUse.setOnClickListener(this);			
	}

	/**
	 * This method calls the Calculator for the result, sets that result in the amountInputView and cancels then the dialog.
	 */
	private void onCalcUseClicked() {
		if (calc.isDouble()) {
			double input = calc.getResult();
			if (input < 0) {
				Resources.showToast(this, getResources().getString(R.string.negative_numbers_not_allowed), Gravity.CENTER, Toast.LENGTH_SHORT);
			} else {
				DecimalFormat df = new DecimalFormat();
				df.setMinimumFractionDigits(2);
				df.setMaximumFractionDigits(2);		
				amountInputView.setText((df.format(input)).replace(",","."));
				amountInputView.setSelection(df.format(input).length());
				adapter.setAmount(input);
				calc.cancel();
			}
		} else {
			Resources.showToast(this, getResources().getString(R.string.calc_incorrect_use), Gravity.CENTER, Toast.LENGTH_SHORT);
		}
	}

	/**
	 * This method creates a DatePickerDialog. It is initially set to the current date.
	 */
	private void showTicker() {
		String date = dateInputView.getText().toString();
		int mYear = Integer.parseInt(date.substring(6));
		int mMonth = Integer.parseInt(date.substring(3,5));
		int mDay = Integer.parseInt(date.substring(0,2));
		DatePickerDialog picker = new DatePickerDialog(this, this, mYear, mMonth-1, mDay);
		picker.show();
	}

	/**
	 * This method is called when the modify button is clicked. It checks if everything is ready to send. After it has send the modification, it checks if it was successful. 
	 */
	private void onModifyClicked() {
		if(validDate() && validAmount() && validCount() && validDescription()) {
			if (lid != null && members != null) {
				new AsyncAddExpense().execute(new Integer[]{EXPENSE_MODIFY});				
			} else {
				Log.i("AddExpense", "Either lid or members is null for modify");
			}
		} else {
			respondToIllegalSendClick();
		}		
	}

	private void onModifyReturned(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		ArrayList<Expense> eTemp = parser.parseTheListOfExpenses();
		ArrayList<Member> mTemp = parser.parseGroupMembers();
		if (eTemp != null && expenseList != null) {
			if (expenseEqualsDataJustSend(eTemp.get(0))) {
				expenseList = eTemp;
				modifyExpenseSuccesful(mTemp);
			} else {	
				String message = parser.statusNoticeMessage();
				if (message.contains("De invoer is gewijzigd.")) {
					Log.i("AddExpense", "Modify expense is not recognised");
					expenseList = eTemp;
					modifyExpenseSuccesful(mTemp);
				} else {
					expenseUnsuccesful(message, EXPENSE_MODIFY);
				}
			}
		} else {
			Resources.showToast(this, getResources().getString(R.string.expense_add_unknown), Gravity.CENTER, Toast.LENGTH_LONG);
		}
	}

	/**
	 * This method is called when the submit button is clicked. It checks if everything is ready to send. After it has send the new expense, it checks if it was successful. 
	 */
	private void onAddClicked() {
		if(validDate() && validAmount() && validCount() && validDescription()) {
			if (!expenseEqualsDataJustSend(justSend)) {
				if (lid != null && members != null) {
					if (!mService.isOnline()) {
						Resources.showToast(this, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
					} else {
						showProgressDialog();
						new AsyncAddExpense().execute(new Integer[]{EXPENSE_ADD});
					}
				} else {
					Log.i("AddExpense", "Either lid or members is null for add");
				}
			} else {
				respondSendAlready();
			}
		} else {
			respondToIllegalSendClick();
		}
	}

	/**
	 * This method is called if previously sent data is the same as the new request. It offers the user a dialog to reconsider. Yes to send the data, no to cancel the send.
	 */
	private void respondSendAlready() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.duplicate_data));
		builder.setMessage(getResources().getString(R.string.duplicate_data_question));
		builder.setCancelable(true);
		builder.setNeutralButton(getResources().getString(R.string.no), null);
		builder.setPositiveButton(getResources().getString(R.string.yes), new android.content.DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				if (!mService.isOnline()) {
					Resources.showToast(mContext, getResources().getString(R.string.not_connected), Gravity.CENTER, Toast.LENGTH_SHORT);
				} else {
					showProgressDialog();
					new AsyncAddExpense().execute(new Integer[]{EXPENSE_ADD});		
				}
			}
		});
		AlertDialog alert = builder.create();
		alert.show();		
	}

	/**
	 * This method checks if the data to be send is not equal to send previously, to prevent unintentional duplication.
	 * @return true if already sent.
	 */
	private boolean expenseEqualsDataJustSend(Expense base) {
		if (base == null)
			return false;
		if (base.getDate().equals(date) && 
				base.getDescription().equals(descriptionRevision(description)) && 
				base.getSpender().equals(members.get(memberSpinner.getSelectedItemPosition()).getMember()) && 
				Math.abs(base.getAmount()-Double.parseDouble(inputAmount))<0.01 &&
				participantsHaveEqualCount(members, base.getParticipants())) {			
			return true;
		}
		return false;
	}

	/**
	 * WieBetaaltWat trims the description of starting and ending spaces. This method should change the same things on the description as WieBetaaltWat does.
	 * @param description
	 * @return revised description
	 */
	private String descriptionRevision(String description2) {
		return description.trim();
	}

	/**
	 * This method compares the two ArrayLists of Members for equal count.
	 * @param first
	 * @param second
	 * @return if both MemberLists have for each member the same count, it returns true. False otherwise.
	 */
	public boolean participantsHaveEqualCount(ArrayList<Member> first, ArrayList<Member> second) {
		for (Member f : first) {
			for (Member s : second) {
				if (s.getMember().equals(f.getMember())) {
					if (s.getCount() != f.getCount()) {
						return false;
					}
					continue;
				}
			}
		}
		return true;
	}

	/**
	 * This method is called when AsyncTask has returned a result for a addExpense request.
	 * @param back
	 */
	private void onAddReturned(String back) {
		MyHtmlParser parser = new MyHtmlParser(back);
		ArrayList<Expense> eTemp = parser.parseTheListOfExpenses();
		ArrayList<Member> mTemp = parser.parseGroupMembers();
		if (eTemp != null && expenseList != null) {
			if (expenseEqualsDataJustSend(eTemp.get(0))) {
				expenseList = eTemp;
				addExpenseSuccesful(mTemp);
			} else {
				String message = parser.statusNoticeMessage();
				if (message.contains("De invoer is toegevoegd aan de lijst.")) {
					Log.i("AddExpense", "Add expense is not recognised");
					expenseList = eTemp;
					addExpenseSuccesful(mTemp);
				} else {	
					expenseUnsuccesful(parser.statusNoticeMessage(), EXPENSE_ADD);
				}
			}
		} else {
			Resources.showToast(this, getResources().getString(R.string.expense_add_unknown), Gravity.CENTER, Toast.LENGTH_LONG);
		}
	}

	/**
	 * This method is called if the response from the server, gives an unsuccessful message handling an expense.
	 */
	private void expenseUnsuccesful(String response, int choice) {
		if (response == null || response.length() == 0) {
			if (choice == EXPENSE_ADD)
				Resources.showToast(this, getResources().getString(R.string.expense_add_fail), Gravity.CENTER, Toast.LENGTH_LONG);
			else if (choice == EXPENSE_MODIFY)
				Resources.showToast(this, getResources().getString(R.string.expense_modify_fail), Gravity.CENTER, Toast.LENGTH_LONG);
		} else {
			Resources.showToast(this, response, Gravity.CENTER, Toast.LENGTH_SHORT);
		}
	}

	/**
	 * This method is called if the response from the server, gives an successful message to adding a new expense.
	 */
	private void addExpenseSuccesful(ArrayList<Member> mTemp) {
		setResultIntent(true, mTemp);
		justSend = new Expense(getSpenderId(), description, Double.parseDouble(inputAmount), date, null, null, null);
		resetToInitialView();
		Resources.showToast(this, getResources().getString(R.string.expense_add_success), Gravity.CENTER, Toast.LENGTH_SHORT);
	}

	/**
	 * This method is called if the response from the server, gives an successful message to modifying an expense.
	 */
	private void modifyExpenseSuccesful(ArrayList<Member> mTemp) {
		setResultIntent(true, mTemp);
		resetToInitialView();
		submitButton.setVisibility(View.VISIBLE);
		modifyButton.setVisibility(View.GONE);
		Resources.showToast(this, getResources().getString(R.string.expense_modify_success), Gravity.CENTER, Toast.LENGTH_SHORT);
	}

	/**
	 * Return the Spender Id. 
	 * @return Id.
	 */
	private String getSpenderId() {
		return members.get(memberSpinner.getSelectedItemPosition()).getId();
	}

	/**
	 * Check the date input view for a valid date. 
	 * @return true if valid else return false.
	 */
	private boolean validDate() {
		date = dateInputView.getText().toString();
		Pattern p = Pattern.compile("\\d{2}\\-\\d{2}\\-\\d{4}");
		Matcher m = p.matcher(date);
		if (m.find())	
			return true;
		return false;
	}	

	/**
	 * Check whether at least one ListMember has a count higher than zero.
	 * @return true if this is true.
	 */
	private boolean validCount() {
		for (Member m : members) {
			if (m.getCount() > 0)
				return true;
		}
		return false;
	}

	/**
	 * Check if the description input is not null or empty.
	 * @return true if it is a valid description.
	 */
	private boolean validDescription() {
		description = descriptionView.getText().toString();
		if (description == null || description.length() <= 0)
			return false;
		return true;
	}

	/**
	 * Checks whether the input is not null or zero.
	 * @return true
	 */
	private boolean validAmount() {
		inputAmount = amountInputView.getText().toString();
		if (inputAmount == null || inputAmount.length() <= 0)
			return false;
		try {
			double amount = Double.parseDouble(inputAmount);
			if (amount > 0)
				return true;
		} catch (NumberFormatException e) {
			Resources.showToast(this, getResources().getString(R.string.calc_incorrect_amount), Gravity.CENTER, Toast.LENGTH_SHORT);
			Log.i("AddExpenseActivity","NumberformatException because parsing the inputAmount failed. inputAmount = "+ inputAmount);
		}
		return false;
	}

	/**
	 * If there is invalid input, show a toast as a response.
	 */
	private void respondToIllegalSendClick() {
		String response = ""; 
		if (!validDate()) {
			response = response.concat(getResources().getString(R.string.calc_incorrect_date));
		}
		if (!validAmount()) {		
			response = response.concat(getResources().getString(R.string.calc_incorrect_amount));
		}
		if (!validDescription()) {			
			response = response.concat(getResources().getString(R.string.calc_incorrect_description));
		}
		if (!validCount()) {	
			response = response.concat(getResources().getString(R.string.calc_incorrect_count));
		}
		response = response.concat(getResources().getString(R.string.try_again));
		Resources.showToast(this, response, Gravity.CENTER, Toast.LENGTH_LONG);
	}

	/**
	 * Return to the previous activity.
	 */
	private void onCancelClicked() {
		finish();		
	}

	/**
	 * This method checks the input in the amountInputView. If it is a viable double, it will set the amount in the adapter.
	 */
	private void checkAmountInput() {
		String a = amountInputView.getText().toString();
		if (a.length() > 0) {
			if (a.charAt(0)=='.')
				a = "0"+a;
			Pattern doublePattern = Pattern.compile("\\d+(\\.\\d*)?");
			Matcher m = doublePattern.matcher(a);
			double d = 0;
			if (m.find()) {
				d = Double.parseDouble(m.group());					
			}
			adapter.setAmount(d);
		} else {
			adapter.setAmount(0.00);
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
	 * This method resets all views to their initial state.
	 */
	private void resetToInitialView() {
		memberSpinner.setSelection(0);		
		amountInputView.setText("");
		inputAmount = null;
		descriptionView.setText("");
		description = null;
		setDateToday();
		date = null;
		adapter.setAmount(0);
		adapter.setAllCount(0);
	}

	/**
	 * This intent is send back to update the ExpenseList.
	 * @param success
	 */
	private void setResultIntent(boolean success, ArrayList<Member> mTemp) {		
		Intent resultIntent = new Intent();
		resultIntent.putExtra("Success", success);
		resultIntent.putExtra("ExpenseList", expenseList);
		resultIntent.putExtra("MemberList", mTemp);
		setResult(Activity.RESULT_OK, resultIntent);
	}

	/**
	 * This method is called when the DatePicker has been used.
	 * @param view
	 * @param year
	 * @param monthOfYear
	 * @param dayOfMonth
	 */
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date date = new Date(year-1900, monthOfYear, dayOfMonth);
		dateInputView.setText(dateFormat.format(date));
	}

	/**
	 * This private class, makes sure that the network connections are made on a separate thread.
	 * @author Vincent
	 *
	 */
	private class AsyncAddExpense extends AsyncTask<Integer, Void, String> {
		int add = EXPENSE_ADD;

		protected String doInBackground(Integer... i) {
			add = i[0];
			if (add == EXPENSE_ADD)
				return mService.sendExpense(lid, getSpenderId(), description, inputAmount, date, members);
			else if (add == EXPENSE_MODIFY)
				return mService.sendModifiedExpense(modifyExpense.getTid(), lid, getSpenderId(), description, inputAmount, date, members);
			return null;
		}

		protected void onPostExecute(String back) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();
			if (add == EXPENSE_ADD)
				onAddReturned(back);
			else if (add == EXPENSE_MODIFY)
				onModifyReturned(back);
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
			inflater.inflate(R.menu.add_expense_options_menu, menu);
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
		case R.id.calculator_option:
			showCalculator();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
