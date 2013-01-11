package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class MemberGroup implements Parcelable, Serializable{

	private ArrayList<Member> groupMembers;
	private String groupName;

	public MemberGroup(ArrayList<Member> members) {
		setGroupMembers(members);
		setGroupName(null);
	}

	public MemberGroup(ArrayList<Member> members, String groupName) {
		setGroupMembers(members);
		setGroupName(groupName);
	}

	/**
	 * This constructor reads the instances from the Parcel.
	 * @param in
	 */
	public MemberGroup (Parcel in) {
		readFromParcel(in);
	}

	public Member getMember(int pos) {
		return groupMembers.get(pos);
	}

	/**
	 * The first member with this name is returned, otherwise null.
	 * @param name
	 * @return Member with this name
	 */
	public Member getMember(String name) {
		for (Member m : groupMembers) {
			if (m.getMember().equals(name)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * This method checks whether each Member in the list has an id.
	 * @return true if all members have an id, otherwise false
	 */
	public boolean membersHaveId() {
		for (Member m : groupMembers) {
			if (m.getId() == -1) {
				return false;
			}
		}
		return true;
	}

	public ArrayList<Member> getGroupMembers() {
		return groupMembers;
	}

	public void setGroupMembers(ArrayList<Member> groupMembers) {
		this.groupMembers = groupMembers;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<(");
		sb.append(groupName+", ");
		for (Member m : groupMembers) {
			sb.append(m+", ");
		}
		sb.delete(sb.lastIndexOf(", "), sb.lastIndexOf(", ")+2);
		sb.append(")>");
		return sb.toString();
	}

	/**
	 * Parcelable
	 * @return
	 */
	public int describeContents() {
		return 0;
	}

	/**
	 * This method writes each instance to the Parcel. This method takes a Parcel, and some kind of flag integer.
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(groupMembers);
		dest.writeString(groupName);
	}

	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {	
		groupMembers= in.readArrayList(Member.class.getClassLoader());
		groupName = in.readString();
	}

	/**
	 * This method.....
	 */
	public static final Parcelable.Creator<MemberGroup> CREATOR =
			new Parcelable.Creator<MemberGroup>() {
		public MemberGroup createFromParcel(Parcel in) {
			return new MemberGroup(in);
		}

		public MemberGroup[] newArray(int size) {
			return new MemberGroup[size];
		}
	};
}
