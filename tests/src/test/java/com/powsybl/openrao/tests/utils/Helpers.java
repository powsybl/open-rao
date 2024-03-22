/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openrao.tests.utils;

import com.google.common.base.Suppliers;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.commons.OpenRaoException;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreators;
import com.powsybl.openrao.data.craccreation.creator.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.craccreation.creator.cim.craccreator.CimCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.csaprofile.craccreator.CsaProfileCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.cse.CseCracCreationContext;
import com.powsybl.openrao.data.craccreation.creator.fbconstraint.craccreator.FbConstraintCreationContext;
import com.powsybl.openrao.data.cracioapi.CracExporters;
import com.powsybl.openrao.data.cracioapi.CracImporters;
import com.powsybl.openrao.data.craciojson.JsonImport;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.openrao.data.nativecracioapi.NativeCracImporter;
import com.powsybl.openrao.data.nativecracioapi.NativeCracImporters;
import com.powsybl.openrao.data.raoresultapi.RaoResult;
import com.powsybl.openrao.data.raoresultjson.RaoResultImporter;
import com.powsybl.openrao.data.refprog.referenceprogram.ReferenceProgram;
import com.powsybl.openrao.data.refprog.refprogxmlimporter.RefProgImporter;
import com.powsybl.openrao.monitoring.anglemonitoring.AngleMonitoringResult;
import com.powsybl.openrao.monitoring.anglemonitoring.json.AngleMonitoringResultImporter;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.openrao.tests.steps.CommonTestData;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCimCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCsaProfileCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripCseCracCreationContext;
import com.powsybl.openrao.tests.utils.round_trip_crac.RoundTripFbConstraintCreationContext;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Properties;

public final class Helpers {
    private Helpers() {
        // must nor be used
    }

    public static Network importNetwork(File networkFile, boolean useRdfId) {
        Properties importParams = new Properties();
        if (useRdfId) {
            importParams.put("iidm.import.cgmes.source-for-iidm-id", "rdfID");
        }
        return Network.read(Paths.get(networkFile.toString()), LocalComputationManager.getDefault(), Suppliers.memoize(ImportConfig::load).get(), importParams);
    }

    public static Pair<Crac, CracCreationContext> importCrac(File cracFile, Network network, String timestamp, CracCreationParameters cracCreationParameters) {
        if (cracFile.getName().endsWith(".json")) {
            // for now, the only JSON format is the farao internal format
            return Pair.of(importCracFromInternalFormat(cracFile), null);
        } else {
            CracCreationContext ccc = importCracFromNativeCrac(cracFile, network, timestamp, cracCreationParameters);
            return Pair.of(ccc.getCrac(), ccc);
        }
    }

    public static Crac importCracFromInternalFormat(File cracFile) {
        try {
            return roundTripOnCrac(new JsonImport().importCrac(new FileInputStream(cracFile)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static CracCreationContext importCracFromNativeCrac(File cracFile, Network network, String timestamp, CracCreationParameters cracCreationParameters) {
        byte[] cracBytes = null;
        try (InputStream cracInputStream = new BufferedInputStream(new FileInputStream(cracFile))) {
            cracBytes = getBytesFromInputStream(cracInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OpenRaoException("Could not load CRAC file", e);
        }
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);

        NativeCracImporter<?> nativeCracImporter = NativeCracImporters.findImporter(cracFile.getName(), new ByteArrayInputStream(cracBytes));
        NativeCrac nativeCrac = nativeCracImporter.importNativeCrac(new ByteArrayInputStream(cracBytes));
        CracCreationContext cracCreationContext = CracCreators.createCrac(nativeCrac, network, offsetDateTime, cracCreationParameters);
        // round-trip CRAC json export/import to test it implicitly
        return roundTripOnCracCreationContext(cracCreationContext);
    }

    public static String getCracFormat(File cracFile) {
        if (cracFile.getName().endsWith(".json")) {
            return "JSON";
        }
        byte[] cracBytes = null;
        try (InputStream cracInputStream = new BufferedInputStream(new FileInputStream(cracFile))) {
            cracBytes = getBytesFromInputStream(cracInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new OpenRaoException("Could not load CRAC file", e);
        }
        return NativeCracImporters.findImporter(cracFile.getName(), new ByteArrayInputStream(cracBytes)).getFormat();
    }

    private static CracCreationContext roundTripOnCracCreationContext(CracCreationContext cracCreationContext) {
        Crac crac = roundTripOnCrac(cracCreationContext.getCrac());
        if (cracCreationContext instanceof FbConstraintCreationContext) {
            return new RoundTripFbConstraintCreationContext((FbConstraintCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CseCracCreationContext) {
            return new RoundTripCseCracCreationContext((CseCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CimCracCreationContext) {
            return new RoundTripCimCracCreationContext((CimCracCreationContext) cracCreationContext, crac);
        } else if (cracCreationContext instanceof CsaProfileCracCreationContext) {
            return new RoundTripCsaProfileCracCreationContext((CsaProfileCracCreationContext) cracCreationContext, crac);
        } else {
            throw new NotImplementedException(String.format("%s type is not supported", cracCreationContext.getClass().getName()));
        }
    }

    private static Crac roundTripOnCrac(Crac crac) {
        // export Crac
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CracExporters.exportCrac(crac, "Json", outputStream);

        // import Crac
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return CracImporters.importCrac("crac.json", inputStream);
    }

    public static ZonalData<SensitivityVariableSet> importUcteGlskFile(File glskFile, String timestamp, Network network) throws IOException {
        InputStream inputStream = new FileInputStream(glskFile);
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(inputStream);

        Instant instant;
        if (timestamp == null) {
            instant = getStartInstantOfUcteGlsk(ucteGlskDocument);
        } else {
            instant = getOffsetDateTimeFromBrusselsTimestamp(timestamp).toInstant();
        }

        return ucteGlskDocument.getZonalGlsks(network, instant);
    }

    public static CimGlskDocument importCimGlskFile(File glskFile) throws IOException {
        InputStream inputStream = new FileInputStream(glskFile);
        return CimGlskDocument.importGlsk(inputStream);
    }

    private static Instant getStartInstantOfUcteGlsk(UcteGlskDocument ucteGlskDocument) {
        return ucteGlskDocument.getGSKTimeInterval().getStart();
    }

    public static OffsetDateTime getOffsetDateTimeFromBrusselsTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.of(LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), ZoneId.of("Europe/Brussels"))
            .toOffsetDateTime();
    }

    public static ReferenceProgram importRefProg(File refProgFile, String timestamp) throws IOException {
        if (timestamp == null) {
            throw new OpenRaoException("A timestamp should be provided in order to import the refProg file.");
        }

        InputStream refProgInputStream = new FileInputStream(refProgFile);
        OffsetDateTime offsetDateTime = getOffsetDateTimeFromBrusselsTimestamp(timestamp);
        return RefProgImporter.importRefProg(refProgInputStream, offsetDateTime);
    }

    public static RaoResult importRaoResult(File raoResultFile) throws IOException {
        InputStream inputStream = new FileInputStream(raoResultFile);
        RaoResult raoResult = new RaoResultImporter().importRaoResult(inputStream, CommonTestData.getCrac());
        inputStream.close();
        return raoResult;
    }

    public static AngleMonitoringResult importAngleMonitoringResult(File angleMonitoringResultFile) throws IOException {
        InputStream inputStream = new FileInputStream(angleMonitoringResultFile);
        AngleMonitoringResult angleMonitoringResult = new AngleMonitoringResultImporter().importAngleMonitoringResult(inputStream, CommonTestData.getCrac());
        inputStream.close();
        return angleMonitoringResult;
    }

    public static File getFile(String path) {
        Objects.requireNonNull(path);
        try {
            return new File(path);
        } catch (Exception e) {
            throw new OpenRaoException(String.format("Could not load file %s", path));
        }
    }

    private static byte[] getBytesFromInputStream(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            org.apache.commons.io.IOUtils.copy(inputStream, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new UncheckedIOException(e);
        }
    }
}
