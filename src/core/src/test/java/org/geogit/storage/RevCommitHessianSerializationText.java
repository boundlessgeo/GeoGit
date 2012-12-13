package org.geogit.storage;

import org.geogit.storage.hessian.HessianFactory;

public class RevCommitHessianSerializationText extends RevCommitSerializationTest {

    @Override
    protected ObjectSerialisingFactory getFactory() {
        return new HessianFactory();
    }

}
