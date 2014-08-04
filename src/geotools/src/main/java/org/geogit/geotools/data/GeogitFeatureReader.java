/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.data;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.geogit.api.Context;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.Ref;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.DiffTree;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.geotools.data.GeoGitDataStore.ChangeType;
import org.geotools.data.FeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.spatial.ReprojectingFilterVisitor;
import org.geotools.filter.visitor.SpatialFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.Identifier;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Envelope;

/**
 *
 */
class GeogitFeatureReader<T extends FeatureType, F extends Feature> implements
        FeatureReader<T, F>, Iterator<F> {

    private static final Logger LOGGER = Logging.getLogger(GeogitFeatureReader.class);

    private SimpleFeatureType schema;

    private Iterator<SimpleFeature> features;

    @Nullable
    private Integer offset;

    @Nullable
    private Integer maxFeatures;

    /**
     * @param context
     * @param schema
     * @param origFilter
     * @param typeTreePath
     * @param headRef
     * @param oldHeadRef
     * @param offset
     * @param maxFeatures
     * @param changeType
     */
    public GeogitFeatureReader(final Context context, final SimpleFeatureType schema,
            final Filter origFilter, final String typeTreePath, final String headRef,
            String oldHeadRef, ChangeType changeType, @Nullable Integer offset,
            @Nullable Integer maxFeatures) {
        checkNotNull(context);
        checkNotNull(schema);
        checkNotNull(origFilter);
        checkNotNull(typeTreePath);
        checkNotNull(headRef);
        checkNotNull(oldHeadRef);
        checkNotNull(changeType);

        this.schema = schema;
        this.offset = offset;
        this.maxFeatures = maxFeatures;

        final String effectiveHead = headRef == null ? Ref.WORK_HEAD : headRef;
        final String effectiveOldHead = oldHeadRef == null ? RevTree.EMPTY_TREE_ID.toString()
                : oldHeadRef;
        final String typeTreeRefSpec = effectiveHead + ":" + typeTreePath;
        final Optional<RevTree> parentTree = context.command(RevObjectParse.class)
                .setRefSpec(typeTreeRefSpec).call(RevTree.class);

        Preconditions.checkArgument(parentTree.isPresent(), "Feature type tree not found: %s",
                typeTreeRefSpec);

        final Filter filter = reprojectFilter(origFilter);
        final ReferencedEnvelope queryBounds = getQueryBounds(filter);

        DiffTree diffOp = context.command(DiffTree.class);
        diffOp.setOldVersion(effectiveOldHead);
        diffOp.setNewVersion(effectiveHead);
        List<String> pathFilters = resolvePathFilters(typeTreePath, filter);
        diffOp.setPathFilter(pathFilters);
        if (!queryBounds.isEmpty()) {
            diffOp.setBoundsFilter(queryBounds);
        }
        diffOp.setChangeTypeFilter(changeType(changeType));

        Iterator<DiffEntry> diffs = diffOp.call();

        Iterator<NodeRef> featureRefs = toFeatureRefs(diffs, changeType);

        final boolean filterSupportedByRefs = Filter.INCLUDE.equals(filter)
                || filter instanceof BBOX || filter instanceof Id;

        if (filterSupportedByRefs) {
            featureRefs = applyRefsOffsetLimit(featureRefs);
        }

        NodeRefToFeature refToFeature = new NodeRefToFeature(context, schema);
        final Iterator<SimpleFeature> featuresUnfiltered = transform(featureRefs, refToFeature);

        FilterPredicate filterPredicate = new FilterPredicate(filter);
        Iterator<SimpleFeature> featuresFiltered = filter(featuresUnfiltered, filterPredicate);
        if (!filterSupportedByRefs) {
            featuresFiltered = applyFeaturesOffsetLimit(featuresFiltered);
        }
        this.features = featuresFiltered;
    }

    private org.geogit.api.plumbing.diff.DiffEntry.ChangeType changeType(ChangeType changeType) {
        if (changeType == null) {
            return org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
        }
        switch (changeType) {
        case ADDED:
            return org.geogit.api.plumbing.diff.DiffEntry.ChangeType.ADDED;
        case REMOVED:
            return org.geogit.api.plumbing.diff.DiffEntry.ChangeType.REMOVED;
        default:
            return org.geogit.api.plumbing.diff.DiffEntry.ChangeType.MODIFIED;
        }
    }

    private Iterator<NodeRef> toFeatureRefs(final Iterator<DiffEntry> diffs,
            final ChangeType changeType) {

        return Iterators.transform(diffs, new Function<DiffEntry, NodeRef>() {

            private final ChangeType diffType = changeType;

            @Override
            public NodeRef apply(DiffEntry e) {
                if (e.isAdd()) {
                    return e.getNewObject();
                }
                if (e.isDelete()) {
                    return e.getOldObject();
                }
                return ChangeType.CHANGED_OLD.equals(diffType) ? e.getOldObject() : e
                        .getNewObject();
            }
        });
    }

    private List<String> resolvePathFilters(String typeTreePath, Filter filter) {
        List<String> pathFilters;
        if (filter instanceof Id) {
            final Set<Identifier> identifiers = ((Id) filter).getIdentifiers();
            Iterator<FeatureId> featureIds = filter(
                    filter(identifiers.iterator(), FeatureId.class), notNull());
            Preconditions.checkArgument(featureIds.hasNext(), "Empty Id filter");
            pathFilters = new ArrayList<>();
            while (featureIds.hasNext()) {
                String fid = featureIds.next().getID();
                pathFilters.add(NodeRef.appendChild(typeTreePath, fid));
            }
        } else {
            pathFilters = ImmutableList.of(typeTreePath);
        }
        return pathFilters;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getFeatureType() {
        return (T) schema;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean hasNext() {
        return features.hasNext();
    }

    @SuppressWarnings("unchecked")
    @Override
    public F next() {
        return (F) features.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    private Iterator<SimpleFeature> applyFeaturesOffsetLimit(Iterator<SimpleFeature> features) {
        if (offset != null) {
            Iterators.advance(features, offset.intValue());
        }
        if (maxFeatures != null) {
            features = Iterators.limit(features, maxFeatures.intValue());
        }
        return features;
    }

    private Iterator<NodeRef> applyRefsOffsetLimit(Iterator<NodeRef> featureRefs) {
        if (offset != null) {
            Iterators.advance(featureRefs, offset.intValue());
        }
        if (maxFeatures != null) {
            featureRefs = Iterators.limit(featureRefs, maxFeatures.intValue());
        }
        return featureRefs;
    }

    private static class NodeRefToFeature implements Function<NodeRef, SimpleFeature> {

        private RevObjectParse parseRevFeatureCommand;

        private FeatureBuilder featureBuilder;

        public NodeRefToFeature(Context commandLocator, SimpleFeatureType schema) {
            this.featureBuilder = new FeatureBuilder(schema);
            this.parseRevFeatureCommand = commandLocator.command(RevObjectParse.class);
        }

        @Override
        public SimpleFeature apply(final NodeRef featureRef) {
            Optional<RevFeature> revFeature = parseRevFeatureCommand.setObjectId(
                    featureRef.objectId()).call(RevFeature.class);
            Preconditions.checkState(revFeature.isPresent());

            String id = featureRef.name();
            Feature feature = featureBuilder.build(id, revFeature.get());
            return (SimpleFeature) feature;
        }
    };

    private static final class FilterPredicate implements Predicate<SimpleFeature> {
        private Filter filter;

        public FilterPredicate(final Filter filter) {
            this.filter = filter;
        }

        @Override
        public boolean apply(SimpleFeature feature) {
            return filter.evaluate(feature);
        }
    }

    @Nullable
    private ReferencedEnvelope getQueryBounds(Filter filterInNativeCrs) {
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        if (crs == null) {
            crs = DefaultEngineeringCRS.GENERIC_2D;
        }
        ReferencedEnvelope queryBounds = new ReferencedEnvelope(crs);
        Envelope bounds = (Envelope) filterInNativeCrs.accept(new ExtractBounds(), queryBounds);
        if (bounds != null) {
            queryBounds.expandToInclude(bounds);
        }
        return queryBounds;
    }

    /**
     * @param filter
     * @return
     */
    private Filter reprojectFilter(Filter filter) {
        if (hasSpatialFilter(filter)) {
            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            if (crs == null) {
                LOGGER.fine("Not reprojecting filter to native CRS because feature type does not declare a CRS");

            } else {

                FilterFactory2 factory = CommonFactoryFinder.getFilterFactory2();

                filter = (Filter) filter.accept(new ReprojectingFilterVisitor(factory, schema),

                null);

            }
        }
        return filter;
    }

    private boolean hasSpatialFilter(Filter filter) {
        SpatialFilterVisitor spatialFilterVisitor = new SpatialFilterVisitor();
        filter.accept(spatialFilterVisitor, null);
        return spatialFilterVisitor.hasSpatialFilter();
    }
}
