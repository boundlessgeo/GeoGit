package org.geogit.api.plumbing;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureBuilder;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.RevTreeBuilder;
import org.geogit.storage.ObjectDatabase;
import org.geogit.storage.ObjectSerialisingFactory;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

import com.google.common.base.Suppliers;

public class CatObjectTest extends RepositoryTestCase {

    private ObjectDatabase odb;

    private ObjectSerialisingFactory serialFactory;

    private static final ObjectId FAKE_ID = ObjectId.forString("fake");

    private static final String FEATURE_PREFIX = "Feature.";

    @Override
    protected void setUpInternal() throws Exception {
        odb = repo.getObjectDatabase();
        serialFactory = repo.getSerializationFactory();
    }

    @Test
    public void TestCatTreeWithoutBucketsObject() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT / 2;
        RevTree tree = createTree(numChildren);
        CharSequence desc = geogit.command(CatObject.class).setObject(Suppliers.ofInstance(tree))
                .call();
        String[] lines = desc.toString().split("\n");
        assertEquals(numChildren + 1, lines.length);
        HashSet<String> featureNames = new HashSet<String>();
        for (int i = 1; i < lines.length; i++) {
            String[] tokens = lines[i].split("-->");
            assertEquals(FAKE_ID.toString(), tokens[0].trim());
            featureNames.add(tokens[1]);
        }
        assertEquals(numChildren, featureNames.size());

    }

    @Test
    public void TestCatTreeWithBucketsObject() throws Exception {
        int numChildren = RevTree.NORMALIZED_SIZE_LIMIT * 2;
        RevTree tree = createTree(numChildren);
        CharSequence desc = geogit.command(CatObject.class).setObject(Suppliers.ofInstance(tree))
                .call();
        String[] lines = desc.toString().split("\n");
        assertEquals(numChildren + 1 + tree.buckets().get().size(), lines.length);
        HashSet<String> featureNames = new HashSet<String>();
        for (int i = 1; i < lines.length; i++) {
            String[] tokens = lines[i].split("-->");
            if (tokens[1].contains(FEATURE_PREFIX)) {
                assertEquals(FAKE_ID.toString(), tokens[0].trim());
                featureNames.add(tokens[1]);
            }
        }
        assertEquals(numChildren, featureNames.size());
    }


    private RevTree createTree(int numChildren) {
        RevTreeBuilder rtb = new RevTreeBuilder(odb, serialFactory);
        for (int i = 0; i < numChildren; i++) {
            String key = FEATURE_PREFIX + i;
            NodeRef ref = new NodeRef(key, FAKE_ID, FAKE_ID, TYPE.FEATURE);
            rtb.put(ref);
        }
        return rtb.build();

    }

    @Test
    public void TestCatFeatureObject() {
        RevFeatureBuilder rfb = new RevFeatureBuilder();
        RevFeature feature = rfb.build(points1);
        CharSequence desc = geogit.command(CatObject.class)
                .setObject(Suppliers.ofInstance(feature)).call();
        String[] lines = desc.toString().split("\n");

        assertEquals(points1.getProperties().size() + 1, lines.length);
        assertEquals("1000", lines[1]);
        assertEquals("POINT (1 1)", lines[2]);
        assertEquals("StringProp1_1", lines[3]);

    }

}
