/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage;

/**
 *
 */
public class RevCommitTextSerializationTest extends RevCommitSerializationTest {

    @Override
    protected ObjectSerialisingFactory getFactory() {
        return new TextSerializationFactory();
    }

}
