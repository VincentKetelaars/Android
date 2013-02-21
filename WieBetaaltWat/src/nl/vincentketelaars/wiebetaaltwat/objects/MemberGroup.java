package nl.vincentketelaars.wiebetaaltwat.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import android.os.Parcel;
import android.os.Parcelable;

public class MemberGroup implements Parcelable, Serializable{

	private ArrayList<Member> groupMembers;
	private String groupName;
	private long lastUpdate;


	public MemberGroup(ArrayList<Member> members) {
		setGroupMembers(members);
		setGroupName(null);
		setLastUpdate(System.currentTimeMillis());
	}

	public MemberGroup(ArrayList<Member> members, String groupName) {
		setGroupMembers(members);
		setGroupName(groupName);
		setLastUpdate(System.currentTimeMillis());
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
			if (m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}

	/**
	 * The first member with this email is returned, otherwise null.
	 * @param name
	 * @return Member with this name
	 */
	public Member getMemberByEmail(String email) {
		for (Member m : groupMembers) {
			if (m.getEmail().equals(email)) {
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
			if (m.getUid() == -1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * This method compares the two ArrayLists of Members for equal count. Both ArrayLists need to have the same number of Members.
	 * @param first
	 * @param second
	 * @return if both MemberLists have for each member the same count, it returns true. False otherwise.
	 */
	public boolean participantsHaveEqualCount(MemberGroup second) {
		for (Member f : getGroupMembers()) {
			for (Member s : second.getGroupMembers()) {
				if (s.getName().equals(f.getName())) {
					if (s.getCount() != f.getCount()) {
						return false;
					}
					continue;
				}
			}
		}
		return getGroupMembers().size() == second.getGroupMembers().size();
	}

	/**
	 * For each member in this group, if a Member by the same name is in nGroup, that count will be used from now on. 
	 * @param nGroup
	 */
	public void setParticipantsCount(MemberGroup nGroup) {
		for (Member p : nGroup.getGroupMembers()) {
			for (Member m : getGroupMembers()) {
				if (m.getName().equals(p.getName())) {
					m.setCount(p.getCount());
				}
			}
		}
	}

	/**
	 * This method returns the position of the Member in the list.
	 * @param s
	 * @return 0 for the first Member, -1 if the Member is not in the list.
	 */
	public int getMemberPosition(String s) {
		for (int i = 0; i < getGroupMembers().size(); i++) {
			if (s.equals(getMember(i).getName()))
				return i;
		}
		return -1;
	}

	/**
	 * This method returns the position of the Member in the list.
	 * @param m
	 * @return 0 for the first Member, -1 if the Member is not in the list.
	 */
	public int getMemberPosition(Member m) {
		return getMemberPosition(m.getName());
	}

	/**
	 * This method returns whether a member is in the list.
	 * @param m
	 * @return true if in the list, otherwise false;
	 */
	public boolean hasMember(String s) {
		return getMemberPosition(s) != -1;
	}

	public void addMember(Member member) {
		groupMembers.add(member);
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
		if (groupName != null)
			sb.append(groupName+", ");
		for (Member m : groupMembers) {
			sb.append(m+", ");
		}
		sb.delete(sb.lastIndexOf(", "), sb.lastIndexOf(", ")+2);
		sb.append(")>");
		return sb.toString();
	}

	public String memberNames() {
		StringBuilder s = new StringBuilder();
		for (Member m : getGroupMembers()) {
			s.append(m.getName());
			if (m.getCount() > 1) {
				s.append(" "+m.getCount()+"x");
			}
			s.append(", ");
		}
		return s.toString().substring(0,s.length()-2);
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
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
		dest.writeLong(lastUpdate);
	}

	/**
	 * This method assigns the values from the Parcel to the instances.
	 * @param in (Parcel)
	 */
	public void readFromParcel(Parcel in) {	
		groupMembers= in.readArrayList(Member.class.getClassLoader());
		groupName = in.readString();
		lastUpdate = in.readLong();
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

	/**
	 * Merge all the attributes of this GroupMember with the provided GroupMember
	 * @param temp
	 * @return
	 */
	public boolean mergeMemberGroup(MemberGroup temp) {
		if (groupMembers == null || temp.getGroupMembers().size() >= groupMembers.size()) {
			Collections.copy(groupMembers, temp.getGroupMembers());
			return true;
		}
		boolean success = true;
		for (Member m : temp.getGroupMembers()) {
			for (Member n : groupMembers) {
				if (m.getName().equals(n.getName())) {
					success = n.mergeMember(m) && success;
					break;
				}
			}
			groupMembers.add(m); // New WBWList
			m.setLastUpdate(System.currentTimeMillis());
		}			
		long time = System.currentTimeMillis();
		for (Member x : groupMembers) {
			if (Math.abs(time - x.getLastUpdate()) > 1000) // The updates should easily have been done in one second.
				groupMembers.remove(x); // Any groupList not in temp.getGroupLists() has to be removed from the current WBW.
		}
		return success;
	}
}
