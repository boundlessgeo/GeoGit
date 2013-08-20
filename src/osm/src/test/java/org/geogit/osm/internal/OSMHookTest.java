/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.storage.FieldType;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class OSMHookTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Override
    protected void setUpInternal() throws Exception {
        repo.getConfigDatabase().put("user.name", "groldan");
        repo.getConfigDatabase().put("user.email", "groldan@opengeo.org");
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testOSMHook() throws Exception {

        // set the hook that will trigger an unmapping when something is imported to the busstops
        // tree
        CharSequence commitPreHookCode = "var diffs = geogit.getFeaturesToCommit(\"busstops\", false);\n"
                + "if (diffs.length > 0){\n"
                + "\tvar params = {\"path\" : \"busstops\"};\n"
                + "\tgeogit.run(\"org.geogit.osm.internal.OSMUnmapOp\", params)\n}";
        File hooksFolder = new File(geogit.getPlatform().pwd(), ".geogit/hooks");
        File commitPreHookFile = new File(hooksFolder, "pre_commit.js");

        Files.write(commitPreHookCode, commitPreHookFile, Charsets.UTF_8);

        // Import
        String filename = OSMImportOp.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        geogit.command(OSMImportOp.class).setDataSource(file.getAbsolutePath()).call();

        // Map
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> mappings = Maps.newHashMap();
        mappings.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        fields.put("name", new AttributeDefinition("name", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("busstops", mappings, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call();
        geogit.command(OSMMapOp.class).setMapping(mapping).call();

        Optional<RevFeature> revFeature = geogit.command(RevObjectParse.class)
                .setRefSpec("HEAD:busstops/507464799").call(RevFeature.class);
        assertTrue(revFeature.isPresent());
        Optional<RevFeatureType> featureType = geogit.command(ResolveFeatureType.class)
                .setRefSpec("HEAD:busstops/507464799").call();
        assertTrue(featureType.isPresent());
        ImmutableList<Optional<Object>> values = revFeature.get().getValues();
        assertEquals(3, values.size());
        String wkt = "POINT (7.1959361 50.739397)";
        assertEquals(wkt, values.get(2).get().toString());
        assertEquals(507464799l, values.get(0).get());

        // Modify a node
        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder((SimpleFeatureType) featureType.get()
                .type());
        fb.set("geom", gf.createPoint(new Coordinate(0, 1)));
        fb.set("name", "newname");
        fb.set("id", 507464799l);
        SimpleFeature newFeature = fb.buildFeature("507464799");
        geogit.getRepository().getWorkingTree().insert("busstops", newFeature);
        geogit.command(AddOp.class).call();
        geogit.command(CommitOp.class).setMessage("msg").call(); // this should trigger the hook

        // check that the unmapping has been triggered and the unmapped node has the changes we
        // introduced
        Optional<RevFeature> unmapped = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/507464799").call(RevFeature.class);
        assertTrue(unmapped.isPresent());
        values = unmapped.get().getValues();
        assertEquals("POINT (0 1)", values.get(6).get().toString());
        assertEquals(
                "bus:yes|public_transport:platform|highway:bus_stop|VRS:ortsteil:Hoholz|name:newname|VRS:ref:68566|VRS:gemeinde:BONN",
                values.get(3).get().toString());
        // check that unchanged nodes keep their attributes
        Optional<RevFeature> unchanged = geogit.command(RevObjectParse.class)
                .setRefSpec("WORK_HEAD:node/1633594723").call(RevFeature.class);
        values = unchanged.get().getValues();
        assertEquals("14220478", values.get(4).get().toString());
        assertEquals("1355097351000", values.get(2).get().toString());
        assertEquals("2", values.get(1).get().toString());

    }
}
