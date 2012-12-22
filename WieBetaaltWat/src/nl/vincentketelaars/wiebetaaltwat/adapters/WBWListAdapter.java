package nl.vincentketelaars.wiebetaaltwat.adapters;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.objects.WBWList;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * This class is merely an adapter for a list of WBWLists.
 * @author Vincent
 *
 */
public class WBWListAdapter extends ArrayAdapter<WBWList>{
	private TextView list;
	private TextView myBalance;
	private TextView highestBalance;
	private TextView lowestBalance;
	private List<WBWList> WBWLists = new ArrayList<WBWList>();

	public WBWListAdapter(Context context, int textViewResourceId, List<WBWList> objects) {
		super(context, textViewResourceId, objects);
		this.WBWLists = objects;
	}

	public int getCount() {
		return this.WBWLists.size();
	}

	public WBWList getItem(int index) {
		return this.WBWLists.get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;	
		
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.wbw_list, parent, false);
		}

		// Get item
		WBWList wbwList = getItem(position);		
		list = (TextView) row.findViewById(R.id.wbw_list_name);
		myBalance = (TextView) row.findViewById(R.id.wbw_list_my_balance);
		highestBalance =  (TextView) row.findViewById(R.id.wbw_list_highest_balance);
		lowestBalance =  (TextView) row.findViewById(R.id.wbw_list_lowest_balance);
		
		// Make sure that the double is printed with two decimals
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);		
		
		// Set item
		list.setText(wbwList.getListName());		
		myBalance.setText("€ "+df.format(wbwList.getMe().getBalance())); 		
		highestBalance.setText("€ "+df.format(wbwList.getHighestMember().getBalance()));
		lowestBalance.setText("€ "+df.format(wbwList.getLowestMember().getBalance()));
		
		// Set balance color
		if (wbwList.getMe().getBalance() < 0)
			myBalance.setTextColor(Color.RED);
		if (wbwList.getLowestMember().getBalance() < 0)
			lowestBalance.setTextColor(Color.RED);

		return row;
	}
}
