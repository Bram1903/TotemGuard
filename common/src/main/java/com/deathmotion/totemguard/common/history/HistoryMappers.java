/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.common.history;

import com.deathmotion.totemguard.api.history.AlertEntry;
import com.deathmotion.totemguard.api.history.PunishmentEntry;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;

final class HistoryMappers {

    private HistoryMappers() {
    }

    static AlertEntry toAlertEntry(AlertRecord record) {
        return new AlertEntry(
                record.id(),
                record.checkName(),
                record.serverName(),
                record.violations(),
                record.debug(),
                record.keepalivePing(),
                record.transactionPing(),
                record.clientBrand(),
                record.clientVersion(),
                record.createdAt()
        );
    }

    static PunishmentEntry toPunishmentEntry(PunishmentRecord record) {
        return new PunishmentEntry(
                record.id(),
                record.checkName(),
                record.serverName(),
                record.type(),
                record.command(),
                record.debug(),
                record.createdAt()
        );
    }
}
