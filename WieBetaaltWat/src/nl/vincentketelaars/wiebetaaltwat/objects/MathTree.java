package nl.vincentketelaars.wiebetaaltwat.objects;
import java.util.Stack;


public class MathTree {
	
	private MathNode rootNode;
	
	public MathTree() {
		
	}

	public MathNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(MathNode rootNode) {
		this.rootNode = rootNode;
	}
	
	public MathNode getLeftMostNode() {
		MathNode current = getRootNode();
		while (current.hasChildren() && !current.childrenAreLeafs()) {
			if (!current.getLeft().isLeafNode()) {
				current = current.getLeft();
			} else {
				current = current.getRight();
			}
		}
		return current;
	}
	
	public String toString() {
		Stack<MathNode> stack = new Stack<MathNode>();
		stack.push(rootNode);
		StringBuilder sb = new StringBuilder();
		while (!stack.isEmpty()) {
			MathNode node = stack.pop();
			if (node.hasChildren()) {
				stack.push(node.getRight());
				stack.push(node.getLeft());
			} else {
				sb.append("Node: "+node+"\n");
			}			
		}
		return sb.toString();
	}

}