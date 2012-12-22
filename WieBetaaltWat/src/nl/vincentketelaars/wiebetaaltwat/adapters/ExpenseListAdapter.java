package nl.vincentketelaars.wiebetaaltwat.adapters;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.objects.Expense;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * This class is merely a adapter to build a list of Expenses.
 * @author Vincent
 *
 */
public class ExpenseListAdapter extends ArrayAdapter<Expense> {
	private TextView spender;
	private TextView description;
	private TextView amount;
	private TextView date;
	private TextView participants;
	private List<Expense> expenseList = new ArrayList<Expense>();
	private String filter;
	private List<Expense> filteredList;
	private FilterStyle filterStyle;
	private static Context mContext;
	
	// FilterStyle for the filter in this list.
	public enum FilterStyle {
		NONE(mContext.getResources().getString(R.string.none)), 
		SPENDER(mContext.getResources().getString(R.string.spender)), 
		PARTICIPANTS(mContext.getResources().getString(R.string.participant)), 
		BOTH(mContext.getResources().getString(R.string.both));
		
		private final String name;
		
		FilterStyle(String name) {
			this.name = name;		
		}
		
		public String getName() {
			return name;
		}
	};

	public ExpenseListAdapter(Context context, int textViewResourceId, List<Expense> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;		
		this.expenseList = objects;
		this.filteredList = objects;
		filterStyle = FilterStyle.NONE;
	}

	public int getCount() {
		return this.filteredList.size();
	}

	public Expense getItem(int index) {
		return this.filteredList.get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;	

		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.expense_list, parent, false);
		}

		// Get item
		Expense expense = getItem(position);	

		spender = (TextView) row.findViewById(R.id.expense_list_spender);
		description = (TextView) row.findViewById(R.id.expense_list_description);
		amount =  (TextView) row.findViewById(R.id.expense_list_amount);
		date =  (TextView) row.findViewById(R.id.expense_list_date);
		participants =  (TextView) row.findViewById(R.id.expense_list_participants);

		// Make sure that the double is printed with two decimals
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);		

		// Set item
		spender.setText(expense.getSpender());		
		description.setText(expense.getDescription());
		amount.setText("€ "+df.format(expense.getAmount()));
		date.setText(expense.getDate());
		participants.setText(expense.memberNames());

		return row;
	}

	/**
	 * Filter the contents of the list. A filteredList is created, based on the filter criteria: name and enum value.
	 */
	public void filter() {
		if (filter == null || filterStyle == null) {
			Collections.copy(filteredList, expenseList);
			return;
		}

		ArrayList<Expense> temp = new ArrayList<Expense>();
		for (Expense e : expenseList) {
			switch (filterStyle) {
			case NONE:
				temp.add(e);
				break;
			case SPENDER:
				if (e.getSpender().equals(filter)) {
					temp.add(e);
				}
				break;
			case PARTICIPANTS:
				if (e.participantsContain(filter)) {
					temp.add(e);
				}
				break;
			case BOTH:
				if (e.getSpender().equals(filter) || e.participantsContain(filter)) {
					temp.add(e);
				}
				break;
			default:
				temp.add(e);
				break;
			}
		}
		filteredList = temp;
		notifyDataSetChanged();
	}

	/**
	 * Set FilterStyle to one of the values declared in the enum, using the position.
	 * @param pos
	 */
	public void setFilterStyle(FilterStyle style) {
		filterStyle = style;
		filter();
	}

	/**
	 * Set the name to be filtered.
	 * @param member
	 */
	public void setFilterName(String member) {
		this.filter = member;	
		this.filter();		
	}
	
	public String getFilterName() {
		return filter;
	}
	
	public FilterStyle getFilterStyle() {
		return filterStyle;
	}
}
