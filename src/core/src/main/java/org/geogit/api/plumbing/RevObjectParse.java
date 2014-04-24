/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.ObjectId;
import org.geogit.api.RevObject;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Resolves the reference given by a ref spec to the {@link RevObject} it finally points to,
 * dereferencing symbolic refs as necessary.
 * 
 * @see RevParse
 * @see ResolveObjectType
 */
public class RevObjectParse extends AbstractGeoGitOp<Optional<RevObject>> {

    private ObjectId objectId;

    private String refSpec;

    /**
     * @param refSpec the ref spec to resolve
     * @return {@code this}
     */
    public RevObjectParse setRefSpec(final String refSpec) {
        this.objectId = null;
        this.refSpec = refSpec;
        return this;
    }

    /**
     * @param objectId the {@link ObjectId object id} to resolve
     * @return {@code this}
     */
    public RevObjectParse setObjectId(final ObjectId objectId) {
        this.refSpec = null;
        this.objectId = objectId;
        return this;
    }

    /**
     * @return the resolved object id
     * @throws IllegalArgumentException if the provided refspec doesn't resolve to any known object
     * @see RevObject
     */
    @Override
    protected Optional<RevObject> _call() throws IllegalArgumentException {
        return call(RevObject.class);
    }

    /**
     * @param clazz the base type of the parsed objects
     * @return the resolved object id
     * @see RevObject
     */
    public <T extends RevObject> Optional<T> call(Class<T> clazz) {
        final ObjectId resolvedObjectId;
        if (objectId == null) {
            Optional<ObjectId> parsed = command(RevParse.class).setRefSpec(refSpec).call();
            if (parsed.isPresent()) {
                resolvedObjectId = parsed.get();
            } else {
                resolvedObjectId = ObjectId.NULL;
            }
        } else {
            resolvedObjectId = objectId;
        }
        if (resolvedObjectId.isNull()) {
            return Optional.absent();
        }

        RevObject revObject = stagingDatabase().get(resolvedObjectId);
        Preconditions.checkArgument(clazz.isAssignableFrom(revObject.getClass()),
                "Wrong return class for RevObjectParse operation");

        return Optional.of(clazz.cast(revObject));
    }
}
