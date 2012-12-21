/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.util.Iterator;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

import com.google.common.base.Optional;
import com.google.common.collect.AbstractIterator;
import com.google.inject.Inject;

/**
 * Internal operation for importing tables from a GeoTools {@link DataStore}.
 * 
 * @see DataStore
 */
public class ImportOp extends AbstractGeoGitOp<RevTree> {

    private boolean all = false;

    private String table = null;

    /**
     * The path to import the data into
     */
    private String destPath;

    private DataStore dataStore;

    private WorkingTree workTree;

    private boolean overwrite = true;

    /**
     * Constructs a new import operation with the given working tree.
     * 
     * @param workTree the working tree where features will be imported to
     */
    @Inject
    public ImportOp(final WorkingTree workTree) {
        this.workTree = workTree;
    }

    /**
     * Executes the import operation using the parameters that have been specified. Features will be
     * added to the working tree, and a new working tree will be constructed. Either {@code all} or
     * {@code table}, but not both, must be set prior to the import process.
     * 
     * @return RevTree the new working tree
     */
    @Override
    public RevTree call() {
        if (dataStore == null) {
            throw new GeoToolsOpException(StatusCode.DATASTORE_NOT_DEFINED);
        }

        if ((table == null || table.isEmpty()) && all == false) {
            throw new GeoToolsOpException(StatusCode.TABLE_NOT_DEFINED);
        }

        if (table != null && !table.isEmpty() && all == true) {
            throw new GeoToolsOpException(StatusCode.ALL_AND_TABLE_DEFINED);
        }

        boolean foundTable = false;

        List<Name> typeNames;
        try {
            typeNames = dataStore.getNames();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
        }

        for (Name typeName : typeNames) {
            if (!all && !table.equals(typeName.toString()))
                continue;

            foundTable = true;

            SimpleFeatureSource featureSource;
            SimpleFeatureCollection features;
            try {
                featureSource = dataStore.getFeatureSource(typeName);
                features = featureSource.getFeatures();
            } catch (Exception e) {
                throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_FEATURES);
            }

            RevFeatureType revType = RevFeatureType.build(featureSource.getSchema());

            if (destPath == null || destPath.isEmpty()) {
                destPath = revType.getName().getLocalPart();
            }

            NodeRef.checkValidPath(destPath);

            Optional<ObjectId> tree = command(ResolveTreeish.class).setTreeish(destPath).call();
            if (!overwrite && tree.isPresent()) {
                SimpleFeatureIterator featureIterator = features.features();
                while (featureIterator.hasNext()) {
                    SimpleFeature feature = featureIterator.next();
                    String path = NodeRef.appendChild(destPath, feature.getID());
                    Optional<Node> node = workTree.findUnstaged(path);
                    if (!node.isPresent()) {
                        workTree.insert(destPath, feature);
                    }
                }
            } else {
                final SimpleFeatureIterator featureIterator = features.features();
                Iterator<Feature> iterator = new AbstractIterator<Feature>() {
                    @Override
                    protected Feature computeNext() {
                        if (!featureIterator.hasNext()) {
                            return super.endOfData();
                        }
                        return featureIterator.next();
                    }
                };
                ProgressListener progressListener = getProgressListener();
                try {
                    Integer collectionSize = features.size();
                    workTree.insert(destPath, iterator, true, progressListener, null,
                            collectionSize);
                } catch (Exception e) {
                    throw new GeoToolsOpException(StatusCode.UNABLE_TO_INSERT);
                } finally {
                    featureIterator.close();
                }
            }
        }
        if (!foundTable) {
            if (all) {
                throw new GeoToolsOpException(StatusCode.NO_FEATURES_FOUND);
            } else {
                throw new GeoToolsOpException(StatusCode.TABLE_NOT_FOUND);
            }
        }
        return workTree.getTree();
    }

    /**
     * @param all if this is set, all tables from the data store will be imported
     * @return {@code this}
     */
    public ImportOp setAll(boolean all) {
        this.all = all;
        return this;
    }

    /**
     * @return whether or not the all flag has been set
     */
    public boolean getAll() {
        return all;
    }

    /**
     * @param table if this is set, only the specified table will be imported from the data store
     * @return {@code this}
     */
    public ImportOp setTable(String table) {
        this.table = table;
        return this;
    }

    /**
     * @return the table that has been set, or null
     */
    public String getTable() {
        return table;
    }

    /**
     * 
     * @param overwrite If this is true, existing features will be overwritten in case they exist
     *        and have the same path and Id than the features to import. If this is false, existing
     *        features will not be overwritten, and an exception will be thrown if trying to import
     *        a feature into a path that already contains another one with the same Id
     * @return {@code this}
     */
    public ImportOp setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * 
     * @param destPath the path to import to to. If not provided, it will be taken from the feature
     *        type of the table to import.
     */
    public ImportOp setDestinationPath(String destPath) {
        this.destPath = destPath;
        return this;
    }

    /**
     * @param dataStore the data store to use for the import process
     * @return {@code this}
     */
    public ImportOp setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
        return this;
    }

    /**
     * @return the data store that has been set
     */
    public DataStore getDataStore() {
        return dataStore;
    }

}
