package org.geogit.osm.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geogit.osm.cli.commands.OSMMap;
import org.geogit.storage.FieldType;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MappingTest {

    @Test
    public void TestDuplicatedFieldName() {
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        fields.put("lit", new AttributeDefinition("name", FieldType.STRING));
        fields.put("geom", new AttributeDefinition("name", FieldType.POINT));
        filters.put("highway", new ArrayList<String>());
        try {
            MappingRule mappingRule = new MappingRule("mapped", filters, fields);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().startsWith("Duplicated"));
            assertTrue(e.getMessage().endsWith("name"));
        }
    }

    @Test
    public void TestJsonSerialization() {
        Map<String, AttributeDefinition> fields = Maps.newHashMap();
        Map<String, List<String>> filters = Maps.newHashMap();
        filters.put("highway", Lists.newArrayList("bus_stop"));
        fields.put("geom", new AttributeDefinition("geom", FieldType.POINT));
        fields.put("name", new AttributeDefinition("name_alias", FieldType.STRING));
        MappingRule mappingRule = new MappingRule("busstops", filters, fields);
        List<MappingRule> mappingRules = Lists.newArrayList();
        mappingRules.add(mappingRule);
        Mapping mapping = new Mapping(mappingRules);

        GsonBuilder gsonBuilder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation();
        Gson gson = gsonBuilder.create();

        String s = gson.toJson(mapping);
        System.out.println(s);
        Mapping unmarshalledMapping = gson.fromJson(s, Mapping.class);
        assertEquals(mapping, unmarshalledMapping);

    }

    @Test
    public void TestLoadingFromFile() {
        String mappingFilename = OSMMap.class.getResource("mapping.json").getFile();
        File mappingFile = new File(mappingFilename);
        Mapping mapping = Mapping.fromFile(mappingFile.getAbsolutePath());
        List<MappingRule> rules = mapping.getRules();
        assertEquals(1, rules.size());
        MappingRule rule = rules.get(0);
        SimpleFeatureType ft = rule.getFeatureType();
        assertEquals("id", ft.getDescriptor(0).getLocalName());
        assertEquals("lit", ft.getDescriptor(1).getLocalName());
        assertEquals("geom", ft.getDescriptor(2).getLocalName());
        assertEquals("nodes", ft.getDescriptor(3).getLocalName());
    }

}
