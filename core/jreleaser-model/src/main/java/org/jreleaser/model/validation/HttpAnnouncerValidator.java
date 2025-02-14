/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model.validation;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Active;
import org.jreleaser.model.Http;
import org.jreleaser.model.HttpAnnouncer;
import org.jreleaser.model.HttpAnnouncers;
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.util.Env;
import org.jreleaser.util.Errors;

import java.nio.file.Files;
import java.util.Map;

import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 1.3.0
 */
public abstract class HttpAnnouncerValidator extends Validator {
    private static final String DEFAULT_TPL = "src/jreleaser/templates/";

    public static void validateHttpAnnouncers(JReleaserContext context, JReleaserContext.Mode mode, HttpAnnouncers http, Errors errors) {
        context.getLogger().debug("announce.http");

        Map<String, HttpAnnouncer> ha = http.getHttpAnnouncers();

        boolean enabled = false;
        for (Map.Entry<String, HttpAnnouncer> e : ha.entrySet()) {
            e.getValue().setName(e.getKey());
            if (mode.validateConfig() || mode.validateAnnounce()) {
                if (validateHttpAnnouncer(context, http, e.getValue(), errors)) {
                    enabled = true;
                }
            }
        }

        if (enabled) {
            http.setActive(Active.ALWAYS);
        } else {
            http.setActive(Active.NEVER);
        }

        if (!http.resolveEnabled(context.getModel().getProject())) {
            context.getLogger().debug(RB.$("validation.disabled"));
        }
    }

    public static boolean validateHttpAnnouncer(JReleaserContext context, HttpAnnouncers http, HttpAnnouncer announcer, Errors errors) {
        context.getLogger().debug("announce.http." + announcer.getName());
        if (!announcer.resolveEnabled(context.getModel().getProject())) {
            context.getLogger().debug(RB.$("validation.disabled"));
            return false;
        }
        if (isBlank(announcer.getName())) {
            context.getLogger().debug(RB.$("validation.disabled"));
            announcer.disable();
            return false;
        }

        if (isBlank(announcer.getUrl())) {
            errors.configuration(RB.$("validation_must_not_be_blank", "http." + announcer.getName() + ".url"));
        }

        if (null == announcer.getMethod()) {
            announcer.setMethod(Http.Method.PUT);
        }

        switch (announcer.resolveAuthorization()) {
            case BEARER:
                announcer.setPassword(
                    checkProperty(context,
                        "HTTP_" + Env.toVar(announcer.getName()) + "_PASSWORD",
                        "http.password",
                        announcer.getPassword(),
                        errors,
                        context.isDryrun()));
                break;
            case BASIC:
                announcer.setUsername(
                    checkProperty(context,
                        "HTTP_" + Env.toVar(announcer.getName()) + "_USERNAME",
                        "http.username",
                        announcer.getUsername(),
                        errors,
                        context.isDryrun()));

                announcer.setPassword(
                    checkProperty(context,
                        "HTTP_" + Env.toVar(announcer.getName()) + "_PASSWORD",
                        "http.password",
                        announcer.getPassword(),
                        errors,
                        context.isDryrun()));
                break;
            case NONE:
                break;
        }

        String defaultPayloadTemplate = DEFAULT_TPL + "/http/" + announcer.getName() + ".tpl";
        if (isBlank(announcer.getPayload()) && isBlank(announcer.getPayloadTemplate())) {
            if (Files.exists(context.getBasedir().resolve(defaultPayloadTemplate))) {
                announcer.setPayloadTemplate(defaultPayloadTemplate);
            } else {
                announcer.setPayload(RB.$("default.release.message"));
            }
        }

        if (isNotBlank(announcer.getPayloadTemplate()) &&
            !defaultPayloadTemplate.equals(announcer.getPayloadTemplate().trim()) &&
            !Files.exists(context.getBasedir().resolve(announcer.getPayloadTemplate().trim()))) {
            errors.configuration(RB.$("validation_directory_not_exist",
                "http." + announcer.getName() + ".payloadTemplate", announcer.getPayloadTemplate()));
        }

        validateTimeout(announcer);

        return true;
    }
}