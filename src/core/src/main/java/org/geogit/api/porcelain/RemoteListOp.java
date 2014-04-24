/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.api.porcelain;

import java.util.ArrayList;
import java.util.List;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Remote;
import org.geogit.storage.ConfigDatabase;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * Return a list of all of the remotes from the local config database.
 * 
 * @see ConfigDatabase
 */
public class RemoteListOp extends AbstractGeoGitOp<ImmutableList<Remote>> {

    /**
     * Executes the remote-list operation.
     * 
     * @return {@code List<Remote>} of all remotes found in the config database, may be empty.
     */
    @Override
    protected ImmutableList<Remote> _call() {
        ConfigDatabase config = configDatabase();
        List<String> remotes = config.getAllSubsections("remote");
        List<Remote> allRemotes = new ArrayList<Remote>();
        for (String remoteName : remotes) {
            String remoteSection = "remote." + remoteName;
            Optional<String> remoteFetchURL = config.get(remoteSection + ".url");
            Optional<String> remoteFetch = config.get(remoteSection + ".fetch");
            Optional<String> remoteMapped = config.get(remoteSection + ".mapped");
            Optional<String> remoteMappedBranch = config.get(remoteSection + ".mappedBranch");
            Optional<String> remoteUserName = config.get(remoteSection + ".username");
            Optional<String> remotePassword = config.get(remoteSection + ".password");
            if (remoteFetchURL.isPresent() && remoteFetch.isPresent()) {
                Optional<String> remotePushURL = config.get(remoteSection + ".pushurl");
                allRemotes.add(new Remote(remoteName, remoteFetchURL.get(), remotePushURL
                        .or(remoteFetchURL.get()), remoteFetch.get(), remoteMapped.or("false")
                        .equals("true"), remoteMappedBranch.orNull(), remoteUserName.orNull(),
                        remotePassword.orNull()));
            }
        }
        return ImmutableList.copyOf(allRemotes);
    }
}
