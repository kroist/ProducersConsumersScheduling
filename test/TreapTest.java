import org.junit.Assert;
import org.junit.Test;

public class TreapTest {
    @Test
    public void lowerboundTest() {
        Treap treap = new Treap();
        treap.add(228);
        treap.add(1);
        Assert.assertEquals(treap.lowerBound(1), 1);
        Assert.assertEquals(treap.lowerBound(2), 228);
        Assert.assertEquals(treap.lowerBound(228), 228);
        Assert.assertEquals(treap.lowerBound(229), 228);
    }
    @Test
    public void lowerboundTest2() {
        Treap treap = new Treap();
        treap.add(228);
        treap.iterateOverTree();
        treap.add(1);
        treap.iterateOverTree();
        treap.add(10);
        treap.iterateOverTree();
        Assert.assertEquals(treap.lowerBound(1), 1);
        Assert.assertEquals(treap.lowerBound(2), 10);
        Assert.assertEquals(treap.lowerBound(11), 10);
        Assert.assertEquals(treap.lowerBound(12), 228);
        Assert.assertEquals(treap.lowerBound(228), 228);

        treap.remove(10);

        Assert.assertEquals(treap.lowerBound(1), 1);
        Assert.assertEquals(treap.lowerBound(2), 228);
        Assert.assertEquals(treap.lowerBound(10), 228);

        treap.remove(1);
        treap.add(10);
        Assert.assertEquals(treap.lowerBound(1), 10);
        Assert.assertEquals(treap.lowerBound(2), 10);
        Assert.assertEquals(treap.lowerBound(10), 10);
        Assert.assertEquals(treap.lowerBound(11), 228);

    }
}
