package org.geogit.gui.internal;

import org.geogit.api.GeoGIT;
import org.geogit.test.integration.RepositoryTestCase;
import org.junit.Test;

public class MapPaneTest extends RepositoryTestCase {

    @Override
    protected void setUpInternal() throws Exception {
        insertAndAdd(points1, points2, points3);
        //commit("points");
        insertAndAdd(lines1, lines2, lines3);
        //commit("lines");
    }

    @Test
    public void show() throws Exception {
        GeoGIT geogit = getGeogit();
        MapPane mapPane = new MapPane(geogit);
        mapPane.show();
        Thread.sleep(2000);
    }
}
