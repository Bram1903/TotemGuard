package com.deathmotion.totemguard.models.impl;

import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;

import java.util.List;

public record ProfileData(String clientBrand, List<DatabaseAlert> databaseAlertList,
                          List<DatabasePunishment> databasePunishmentList) {

}
