package nl.vincentketelaars.wiebetaaltwat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.vincentketelaars.wiebetaaltwat.adapters.AddMemberListAdapter;
import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.Resources;
import nl.vincentketelaars.wiebetaaltwat.views.Calculator;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

public class TestActivity extends Activity implements OnClickListener{

	private ListView listView;
	private ArrayList<Member> members;
	private Spinner memberSpinner;
	private EditText amountInputView;
	private EditText descriptionView;
	private Button cancelButton;
	private Button submitButton;
	private Button dateInputView;
	private AddMemberListAdapter adapter;
	private Button allTo1Button;
	private Button allTo0Button;
	private Button bUse;
	private Calculator calc;
	private Expense modifyExpense;
	private String inputAmount;
	private Toast toast;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_expense_view);
		members = new ArrayList<Member>();
		members.add(new Member("Kees"));
		members.add(new Member("Jan"));
		members.add(new Member("asdf"));
		members.add(new Member("dfe"));
		members.add(new Member("asdrwoiejhiw"));
		members.add(new Member("asdrwoasdfsdfiejhiw"));
		initializeView();
		setListView();
		setMembersSpinner();
		modifyExpense = new Expense("Jan","asdfh",0.5,"11-12-2012",members,"234","234");
		setModifyExpenseValues();
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
		memberSpinner.setSelection(members.indexOf(modifyExpense.getSpender()), true);
		dateInputView.setText(modifyExpense.getDate());
		descriptionView.setText(modifyExpense.getDescription());
		adapter.setMembers(members);
	}

	private void initializeView() {
		memberSpinner = (Spinner) findViewById(R.id.spinner_members);
		dateInputView = (Button) findViewById(R.id.add_expense_date_input);
		amountInputView = (EditText) findViewById(R.id.add_expense_amount_input);
//		amountInputView.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(5,2)});
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
		descriptionView = (EditText) findViewById(R.id.add_description_input);
		cancelButton = (Button) findViewById(R.id.add_expense_cancel_button);
		submitButton = (Button)	findViewById(R.id.add_expense_submit_button);
		allTo1Button = (Button) findViewById(R.id.add_expense_add_all_button);
		allTo0Button = (Button)	findViewById(R.id.add_expense_clear_all_button);
		cancelButton.setOnClickListener(this);
		submitButton.setOnClickListener(this);
		allTo1Button.setOnClickListener(this);
		allTo0Button.setOnClickListener(this);

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date date = new Date();
		dateInputView.setText(dateFormat.format(date));
	}

	private void setListView() {		
		listView = (ListView) findViewById(R.id.add_members_view);
		adapter = new AddMemberListAdapter(this, R.layout.add_expense_member_list, members);
		listView.setAdapter(adapter); 
	}

	private void setMembersSpinner() {
		ArrayList<String> names = new ArrayList<String>();
		for (Member m : members) {
			names.add(m.getMember());
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, names);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
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
		case R.id.add_expense_add_all_button:
			adapter.setAllCount(1);
			break;
		case R.id.add_expense_clear_all_button:	
			adapter.setAllCount(0);
			break;
		case R.id.calc_use:
			onCalcUseClicked();
			break;
		}
	}
	
	private void showCalculator() {
		calc = new Calculator(this);
		calc.show();
		bUse = calc.getUseButton();
		bUse.setOnClickListener(this);			
	}
	
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
		}
	}

	private void onAddClicked() {
		if(validDate() && validAmount() && validCount() && validDescription()) {
			
		} else {
			respondToAddClick();
		}
	}

	/**
	 * Check the date input view for a valid date. 
	 * @return true if valid else return false.
	 */
	private boolean validDate() {
		String date = dateInputView.getText().toString();
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
		String description = descriptionView.getText().toString();
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
		    Log.i("AddExpenseActivity","NumberformatException because parsing the inputAmount failed. inputAmount = "+ inputAmount);
		}
		return false;
	}
	
	/**
	 * If there is invalid input, show a toast as a response.
	 */
	private void respondToAddClick() {
		Toast toast = Toast.makeText(getApplicationContext(), "",Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);	
		String response = "";
		if (!validDate()) {
			response = response.concat("Please use the correct input for the date (dd-mm-yyyy).\n");
		}
		if (!validAmount()) {		
			response = response.concat("Make sure you have filled in an amount.\n");
		}
		if (!validDescription()) {			
			response = response.concat("Please put something in the description field.\n");
		}
		if (!validCount()) {	
			response = response.concat("Make sure you have selected at least one person.\n");
		}
		response = response.concat("Please try again.");
		toast.setText(response);
		toast.show();
	}


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

//	/**
//	 * This is a private class that establishes a filter for an amount. You can limit the number by setting the number of digits before and after the dot.
//	 *
//	 */
//	private class DecimalDigitsInputFilter implements InputFilter {
//
//		Pattern mPattern;
//
//		public DecimalDigitsInputFilter(int digitsBeforeZero,int digitsAfterZero) {
//			mPattern=Pattern.compile("[0-9]{0," + (digitsBeforeZero-1) + "}+((\\.[0-9]{0," + (digitsAfterZero-1) + "})?)");
//		}
//
//		public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
//
//			Matcher matcher=mPattern.matcher(dest);       
//			if(!matcher.matches())
//				return "";
//			return null;
//		}
//	}


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
