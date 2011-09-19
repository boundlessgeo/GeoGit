package org.geogit.repository.remote;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.geogit.api.ObjectId;
import org.geogit.api.RevBlob;
import org.geogit.api.RevCommit;
import org.geogit.api.RevTree;
import org.geogit.repository.Repository;
import org.geogit.repository.remote.payload.IPayload;
import org.geogit.repository.remote.payload.Payload;
import org.geogit.storage.BlobReader;
import org.geogit.storage.hessian.HessianCommitReader;
import org.geogit.storage.hessian.HessianRevTreeReader;

/**
 * A Remote is a single end point of a request/response geogit instance which response to git
 * protocol
 * 
 * @author jhudson
 */
public class Remote extends AbstractRemote {

    private final String location;
    private char type_null = '\u0000';
    private final static int BUFFER_SIZE = 2048;
    private int read = 0;
    private char type = type_null;
    private int length = 0;
    private ObjectId objectId = null;
    private int onHold = 0;

    public Remote( String location ) throws NullPointerException {
        this.location = location;
    }

    @Override
    public Repository getRepository() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRepository( Repository repo ) {
        // TODO Auto-generated method stub
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub
    }

    @Override
    public IPayload requestFetchPayload( Map<String, String> branchHeads ) {

        Payload payload = null;

        StringBuffer branchBuffer = new StringBuffer();

        for( String branchName : branchHeads.keySet() ) {
            branchBuffer.append(branchName + ":" + branchHeads.get(branchName) + ",");
        }

        String branches = branchBuffer.toString();

        if (branches.length() > 0) {
            branches = branches.substring(branches.length() - 1);
        }

        DefaultHttpClient httpclient = new DefaultHttpClient();

        try {
            HttpGet httpget = new HttpGet(location + "?branches=" + branches);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            payload = parsePayload(entity.getContent(), response);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpclient.getConnectionManager().shutdown();
        }

        return payload;
    }

    /**
     * This is a custom protocol which is used to transport all of the COMMIT/TREE/BLOB objects to
     * this client this is the protocol: [{C/T/B}{00000000000000000000}{0000000000}{PAYLOAD}] first
     * byte is a single character : 'C' for a commit 'T' for a tree 'B' for a blob 2nd byte to the
     * 21st byte are the objects ID - 20 bytes 22nd byte to the 31st byte is the objects length - 10
     * bytes the rest is the payload
     * 
     * @param instream
     * @param response
     * @return
     * @throws IOException
     */
    private Payload parsePayload( InputStream instream, HttpResponse response ) throws IOException {
        final Payload payload = new Payload();
        try {
            ByteArrayBuffer payloadBuffer = new ByteArrayBuffer(0);

            int c;

            while( (c = instream.read()) != -1 ) {

                type = (char) c;

                while( payloadBuffer.length() < 20 ) {
                    int cc = instream.read();
                    payloadBuffer.append(cc);
                }

                objectId = extractObjectId(payloadBuffer.toByteArray());
                payloadBuffer = new ByteArrayBuffer(0);

                while( payloadBuffer.length() < 10 ) {
                    payloadBuffer.append(instream.read());
                }
                length = extractLength(payloadBuffer.toByteArray());
                payloadBuffer = new ByteArrayBuffer(0);

                while( payloadBuffer.length() < length ) {
                    payloadBuffer.append(instream.read());
                }

                if (type == 'C') {
                    RevCommit commit = extractCommit(objectId, payloadBuffer.toByteArray());
                    payload.addCommits(commit);
                    //System.out.println(commit);
                } else if (type == 'T') {
                    RevTree tree = extractTree(objectId, payloadBuffer.toByteArray());
                    payload.addTrees(tree);
                    //System.out.println(tree);
                } else if (type == 'B') {
                    RevBlob blob = extractBlob(objectId, payloadBuffer.toByteArray());
                    payload.addBlobs(blob);
                    //System.out.println(blob);
                }
                payloadBuffer = new ByteArrayBuffer(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            instream.close();
        }
        return payload;
    }

    private RevTree extractTree( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        HessianRevTreeReader tr = new HessianRevTreeReader(null);
        RevTree tree = tr.read(objectId, b);
        return tree;
    }

    private RevBlob extractBlob( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);
        BlobReader br = new BlobReader();
        RevBlob blob = br.read(objectId, b);
        return blob;
    }

    private RevCommit extractCommit( ObjectId objectId, byte[] buffer ) throws IOException {
        ByteArrayInputStream b = new ByteArrayInputStream(buffer);

        HessianCommitReader cr = new HessianCommitReader();
        RevCommit commit = cr.read(objectId, b);
        return commit;
    }

    private ObjectId extractObjectId( byte[] byteArray ) {
        return new ObjectId(byteArray);
    }

    private int extractLength( byte[] byteArray ) {
        String value = new String(byteArray).trim();
        return Integer.parseInt(value);
    }
}