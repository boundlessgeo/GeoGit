/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geogit.geotools.data;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalInjectorBuilder;
import org.geogit.api.InjectorBuilder;
import org.geogit.cli.CLIInjectorBuilder;
import org.geogit.repository.Repository;
import org.geotools.data.DataStoreFactorySpi;

import com.google.common.base.Preconditions;

public class GeoGitDataStoreFactory implements DataStoreFactorySpi {

    static {
        if (GlobalInjectorBuilder.builder == null
                || GlobalInjectorBuilder.builder.getClass().equals(InjectorBuilder.class)) {
            GlobalInjectorBuilder.builder = new CLIInjectorBuilder();
        }
    }

    public static final Param REPOSITORY = new Param("geogit_repository", File.class,
            "Root directory for the geogit repository", true, "/path/to/repository");

    public static final Param DEFAULT_NAMESPACE = new Param("namespace", String.class,
            "Optional namespace for feature types that do not declare a Namespace themselves",
            false);

    @Override
    public String getDisplayName() {
        return "GeoGIT";
    }

    @Override
    public String getDescription() {
        return "GeoGIT Versioning DataStore";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[] { REPOSITORY, DEFAULT_NAMESPACE };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            Object repository = REPOSITORY.lookUp(params);
            return repository instanceof File && ((File) repository).isDirectory();
        } catch (IOException e) {
            //
        }
        return false;
    }

    /**
     * @see org.geotools.data.DataAccessFactory#isAvailable()
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public GeoGitDataStore createDataStore(Map<String, Serializable> params) throws IOException {

        String defaultNamespace = (String) DEFAULT_NAMESPACE.lookUp(params);

        File repositoryRoot = (File) REPOSITORY.lookUp(params);

        GlobalInjectorBuilder.builder = new CLIInjectorBuilder();
        GeoGIT geogit;
        try {
            geogit = new GeoGIT(repositoryRoot);
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
        Repository repository = geogit.getRepository();
        if (null == repository) {
            throw new IOException(String.format("Directory is not a geogit repository: '%s'",
                    repositoryRoot.getAbsolutePath()));
        }

        GeoGitDataStore store = new GeoGitDataStore(geogit);
        if (defaultNamespace != null) {
            store.setNamespaceURI(defaultNamespace);
        }
        return store;
    }

    /**
     * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
     */
    @Override
    public GeoGitDataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        String defaultNamespace = (String) DEFAULT_NAMESPACE.lookUp(params);

        File repositoryRoot = (File) REPOSITORY.lookUp(params);
        if (!repositoryRoot.isDirectory()) {
            if (repositoryRoot.exists()) {
                throw new IOException(repositoryRoot.getAbsolutePath() + " is not a directory");
            }
            repositoryRoot.mkdirs();
        }

        GeoGIT geogit = new GeoGIT(repositoryRoot);

        try {
            Repository repository = geogit.getOrCreateRepository();
            Preconditions.checkState(repository != null);
        } catch (RuntimeException e) {
            throw new IOException(e);
        }

        GeoGitDataStore store = new GeoGitDataStore(geogit);
        if (defaultNamespace != null) {
            store.setNamespaceURI(defaultNamespace);
        }
        return store;
    }

}
