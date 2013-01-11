package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;
import java.util.ArrayList;

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
}
