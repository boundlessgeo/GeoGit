/* Copyright (c) 2013-2014 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.net.InetAddress;

import org.locationtech.geogig.api.Context;
import org.locationtech.geogig.api.DefaultPlatform;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.GlobalContextBuilder;
import org.locationtech.geogig.api.Platform;
import org.locationtech.geogig.api.plumbing.ResolveGeogigDir;
import org.locationtech.geogig.cli.CLIContextBuilder;
import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.rest.repository.FixedEncoder;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.rest.repository.RepositoryRouter;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Protocol;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.base.Optional;
import com.noelios.restlet.application.Decoder;

/**
 * Both an embedded jetty launcher
 */
public class Main extends Application {

    static {
        setup();
    }

    private RepositoryProvider repoProvider;
    private Set<String> whiteList;

    public Main() {
        super();
    }

    public Main(GeoGIG geogig) {
        super();
        this.repoProvider = new SingleRepositoryProvider(geogig);
        this.whiteList = initializeWhiteList(geogig);
    }

    private static Set<String> initializeWhiteList(GeoGIG geogig) {
        Set<String> whiteList = new HashSet<String>();
        Optional<String> spec = geogig.getContext().configDatabase().get("serve.whitelist");
        if (spec.isPresent()) {
            String[] parts = spec.get().split("[ ,]");
            System.out.println("white list : " + Arrays.toString(parts));
            whiteList.addAll(Arrays.asList(parts));
        }
        return whiteList;
    }

    @Override
    public void setContext(org.restlet.Context context) {
        super.setContext(context);
        assert context != null;

        Map<String, Object> attributes = context.getAttributes();

        GeoGIG geogig;
        if (attributes.containsKey("geogig")) {
            geogig = (GeoGIG) attributes.get("geogig");
        } else {
            // revisit, not used at all
            // ServletContext sc = (ServletContext) dispatcher.getContext()
            // .getAttributes().get("org.restlet.ext.servlet.ServletContext");
            // String repo = sc.getInitParameter("repository");
            String repo = null;
            if (repo == null) {
                repo = System.getProperty("org.locationtech.geogig.web.repository");
            }
            if (repo == null) {
                return;
                // throw new IllegalStateException(
                // "Cannot launch geogig servlet without `repository` parameter");
            }
            geogig = loadGeoGIG(repo);
        }
        repoProvider = new SingleRepositoryProvider(geogig);
    }

    private boolean checkWhiteList(Request request, Logger logger) {
        String client = request.getClientInfo().getAddress();
        boolean authorized = false;
        if (!whiteList.isEmpty()) {
            if (client == null) {
                logger.warning("No client provided in request");
            } else if (!whiteList.contains(client)) {
                logger.info(client + " not in whitelist");
            } else {
                authorized = true;
            }
        } else {
            authorized = true;
        }
        return authorized;
    }

    @Override
    public Restlet createRoot() {

        Router router = new Router() {

            @Override
            public void handle(Request request, Response response) {
                if (!checkWhiteList(request, getContext().getLogger())) {
                    response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
                } else {
                    super.handle(request, response);
                }
            }

            @Override
            protected synchronized void init(Request request, Response response) {
                super.init(request, response);
                if (!isStarted()) {
                    return;
                }
                request.getAttributes().put(RepositoryProvider.KEY, repoProvider);
            }
        };
        RepositoryRouter root = new RepositoryRouter();

        router.attach("/repo", root);
        router.attach("/{command}.{extension}", CommandResource.class);
        router.attach("/{command}", CommandResource.class);

        org.restlet.Context context = getContext();
        // enable support for compressing responses if the client supports it.
        // NOTE: restlet 1.0.8 leaves a dangling thread on each request (see
        // EncodeRepresentation.getStream()
        // This problem is fixed in latest versions (2.x) of restlet. See the javadocs for
        // FixedEncoder for further detail
        // Encoder responseEncoder = new com.noelios.restlet.application.Encoder(context);
        FixedEncoder encoder = new FixedEncoder(context);
        encoder.setEncodeRequest(false);
        encoder.setEncodeResponse(true);
        encoder.setNext(router);

        Decoder decoder = new Decoder(context);
        decoder.setDecodeRequest(true);
        decoder.setDecodeResponse(false);
        decoder.setNext(encoder);

        return decoder;
    }

    static GeoGIG loadGeoGIG(String repo) {
        Platform platform = new DefaultPlatform();
        platform.setWorkingDir(new File(repo));
        Context inj = GlobalContextBuilder.builder.build();
        GeoGIG geogig = new GeoGIG(inj, platform.pwd());

        if (geogig.command(ResolveGeogigDir.class).call().isPresent()) {
            geogig.getRepository();
            return geogig;
        }

        return geogig;
    }

    static void startServer(String repo, boolean localhost) throws Exception {
        GeoGIG geogig = loadGeoGIG(repo);
        org.restlet.Context context = new org.restlet.Context();
        Application application = new Main(geogig);
        application.setContext(context);
        Component comp = new Component();
        comp.getDefaultHost().attach(application);
        if (localhost) {
            comp.getServers().add(Protocol.HTTP, InetAddress.getLocalHost().toString(), 8182);
        } else {
            comp.getServers().add(Protocol.HTTP, 8182);
        }
        comp.start();
    }

    static void setup() {
        GlobalContextBuilder.builder = new CLIContextBuilder();
    }

    public static void main(String[] args) throws Exception {
        LinkedList<String> argList = new LinkedList<String>(Arrays.asList(args));
        if (argList.size() == 0) {
            System.out.println("provide geogig repo path");
            System.exit(1);
        }
        boolean local = argList.remove("--local");
        if (!local) {
            System.err.println("you are starting geogig in a potentially insecure manner");
            System.err.println("provide the --local flag to bind to the localhost");
        }
        String repo = argList.pop();
        startServer(repo, local);
    }

}
