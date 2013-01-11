package nl.vincentketelaars.wiebetaaltwat.adapters;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import nl.vincentketelaars.wiebetaaltwat.objects.MemberGroup;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * This class is merely an adapter for a list of ListMembers.
 * @author Vincent
 *
 */
public class AddMemberListAdapter extends ArrayAdapter<Member> implements OnItemSelectedListener {
	private TextView name;
	private List<Member> members = new ArrayList<Member>();
	private double amount;
	private Context context;
	private String spender;

	public AddMemberListAdapter(Context context, int textViewResourceId, List<Member> objects) {
		super(context, textViewResourceId, objects);
		this.setMembers(objects);
		this.context = context;
	}

	public int getCount() {
		return this.getMembers().size();
	}

	public Member getItem(int index) {
		return this.getMembers().get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;	
		Spinner spinner;
		
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.add_expense_member_list, parent, false);	
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,R.array.spinner_numbers, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner = (Spinner) row.findViewById(R.id.add_expense_list_spinner);
			spinner.setAdapter(adapter);
			spinner.setOnItemSelectedListener(this);
		}

		// Get the views.
		Member member = getItem(position);		
		name = (TextView) row.findViewById(R.id.add_expense_list_name);
		spinner = (Spinner) row.findViewById(R.id.add_expense_list_spinner);

		// Make sure that the double is printed with two decimals
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);	
		
		// Set the balance
		if (member.getCount() > 0)
			member.setBalance(-amount * member.getCount() / getTotalCount());
		else
			member.setBalance(0.0);		
		
		// Set spender balance
		if (member.getMember().equals(spender) && getTotalCount() > 0) {
			member.setBalance(amount * (getTotalCount() - member.getCount()) / getTotalCount());
		}
		
		// Set the textview text
		name.setText(member.getMember()+" (€ "+df.format(member.getBalance())+")");
		
		// Set the spinner selection
		spinner.setSelection(member.getCount());

		return row;
	}
	
	/**
	 * Set the total amount and refresh the list.
	 */
	public void setAmount(double value) {
		amount = value;
		this.notifyDataSetChanged();
	}
	
	/**
	 * Get the total amount.
	 * @return amount
	 */
	public double getAmount() {
		return amount;
	}

	/**
	 * When a selection is made, adjust this for the corresponding member. If this selection has changed for this member, refresh the list.
	 */
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,long arg3) {
		boolean changed = false;
		String name = (String) ((TextView) ((TableRow) arg0.getParent()).getChildAt(0)).getText();
		for (Member m : getMembers()) {
			if (m.getMember().equals(name.split("\\s\\(")[0])) {
				if (m.getCount()!=arg2)
					changed=true;
				m.setCount(arg2);
				break;
			}
		}
		// Only refresh if there is a different count value in the member
		if (changed)
			this.notifyDataSetChanged();
	}

	/**
	 * Obligatory method for the OnItemSelectedListener. It responds to empty selections.
	 */
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Set the count of all members in the list.
	 * @param count
	 */
	public void setAllCount(int count) {
		for (Member m : getMembers()) {
			m.setCount(count);
		}
		this.notifyDataSetChanged();
	}
	
	/**
	 * Set the count of all members in the list.
	 * @param memberGroup
	 */
	public void setAllCount(MemberGroup memberGroup) {
		for (Member m : getMembers()) {
			for (Member n : memberGroup.getGroupMembers()) {
				if (m.getId() == n.getId()) {
					m.setCount(n.getCount());
					break;
				}
			}
		}
		this.notifyDataSetChanged();
	}
	
	/**
	 * Retrieve the sum over the count of all members.
	 * @return
	 */
	private int getTotalCount() {
		int total = 0;
		for (int i = 0; i < getCount(); i++) {
			total += getMembers().get(i).getCount();
		}
		return total;
	}

	public List<Member> getMembers() {
		return members;
	}

	public void setMembers(List<Member> members) {
		this.members = members;
		this.notifyDataSetChanged();
	}
	
	public void setSpender(String spender) {
		this.spender = spender;
		this.notifyDataSetChanged();
	}
}
