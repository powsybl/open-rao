/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.cracioapi;

import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.google.common.base.Suppliers;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracImporters {

    private static final Supplier<List<CracImporter>> CRAC_IMPORTERS
        = Suppliers.memoize(() -> new ServiceLoaderCache<>(CracImporter.class).getServices())::get;

    private CracImporters() {
    }

    public static Crac importCrac(Path cracPath) {
        try (InputStream is = new FileInputStream(cracPath.toFile())) {
            return importCrac(cracPath.getFileName().toString(), is);
        } catch (FileNotFoundException e) {
            throw new OpenRaoException("File not found.");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.commons.io.IOUtils.copy(inputStream, baos);
        return baos.toByteArray();
    }

    public static Crac importCrac(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            CracImporter importer = findImporter(fileName, new ByteArrayInputStream(bytes));
            if (importer == null) {
                throw new OpenRaoException("No importer found for this file");
            }
            return importer.importCrac(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracImporter findImporter(String fileName, InputStream inputStream) {
        try {
            byte[] bytes = getBytesFromInputStream(inputStream);

            for (CracImporter importer : CRAC_IMPORTERS.get()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                if (importer.exists(fileName, bais)) {
                    return importer;
                }
            }
            return null;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
