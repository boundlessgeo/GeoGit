package org.geogit.geotools.plubming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Platform;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geogit.geotools.porcelain.TestHelper;
import org.geogit.repository.WorkingTree;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.type.Name;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ImportOpTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @Test
    public void testNullDataStore() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testNullTableNotAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(false);
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testEmptyTableNotAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("");
        importOp.setAll(false);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testEmptyTableAndAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("");
        importOp.setAll(true);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.call();
    }

    @Test
    public void testTableAndAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setTable("table1");
        importOp.setAll(true);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testTableNotFound() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testNoFeaturesFound() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createEmptyTestFactory().createDataStore(null));
        importOp.setAll(true);
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testTypeNameException() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createFactoryWithGetNamesException().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testGetFeatureSourceException() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createFactoryWithGetFeatureSourceException()
                .createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testImportTable() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table1");
        importOp.call();
    }

    @Test
    public void testImportAll() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.call();
    }

    @Test
    public void testForceNoOverwrite() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table2");
        importOp.setDestinationPath("table");
        importOp.setOverwrite(false);
        importOp.call();
        Optional<RevFeature> feature = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec(Ref.WORK_HEAD + ":table/table2.1").call(RevFeature.class);
        assertTrue(feature.isPresent());
        importOp.setTable("table3");
        importOp.setForce(true);
        importOp.call();
        Optional<RevFeature> feature2 = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec(Ref.WORK_HEAD + ":table/table2.1").call(RevFeature.class);
        assertTrue(feature2.isPresent());
        assertEquals(feature, feature2);
    }

    @Test
    public void testForceOverwrite() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(false);
        importOp.setTable("table2");
        importOp.setDestinationPath("table");
        importOp.setOverwrite(true);
        importOp.call();
        Optional<RevFeature> feature = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec(Ref.WORK_HEAD + ":table/table2.1").call(RevFeature.class);
        assertTrue(feature.isPresent());
        importOp.setTable("table3");
        importOp.setForce(true);
        importOp.call();
        Optional<RevFeature> feature2 = cli.getGeogit().command(RevObjectParse.class)
                .setRefSpec(Ref.WORK_HEAD + ":table/table2.1").call(RevFeature.class);
        assertTrue(feature2.isPresent());
        assertNotSame(feature, feature2);
    }

    @Test
    public void testUncompatibleFeatureTypes() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.setDestinationPath("dest");
        exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    @Test
    public void testAlter() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setTable("table1");
        importOp.call();
        importOp.setTable("table2");
        importOp.setDestinationPath("table1");
        importOp.setAlter(true);
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(3, list.size());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(1, set.size());
        Optional<RevFeatureType> featureType = cli.getGeogit().command(RevObjectParse.class)
                .setObjectId(set.iterator().next()).call(RevFeatureType.class);
        assertTrue(featureType.isPresent());
        assertEquals("table2", featureType.get().getName().getLocalPart());
    }

    @Test
    public void testForce() throws Exception {
        ImportOp importOp = cli.getGeogit().command(ImportOp.class);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        importOp.setDestinationPath("dest");
        importOp.setForce(true);
        importOp.call();
        Iterator<NodeRef> features = cli.getGeogit().command(LsTreeOp.class)
                .setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES).call();
        ArrayList<NodeRef> list = Lists.newArrayList(features);
        assertEquals(3, list.size());
        TreeSet<ObjectId> set = Sets.newTreeSet();
        for (NodeRef node : list) {
            set.add(node.getMetadataId());
        }
        assertEquals(2, set.size());
    }

    @Test
    public void testDeleteException() throws Exception {
        WorkingTree workTree = mock(WorkingTree.class);
        doThrow(new Exception("Exception")).when(workTree).delete(any(Name.class));
        ImportOp importOp = new ImportOp(workTree);
        importOp.setDataStore(TestHelper.createTestFactory().createDataStore(null));
        importOp.setAll(true);
        // exception.expect(GeoToolsOpException.class);
        importOp.call();
    }

    private void setUpGeogit(GeogitCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }
}
