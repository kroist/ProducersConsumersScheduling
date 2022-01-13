import java.util.Random;

public class Treap {
    public static final Random rand = new Random();
    private volatile Node root;

    private static class NodePair {
        public Node first;
        public Node second;
        NodePair(Node first, Node second) {
            this.first = first;
            this.second = second;
        }
    }

    public void add(int data) {
        root = insert(root, new Node(data));
    }

    private NodePair split(Node node, int key) {
        if (node == null) {
            return new NodePair(null, null);
        }
        if (key < node.data) {
            NodePair np = split(node.left, key);
            node.left = np.second;
            node.recomputeSum();
            np.second = node;
            return np;
        }
        else {
            NodePair np = split(node.right, key);
            node.right = np.first;
            node.recomputeSum();
            np.first = node;
            return np;
        }
    }

    private Node merge(Node l, Node r) {
        if (l == null)
            return r;
        if (r == null)
            return l;
        Node node;
        if (l.priority > r.priority) {
            l.right = merge(l.right, r);
            node = l;
        }
        else {
            r.left = merge(l, r.left);
            node = r;
        }
        node.recomputeSum();
        return node;
    }

    private Node insert(Node node, Node insNode) {
        if (node == null)
            return insNode;
        if (insNode.priority > node.priority) {
            NodePair pr = split(node, insNode.data);
            insNode.left = pr.first;
            insNode.right = pr.second;
            insNode.recomputeSum();
            return insNode;
        }
        if (insNode.data < node.data) {
            node.left = insert(node.left, insNode);
        }
        else {
            node.right = insert(node.right, insNode);
        }
        node.recomputeSum();
        return node;
    }

    public void remove(int data) {
        root = remove(root, data);
    }

    private Node remove(Node node, int data) {
        if (node == null)
            return null;
        if (node.data == data) {
            return merge(node.left, node.right);
        }
        else {
            if (data < node.data) {
                node.left = remove(node.left, data);
            }
            else {
                node.right = remove(node.right, data);
            }
        }
        node.recomputeSum();
        return node;
    }

    public int lowerBound(long needSum) {
        return lowerBound(root, needSum);
    }
    private int lowerBound(Node node, long needSum) {
        long sumLeft = 0;
        if (node.left != null) {
            sumLeft = node.left.sum;
            if (sumLeft >= needSum)
                return lowerBound(node.left, needSum);
        }
        sumLeft += node.data;
        if (sumLeft >= needSum)
            return node.data;
        return lowerBound(node.right, needSum-sumLeft);
    }

    public void iterateOverTree() {
        dfs(root);
    }
    private void dfs(Node node) {
        if (node == null)
            return;
        System.out.println(node.data + " " + node.sum);
        System.out.println("<");
        dfs(node.left);
        System.out.println(">");
        dfs(node.right);
    }

    public long sum() {
        if (root == null)
            return 0;
        return root.sum;
    }

    public static class Node {
        public volatile int data;
        public volatile long sum;

        public final int priority = rand.nextInt();

        public volatile Node left, right;

        public Node(int data) {
            this.data = data;
            this.sum = data;
        }
        public void recomputeSum() {
            this.sum = this.data;
            if (this.left != null)
                this.sum += this.left.sum;
            if (this.right != null)
                this.sum += this.right.sum;
        }
    }
}
