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
import org.jreleaser.model.JReleaserContext;
import org.jreleaser.model.Teams;
import org.jreleaser.util.Errors;

import java.nio.file.Files;

import static org.jreleaser.model.Teams.TEAMS_WEBHOOK;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
public abstract class TeamsValidator extends Validator {
    private static final String DEFAULT_TEAMS_TPL = "src/jreleaser/templates/teams.tpl";

    public static void validateTeams(JReleaserContext context, Teams teams, Errors errors) {
        context.getLogger().debug("announce.teams");
        if (!teams.resolveEnabled(context.getModel().getProject())) {
            context.getLogger().debug(RB.$("validation.disabled"));
            return;
        }

        teams.setWebhook(
            checkProperty(context,
                TEAMS_WEBHOOK,
                "teams.webhook",
                teams.getWebhook(),
                errors,
                context.isDryrun()));

        if (isBlank(teams.getMessageTemplate())) {
            teams.setMessageTemplate(DEFAULT_TEAMS_TPL);
        }

        if (isNotBlank(teams.getMessageTemplate()) &&
            !Files.exists(context.getBasedir().resolve(teams.getMessageTemplate().trim()))) {
            errors.configuration(RB.$("validation_directory_not_exist", "teams.messageTemplate", teams.getMessageTemplate()));
        }

        validateTimeout(teams);
    }
}