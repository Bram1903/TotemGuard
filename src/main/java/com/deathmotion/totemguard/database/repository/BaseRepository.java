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

package com.deathmotion.totemguard.database.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import org.jetbrains.annotations.Blocking;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Blocking
public abstract class BaseRepository<T, ID> {
    protected final Dao<T, ID> dao;

    protected BaseRepository(ConnectionSource connectionSource, Class<T> clazz) {
        try {
            this.dao = DaoManager.createDao(connectionSource, clazz);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create DAO for " + clazz.getSimpleName(), e);
        }
    }

    protected QueryBuilder<T, ID> createQueryBuilder() {
        return dao.queryBuilder();
    }

    protected List<T> executeQuery(QueryBuilder<T, ID> queryBuilder) {
        try {
            return queryBuilder.query();
        } catch (SQLException e) {
            throw new RuntimeException("Error executing query", e);
        }
    }

    public Optional<T> findById(ID id) {
        return execute(() -> Optional.ofNullable(dao.queryForId(id)));
    }

    public List<T> findAll() {
        return execute(dao::queryForAll);
    }

    public boolean save(T entity) {
        return execute(() -> dao.createOrUpdate(entity).getNumLinesChanged() > 0);
    }

    public boolean delete(T entity) {
        return execute(() -> dao.delete(entity) > 0);
    }

    public boolean deleteById(ID id) {
        return execute(() -> dao.deleteById(id) > 0);
    }

    public long count() {
        return execute(dao::countOf);
    }

    /**
     * Utility method to centralize exception handling.
     */
    protected <R> R execute(SqlExecutor<R> executor) {
        try {
            return executor.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed", e);
        }
    }

    @FunctionalInterface
    protected interface SqlExecutor<R> {
        R execute() throws SQLException;
    }
}
