package nl.vincentketelaars.wiebetaaltwat.adapters;

import java.text.DecimalFormat;
import java.util.List;

import nl.vincentketelaars.wiebetaaltwat.R;
import nl.vincentketelaars.wiebetaaltwat.objects.Member;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MemberAdapter extends ArrayAdapter<Member> implements OnClickListener, OnLongClickListener {

	private List<Member> members;
	private AdapterView<ListAdapter> mListView;
	private OnItemClickListener mOnItemClickListener;
	private OnItemLongClickListener mOnItemLongClickListener;

	public MemberAdapter(Context context, int textViewResourceId, List<Member> m) {
		super(context, textViewResourceId, m);
		members = m;
	}

	public int getCount() {
		return members.size();
	}

	public Member getItem(int position) {
		return members.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View x = convertView;
		if (x == null) {
			x = LayoutInflater.from(parent.getContext()).inflate(R.layout.member_button_view, null);
		}

		TextView nameTextView = (TextView) x.findViewById(R.id.button_member_name);
		TextView balanceTextView = (TextView) x.findViewById(R.id.button_member_balance);

		nameTextView.setText(members.get(position).getMember());

		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);	
		double value = members.get(position).getBalance();
		balanceTextView.setText("€ "+ df.format(value));

		x.setOnClickListener(this);

		return x;
	}

	/**
	 * This OnClickListener should only direct the necessary data to the OnItemListener. It gets the position of the item in the list by checking the ArrayList members.
	 */
	public void onClick(View v) {
		if (mListView == null || mOnItemClickListener == null || members == null)
			return;
		LinearLayout l = (LinearLayout) v;
		TextView t = (TextView) l.getChildAt(0);	
		for (int i = 0; i < getCount(); i++) {
			if (members.get(i).getMember().equals(t.getText().toString())) {
				mOnItemClickListener.onItemClick(mListView, v, i, i);
				break;
			}
		}
	}

	public void setParentListView(AdapterView<ListAdapter> listView) {
		mListView = listView;
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		mOnItemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener listener) {
		mOnItemLongClickListener = listener;
	}

	public boolean onLongClick(View v) {
		System.out.println("Yeah!");
		if (mListView == null || mOnItemLongClickListener == null || members == null)
			return false;
		System.out.println("Ik komt hier!");
		LinearLayout l = (LinearLayout) v;
		TextView t = (TextView) l.getChildAt(0);	
		for (int i = 0; i < getCount(); i++) {
			if (members.get(i).getMember().equals(t.getText().toString())) {
				mOnItemLongClickListener.onItemLongClick(mListView, v, i, i);
				return true;
			}
		}
		return false;
	}
}
