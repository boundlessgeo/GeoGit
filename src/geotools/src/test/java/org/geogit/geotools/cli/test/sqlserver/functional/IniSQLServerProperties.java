/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.geotools.cli.test.sqlserver.functional;

import java.io.File;
import java.io.IOException;

import org.ini4j.Wini;

import com.google.common.base.Optional;

public class IniSQLServerProperties {

    private class SectionOptionPair {
        String section;

        String option;

        public SectionOptionPair(String key) {
            final int index = key.indexOf('.');

            if (index == -1) {
                throw new RuntimeException("Section.key invalid!");
            }

            section = key.substring(0, index);
            option = key.substring(index + 1);

            if (section.length() == 0 || option.length() == 0) {
                throw new RuntimeException("Section.key invalid!");
            }
        }
    }

    private File config() {
        File f = new File(System.getProperty("user.home"), ".geogit-sqlserver-tests.properties");
        try {
            if (!f.exists()) {
                f.createNewFile();

                // Populate the file with default values
                put("database.host", "localhost");
                put("database.port", "1433");
                put("database.schema", "dbo");
                put("database.database", "database");
                put("database.user", "sa");
                put("database.password", "sa");
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot write to the home directory.");
        }
        return f;
    }

    public <T> Optional<T> get(String key, Class<T> c) {
        if (key == null) {
            throw new RuntimeException("Section.key not provided to get.");
        }

        File configFile = config();

        final SectionOptionPair pair = new SectionOptionPair(key);
        try {
            final Wini ini = new Wini(configFile);
            T value = ini.get(pair.section, pair.option, c);

            if (value == null)
                return Optional.absent();

            if (c == String.class && ((String) value).length() == 0)
                return Optional.absent();

            return Optional.of(value);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Config location invalid.");
        }
    }

    public void put(String key, Object value) {
        final SectionOptionPair pair = new SectionOptionPair(key);

        File configFile = config();

        try {
            final Wini ini = new Wini(configFile);
            ini.put(pair.section, pair.option, value);
            ini.store();
        } catch (Exception e) {
            throw new RuntimeException("Config location invalid.");
        }
    }
}
