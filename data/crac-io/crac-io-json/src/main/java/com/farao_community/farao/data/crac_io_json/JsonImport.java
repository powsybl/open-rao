/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.data.crac_io_json;

import com.farao_community.farao.data.crac_api.Crac;
import com.farao_community.farao.data.crac_impl.SimpleCrac;
import com.farao_community.farao.data.crac_io_api.CracImporter;
import com.farao_community.farao.data.crac_impl.json.deserializers.SimpleCracDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.auto.service.AutoService;

import java.io.*;

import org.apache.commons.io.FilenameUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * CRAC object import in json format
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class JsonImport implements CracImporter {
    private static final String JSON_EXTENSION = "json";
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonImport.class);

    @Override
    public Crac importCrac(InputStream inputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            objectMapper.registerModule(new Jdk8Module());
            SimpleModule module = new SimpleModule();
            module.addDeserializer(SimpleCrac.class, new SimpleCracDeserializer());
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, SimpleCrac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Crac importCrac(InputStream inputStream, DateTime timeStampFilter) {
        LOGGER.warn("Timestamp filtering is not implemented for json importer. The timestamp will be ignored.");
        return importCrac(inputStream);
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return validCracFile(fileName);
    }

    private boolean validCracFile(String fileName) {
        return FilenameUtils.getExtension(fileName).equals(JSON_EXTENSION);
    }
}
