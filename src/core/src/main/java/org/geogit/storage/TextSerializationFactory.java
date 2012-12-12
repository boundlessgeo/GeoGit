/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geogit.api.CommitBuilder;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevObject;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevPerson;
import org.geogit.api.RevTree;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * An {@link ObjectSerialisingFactory} for the {@link RevObject}s text format.
 * <p>
 * The following formats are used to interchange {@link RevObject} instances in a text format:
 * <H3>Commit:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * {@code "tree" + "\t" +  <tree id> + "\n"}
 * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"} 
 * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
 * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
 * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
 * {@code "message" + "\t" +  <message> + "\n"}
 * </pre>
 * 
 * <H3>Tree:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>Feature:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>FeatureType:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 * 
 * <H3>Tag:</H3>
 * 
 * <pre>
 * {@code "id" + "\t" +  <id> + "\n"}
 * ...
 * </pre>
 */
public class TextSerializationFactory implements ObjectSerialisingFactory {

    @Override
    public ObjectReader<RevCommit> createCommitReader() {
        return COMMIT_READER;
    }

    @Override
    public ObjectReader<RevTree> createRevTreeReader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectReader<RevFeature> createFeatureReader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectReader<RevFeature> createFeatureReader(Map<String, Serializable> hints) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectReader<RevFeatureType> createFeatureTypeReader() {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RevObject> ObjectWriter<T> createObjectWriter(TYPE type) {
        switch (type) {
        case COMMIT:
            return (ObjectWriter<T>) COMMIT_WRITER;
        default:
            throw new IllegalArgumentException("Unknown or unsupported object type: " + type);
        }
    }

    @Override
    public <T> ObjectReader<T> createObjectReader(TYPE type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectReader<RevObject> createObjectReader() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Abstract text writer that provides print methods on a {@link Writer} to consistently write
     * newlines as {@code \n} instead of using the platform's line separator as in
     * {@link PrintWriter}.
     */
    private static abstract class TextWriter<T extends RevObject> implements ObjectWriter<T> {

        @Override
        public void write(T object, OutputStream out) throws IOException {
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            print(object, writer);
            writer.flush();
        }

        protected abstract void print(T object, Writer w) throws IOException;

        protected void print(Writer w, CharSequence... s) throws IOException {
            if (s == null) {
                return;
            }
            for (CharSequence c : s) {
                w.write(String.valueOf(c));
            }
        }

        protected void println(Writer w, CharSequence... s) throws IOException {
            print(w, s);
            w.write('\n');
        }
    }

    /**
     * Commit writer.
     * <p>
     * Output format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "tree" + "\t" +  <tree id> + "\n"}
     * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"}
     * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
     * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
     * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
     * {@code "message" + "\t" +  <message> + "\n"}
     * </pre>
     * 
     */
    private static final TextWriter<RevCommit> COMMIT_WRITER = new TextWriter<RevCommit>() {

        @Override
        protected void print(RevCommit commit, Writer w) throws IOException {
            println(w, "id\t", commit.getId().toString());
            println(w, "tree\t", commit.getTreeId().toString());
            print(w, "parents\t");
            for (Iterator<ObjectId> it = commit.getParentIds().iterator(); it.hasNext();) {
                print(w, it.next().toString());
                if (it.hasNext()) {
                    print(w, " ");
                }
            }
            println(w);
            printPerson(w, "author", commit.getAuthor());
            printPerson(w, "committer", commit.getCommitter());
            println(w, "timestamp\t", String.valueOf(commit.getTimestamp()));
            println(w, "message\t", Optional.fromNullable(commit.getMessage()).or(""));
            w.flush();
        }

        private void printPerson(Writer w, String name, RevPerson person) throws IOException {
            print(w, name);
            print(w, "\t");
            print(w, Optional.fromNullable(person.getName()).or(""));
            print(w, " <");
            print(w, Optional.fromNullable(person.getEmail()).or(""));
            print(w, ">");
            println(w);
        }
    };

    private abstract static class TextReader<T extends RevObject> implements ObjectReader<T> {

        @Override
        public T read(ObjectId id, InputStream rawData) throws IllegalArgumentException {
            try {
                BufferedReader reader;
                reader = new BufferedReader(new InputStreamReader(rawData, "UTF-8"));
                T parsed = read(reader);
                Preconditions.checkState(parsed != null, "parsed to null");
                Preconditions.checkState(id.equals(parsed.getId()),
                        "Expected and parsed object ids don't match: %s %s", id, parsed.getId());
                return parsed;
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }

        protected abstract T read(BufferedReader reader) throws IOException;
    }

    /**
     * Commit reader.
     * <p>
     * Parses a commit of the format:
     * 
     * <pre>
     * {@code "id" + "\t" +  <id> + "\n"}
     * {@code "tree" + "\t" +  <tree id> + "\n"}
     * {@code "parents" + "\t" +  <parent id> [+ " " + <parent id>...]  + "\n"}
     * {@code "author" + "\t" +  <<author name> | ""> + " " + "<" + <<author email> | ""> + ">\n"}
     * {@code "committer" + "\t" +  <<committer name> | ""> + " " + "<" + <<committer email> | ""> + ">\n"}
     * {@code "timestamp" + "\t" +  <timestamp> + "\n"}
     * {@code "message" + "\t" +  <message> + "\n"}
     * </pre>
     * 
     */
    private static final TextReader<RevCommit> COMMIT_READER = new TextReader<RevCommit>() {

        @Override
        protected RevCommit read(BufferedReader reader) throws IOException {
            String id = parseLine(reader.readLine(), "id");
            String tree = parseLine(reader.readLine(), "tree");
            List<String> parents = Lists.newArrayList(Splitter.on(' ').omitEmptyStrings()
                    .split(parseLine(reader.readLine(), "parents")));
            RevPerson author = parsePerson(reader.readLine(), "author");
            RevPerson committer = parsePerson(reader.readLine(), "committer");
            String timeStamp = parseLine(reader.readLine(), "timestamp");
            String message = parseMessage(reader);

            CommitBuilder builder = new CommitBuilder();
            builder.setAuthor(author.getName());
            builder.setAuthorEmail(author.getEmail());
            builder.setCommitter(committer.getName());
            builder.setCommitterEmail(committer.getEmail());
            builder.setMessage(message);
            List<ObjectId> parentIds = Lists.newArrayList(Iterators.transform(parents.iterator(),
                    new Function<String, ObjectId>() {

                        @Override
                        public ObjectId apply(String input) {
                            ObjectId objectId = ObjectId.valueOf(input);
                            return objectId;
                        }
                    }));
            builder.setParentIds(parentIds);
            builder.setTreeId(ObjectId.valueOf(tree));
            builder.setTimestamp(Long.parseLong(timeStamp));
            RevCommit commit = builder.build();
            ObjectId oid = ObjectId.valueOf(id);
            // Preconditions.checkArgument(oid.equals(commit.getId()));
            return commit;
        }

        private RevPerson parsePerson(String line, String expectedHeaderName) throws IOException {
            String value = parseLine(line, expectedHeaderName);
            int emailStart = value.indexOf('<');
            String name = value.substring(0, emailStart).trim();
            String email = value.substring(emailStart + 1, value.length() - 1).trim();
            if (name.isEmpty()) {
                name = null;
            }
            if (email.isEmpty()) {
                email = null;
            }
            return new RevPerson(name, email);
        }

        private String parseMessage(BufferedReader reader) throws IOException {
            StringBuilder msg = new StringBuilder(parseLine(reader.readLine(), "message"));
            String extraLine;
            while ((extraLine = reader.readLine()) != null) {
                msg.append('\n').append(extraLine);
            }
            return msg.toString();
        }

        private String parseLine(String line, String expectedHeader) throws IOException {
            List<String> fields = Lists.newArrayList(Splitter.on('\t').split(line));
            Preconditions.checkArgument(fields.size() == 2, "Expected %s\\t<...>, got '%s'",
                    expectedHeader, line);
            Preconditions.checkArgument(expectedHeader.equals(fields.get(0)),
                    "Expected field %s, got '%s'", expectedHeader, fields.get(0));
            String value = fields.get(1);
            return value;
        }

    };
}
