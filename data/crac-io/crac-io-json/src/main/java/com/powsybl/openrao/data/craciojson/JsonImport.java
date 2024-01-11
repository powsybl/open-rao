/*
 *  Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.openrao.data.craciojson;

import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracioapi.CracImporter;
import com.powsybl.openrao.data.craciojson.deserializers.CracDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.service.AutoService;
import org.apache.commons.io.FilenameUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static com.powsybl.commons.json.JsonUtil.createObjectMapper;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
@AutoService(CracImporter.class)
public class JsonImport implements CracImporter {
    private static final String JSON_EXTENSION = "json";

    @Override
    public Crac importCrac(InputStream inputStream, @NonNull CracFactory cracFactory) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Crac.class, new CracDeserializer(cracFactory));
            objectMapper.registerModule(module);
            return objectMapper.readValue(inputStream, Crac.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Crac importCrac(InputStream inputStream) {
        return importCrac(inputStream, CracFactory.findDefault());
    }

    @Override
    public boolean exists(String fileName, InputStream inputStream) {
        return validCracFile(fileName);
    }

    private boolean validCracFile(String fileName) {
        return FilenameUtils.getExtension(fileName).equals(JSON_EXTENSION);
    }
}
