package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This class represents the WBWList object. It represents a certain group of people that spend money on each other. It includes the name of this group, the html to their expenselist. 
 * The balance of the account owner, and the balance of highest balance holder, and the lowest. It also has an arraylist containing these expenses and an arraylist of groupmembers. And 
 * also an list id.
 */
public class WBWList implements Parcelable, Serializable {
	private String HTML;
	private String listName;
	private Member me;
	private Member high;
	private Member low;
	private ArrayList<Expense> expenses;
	private MemberGroup groupMembers;
	private String lid;
	private ArrayList<Integer> resultsPerPage;
	private int pages;
	private int numResults;
	private ArrayList<MemberGroup> groupLists;
	private long lastUpdate;
	
	public WBWList (String html, String list, Member me, Member high, Member low, String lid) {
		setHTML(html);
		setListName(list);		
		setMe(me);
		setHighestMember(high);
		setLowestMember(low);
		setGroupMembers(null);
		setExpenses(null);
		setLid(lid);
		setResultsPerPage(null);
		setPages(0);
		setNumResults(0);
		setLastUpdate(System.currentTimeMillis());
	}
		
	public WBWList (String html, String list, Member me, Member high, Member low, MemberGroup groupMembers, String lid) {
		setHTML(html);
		setListName(list);		
		setMe(me);
		setHighestMember(high);
		setLowestMember(low);
		setGroupMembers(groupMembers);
		setExpenses(null);
		setLid(lid);
		setResultsPerPage(null);
		setPages(0);
		setNumResults(0);
		setLastUpdate(System.currentTimeMillis());
	}
	
	/**
	 * This constructor reads the instances from the Parcel.
	 * @param in
	 */
	public WBWList (Parcel in) {
		readFromParcel(in);
	}

	public String getHTML() {
		return HTML;
	}

	public void setHTML(String hTML) {
		HTML = hTML;
	}

	public Member getMe() {
		return me;
	}

	public void setMe(Member me) {
		this.me = me;
	}

	public Member getHighestMember() {
		return high;
	}

	public void setHighestMember(Member high) {
		this.high = high;
	}

	public Member getLowestMember() {
		return low;
	}

	public void setLowestMember(Member low) {
		this.low = low;
	}
	
	public String toString() {
		return "<(HTML: "+getHTML()+", Myself: "+getMe()+", high: "+getHighestMember()+", low: "+getLowestMember()+", members: "+getGroupMembers()+", expenses: "+getExpenses()+")>";
	}

	public ArrayList<Expense> getExpenses() {
		return expenses;
	}

	public void setExpenses(ArrayList<Expense> expenses) {
		this.expenses = expenses;
	}

	public MemberGroup getGroupMembers() {
		return groupMembers;
	}

	public void setGroupMembers(MemberGroup groupMembers) {
		this.groupMembers = groupMembers;
	}

	public String getLid() {
		return lid;
	}

	public void setLid(String lid) {
		this.lid = lid;
	}
	
	public ArrayList<Integer> getResultsPerPage() {
		return resultsPerPage;
	}

	public void setResultsPerPage(ArrayList<Integer> resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	public int getPages() {
		return pages;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public int getNumResults() {
		return numResults;
	}

	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}

	public String getListName() {
		return listName;
	}

	public void setListName(String listName) {
		this.listName = listName;
	}

	public ArrayList<MemberGroup> getGroupLists() {
		return groupLists;
	}

	public void setGroupLists(ArrayList<MemberGroup> groupLists) {
		this.groupLists = groupLists;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	/**
	 * This is a mandatory method with the parcelable interface
	 */
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * This method writes each instance to the Parcel. This method takes a Parcel, and some kind of flag integer.
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(HTML);
		dest.writeString(listName);
		dest.writeParcelable(me, 0);
		dest.writeParcelable(high, 0);
		dest.writeParcelable(low, 0);
		dest.writeList(expenses);
		dest.writeParcelable(groupMembers, 0);
		dest.writeString(lid);
		dest.writeList(resultsPerPage);
		dest.writeInt(pages);
		dest.writeInt(numResults);
		dest.writeList(groupLists);
		dest.writeLong(lastUpdate);
	}
	
	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {	
		HTML = in.readString();
		listName = in.readString();
		me = in.readParcelable(Member.class.getClassLoader());
		high = in.readParcelable(Member.class.getClassLoader());
		low = in.readParcelable(Member.class.getClassLoader());
		expenses = in.readArrayList(Expense.class.getClassLoader());
		groupMembers= in.readParcelable(MemberGroup.class.getClassLoader());
		lid = in.readString();
		resultsPerPage = in.readArrayList(Integer.class.getClassLoader());
		pages = in.readInt();
		numResults = in.readInt();
		groupLists = in.readArrayList(MemberGroup.class.getClassLoader());
		lastUpdate = in.readLong();
	}

	/**
	 * This method.....
	 */
	public static final Parcelable.Creator<WBWList> CREATOR =
			new Parcelable.Creator<WBWList>() {
		public WBWList createFromParcel(Parcel in) {
			return new WBWList(in);
		}

		public WBWList[] newArray(int size) {
			return new WBWList[size];
		}
	};

	/**
	 * merge a WBWList
	 * @param temp
	 * @return
	 */
	public boolean mergeWBWList(WBWList temp) {
		setListName(temp.getListName());
		setMe(temp.getMe());
		setHighestMember(temp.getHighestMember());
		setLowestMember(temp.getLowestMember());
		setLid(temp.getLid());
		setPages(temp.getPages());
		setNumResults(temp.getNumResults());
		boolean success = groupMembers.mergeMemberGroup(temp.getGroupMembers());
		success = mergeExpenses(temp.getExpenses()) && success;
		success = mergeGroupLists(temp.getGroupLists()) && success;		
		lastUpdate = System.currentTimeMillis();	
		return success;
	}
	
	/**
	 * merge the Expenses list
	 * Make sure that the expenses are sorted by last altered.
	 * @param temp
	 * @return
	 */
	public boolean mergeExpenses(ArrayList<Expense> temp) {
		if (expenses == null || temp.size() >= expenses.size()) {
			Collections.copy(expenses, temp);
			return true;
		}
		ArrayList<Expense> first = new ArrayList<Expense>();
		int index = 0;
		int myTry = 0;
		for (Expense e : temp) {
			Expense e1 = expenses.get(index);
			if (e.getTid() != e1.getTid()) {
				first.add(e);
				myTry = 0;
			} else if (!e.equals(e1)) {
				expenses.set(index, e);
				index++;
				myTry = 0;
			} else { // No change has been made, so we try three times if the next has made no change either.
				myTry++;
				index++;
				if (myTry == 3)
					break;
			}
		}
		first.addAll(expenses);
		Collections.copy(expenses, first);
		return true;
	}
	
	/**
	 * merge the groupLists.
	 * @param temp
	 * @return
	 */
	public boolean mergeGroupLists(ArrayList<MemberGroup> temp) {
		if (groupLists == null || temp.size() >= groupLists.size()) {
			Collections.copy(groupLists, temp);
			return true;
		}
		boolean success = true;
		for (MemberGroup m : temp) {
			for (MemberGroup n : groupLists) {
				if (m.getGroupName().equals(n.getGroupName())) {
					success = n.mergeMemberGroup(m) && success;
					break;
				}
			}
			groupLists.add(m); // New WBWList
			m.setLastUpdate(System.currentTimeMillis());
		}			
		long time = System.currentTimeMillis();
		for (MemberGroup x : groupLists) {
			if (Math.abs(time - x.getLastUpdate()) > 1000) // The updates should easily have been done in one second.
				groupLists.remove(x); // Any groupList not in temp.getGroupLists() has to be removed from the current WBW.
		}
		return success;
	}
}
