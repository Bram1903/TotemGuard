/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.commands.commandapi.impl.database.util;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.messenger.impl.DatabaseMessageService;
import net.kyori.adventure.text.Component;

import java.util.Random;

public class ValidationHelper {

    private static ValidationHelper instance;
    private final DatabaseMessageService databaseMessageService;
    private final Random random;
    private Integer generatedCode;

    private ValidationHelper() {
        this.databaseMessageService = TotemGuard.getInstance().getMessengerService().getDatabaseMessageService();
        random = new Random();
    }

    public static ValidationHelper getInstance() {
        if (instance == null) {
            instance = new ValidationHelper();
        }
        return instance;
    }

    private int generateCode() {
        generatedCode = random.nextInt(900000) + 100000;
        return generatedCode;
    }

    public boolean validateCode(int code) {
        if (generatedCode == null) {
            return false;
        }

        return generatedCode == code;
    }

    public Component generateCodeMessage(ValidationType type) {
        return databaseMessageService.confirmationMessage(type, generateCode());
    }
}
