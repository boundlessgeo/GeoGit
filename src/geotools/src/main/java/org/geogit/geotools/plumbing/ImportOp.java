/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.plumbing;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.Node;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveFeatureType;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.geotools.plumbing.GeoToolsOpException.StatusCode;
import org.geogit.repository.WorkingTree;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.util.ProgressListener;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
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

    private boolean force;

    private boolean alter;

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

        if (alter && force) {
            throw new GeoToolsOpException(StatusCode.ALTER_AND_FORCE_DEFINED);
        }

        boolean foundTable = false;

        List<Name> typeNames;
        try {
            typeNames = dataStore.getNames();
        } catch (Exception e) {
            throw new GeoToolsOpException(StatusCode.UNABLE_TO_GET_NAMES);
        }

        if (typeNames.size() > 1 && alter) {
            throw new GeoToolsOpException(StatusCode.ALTER_AND_ALL_DEFINED);
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

            final RevFeatureType featureType = RevFeatureType.build(featureSource.getSchema());

            String path;
            if (destPath == null || destPath.isEmpty()) {
                path = featureType.getName().getLocalPart();
            } else {
                path = destPath;
            }

            NodeRef.checkValidPath(path);

            String refspec = Ref.WORK_HEAD + ":" + path;

            Optional<NodeRef> child = command(FindTreeChild.class).setChildPath(path)
                    .setParent(workTree.getTree()).call();
            if (child.isPresent()) {
                RevFeatureType defaultFeatureType = command(ResolveFeatureType.class)
                        .setFeatureType(refspec).call().get();
                if (featureType.equals(defaultFeatureType) || force) {
                    SimpleFeatureIterator featureIterator = features.features();
                    while (featureIterator.hasNext()) {
                        SimpleFeature feature = featureIterator.next();
                        String featurePath = NodeRef.appendChild(path, feature.getID());
                        Optional<Node> node = workTree.findUnstaged(featurePath);
                        if (node.isPresent()) {
                            if (overwrite) {
                                workTree.replace(path, feature);
                            }
                        } else {
                            workTree.insert(path, feature);
                        }
                    }
                } else if (alter) {
                    // first we modify the feature type and the existing features, if needed
                    // TODO: modify feature type
                    Optional<RevTree> tree = command(RevObjectParse.class).setRefSpec(refspec)
                            .call(RevTree.class);
                    Optional<ImmutableList<Node>> oldFeatures = tree.get().features();
                    if (oldFeatures.isPresent() && oldFeatures.get().size() != 0) {
                        UnmodifiableIterator<Node> nodeIterator = oldFeatures.get().iterator();
                        Iterator<Feature> iterator = filterAndTransformIterator(nodeIterator,
                                featureType);

                        ProgressListener progressListener = getProgressListener();
                        try {
                            Integer collectionSize = features.size();
                            workTree.insert(path, iterator, true, progressListener, null,
                                    collectionSize);
                        } catch (Exception e) {
                            throw new GeoToolsOpException(StatusCode.UNABLE_TO_INSERT);
                        }
                    }
                    // then we add the new ones
                    SimpleFeatureIterator featureIterator = features.features();
                    while (featureIterator.hasNext()) {
                        SimpleFeature feature = featureIterator.next();
                        String featurePath = NodeRef.appendChild(path, feature.getID());
                        Optional<Node> node = workTree.findUnstaged(featurePath);
                        if (node.isPresent()) {
                            if (overwrite) {
                                workTree.replace(path, feature);
                            }
                        } else {
                            workTree.insert(path, feature);
                        }
                    }

                } else {
                    throw new GeoToolsOpException(StatusCode.UNCOMPATIBLE_FEATURE_TYPE);
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
                    workTree.insert(path, iterator, true, progressListener, null, collectionSize);
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

    private Iterator<Feature> filterAndTransformIterator(UnmodifiableIterator<Node> nodeIterator,
            final RevFeatureType defaultFeatureType) {

        UnmodifiableIterator<Node> filteredIterator = Iterators.filter(nodeIterator,
                new Predicate<Node>() {

                    @Override
                    public boolean apply(@Nullable Node node) {
                        return canReplace(defaultFeatureType, featureType);
                    }

                });

        Iterator<Feature> iterator = Iterators.transform(filteredIterator,
                new Function<Node, Feature>() {
                    @Override
                    public Feature apply(Node node) {
                        return alter(node, featureType);
                    }

                });

        return iterator;

    }

    /**
     * returns true if both passed features types can be used interchangeably, so there is not need
     * to modify features, but just change the metadata id of the parent tree
     * 
     * @param featureType
     * @param FeatureType2
     * @return
     */
    private boolean canReplace(RevFeatureType featureType, RevFeatureType FeatureType2) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * Translates a feature pointed by a node from its original feature type to a given one, using
     * values from those attributes that exist in both original and destination feature type. New
     * attributes are populated with null values
     * 
     * @param node The node that points to the feature type. No checking is performed to ensure the
     *        node points to a feature instead of other type
     * @param featureType the destination feature type
     * @return a feature with the passed feature type and data taken from the input feature
     */
    private Feature alter(Node node, RevFeatureType featureType) {
        RevFeature oldFeature = command(RevObjectParse.class).setObjectId(node.getObjectId())
                .call(RevFeature.class).get();
        RevFeatureType oldFeatureType = command(RevObjectParse.class)
                .setObjectId(node.getObjectId()).call(RevFeatureType.class).get();
        ImmutableList<PropertyDescriptor> oldAttributes = oldFeatureType.sortedDescriptors();
        ImmutableList<PropertyDescriptor> newAttributes = featureType.sortedDescriptors();
        ImmutableList<Optional<Object>> oldValues = oldFeature.getValues();
        List<Optional<Object>> newValues = Lists.newArrayList();
        for (int i = 0; i < newAttributes.size(); i++) {
            int idx = oldAttributes.indexOf(newAttributes.get(i));
            if (idx != -1) {
                Optional<Object> oldValue = oldValues.get(i);
                newValues.add(oldValue);
            } else {
                newValues.add(Optional.absent());
            }
        }
        RevFeature newFeature = new RevFeature(node.getObjectId(), ImmutableList.copyOf(newValues));
        FeatureBuilder featureBuilder = new FeatureBuilder(featureType);
        Feature feature = featureBuilder.build(node.getName(), newFeature);
        return feature;
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
     *        features will not be overwritten, and a safe import is performed, where only those
     *        features that do not already exists are added
     * @return {@code this}
     */
    public ImportOp setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * @param force if true, it will change the default feature type of the tree we are importing
     *        into and change all features under that tree to have that same feature type
     * @return {@code this}
     */
    public ImportOp setAlter(boolean alter) {
        this.alter = alter;
        return this;
    }

    /**
     * @param force if true, import features even if their feature type do not match the feature
     *        type of the tree we are importing into
     * @return {@code this}
     */
    public ImportOp setForce(boolean force) {
        this.force = force;
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
