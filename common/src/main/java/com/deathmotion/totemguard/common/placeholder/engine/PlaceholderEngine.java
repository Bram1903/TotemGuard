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

package com.deathmotion.totemguard.common.placeholder.engine;

import com.deathmotion.totemguard.api.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.api.placeholder.PlaceholderProvider;
import com.deathmotion.totemguard.common.database.util.DebugTemplate;
import com.deathmotion.totemguard.common.placeholder.holder.InternalPlaceholderHolder;
import com.deathmotion.totemguard.common.util.SortedMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PlaceholderEngine {

    private final ResolverCatalog<InternalContext> internalResolvers;
    private final ResolverCatalog<PlaceholderContext> apiResolvers;
    private final Set<String> registeredKeys;
    private final Set<String> registeredPatterns;

    public PlaceholderEngine(@NotNull List<InternalPlaceholderHolder> internalHolders,
                             @NotNull List<PlaceholderHolder> apiHolders) {
        this.internalResolvers = ResolverCatalog.compile(
                internalHolders,
                InternalPlaceholderHolder::resolve
        );
        this.apiResolvers = ResolverCatalog.compile(
                apiHolders,
                PlaceholderHolder::resolve
        );

        TreeSet<String> keys = new TreeSet<>(internalResolvers.registeredKeys());
        keys.addAll(apiResolvers.registeredKeys());
        this.registeredKeys = Collections.unmodifiableSet(keys);

        TreeSet<String> patterns = new TreeSet<>(internalResolvers.registeredPatterns());
        patterns.addAll(apiResolvers.registeredPatterns());
        this.registeredPatterns = Collections.unmodifiableSet(patterns);
    }

    public @NotNull String replace(@NotNull String message,
                                   @NotNull InternalContext internalCtx,
                                   @NotNull PlaceholderContext apiCtx) {

        if (message.isEmpty() || message.indexOf('%') == -1) {
            return message;
        }

        StringBuilder out = new StringBuilder(message.length());
        int cursor = 0;
        int length = message.length();

        while (cursor < length) {
            int start = message.indexOf('%', cursor);
            if (start < 0 || start == length - 1) {
                out.append(message, cursor, length);
                return out.toString();
            }

            int end = message.indexOf('%', start + 1);
            if (end < 0) {
                out.append(message, cursor, length);
                return out.toString();
            }

            String key = message.substring(start + 1, end);
            if (!isValidKey(key)) {
                out.append(message, cursor, end + 1);
                cursor = end + 1;
                continue;
            }

            out.append(message, cursor, start);

            String replacement = resolveExtra(key, apiCtx.extras());
            if (replacement == null) {
                replacement = internalResolvers.resolve(key, internalCtx);
            }
            if (replacement == null) {
                replacement = apiResolvers.resolve(key, apiCtx);
            }

            if (replacement != null) {
                out.append(replacement);
            } else {
                out.append(message, start, end + 1);
            }

            cursor = end + 1;
        }

        return out.toString();
    }

    public @NotNull Capture replaceCapturing(@NotNull String message,
                                             @NotNull InternalContext internalCtx,
                                             @NotNull PlaceholderContext apiCtx) {
        if (message.isEmpty() || message.indexOf('%') == -1) {
            return new Capture(message, message, null);
        }

        StringBuilder dispatched = new StringBuilder(message.length());
        StringBuilder template = new StringBuilder(message.length());
        StringBuilder argsJoined = new StringBuilder();
        int argIndex = 0;
        int cursor = 0;
        int length = message.length();

        while (cursor < length) {
            int start = message.indexOf('%', cursor);
            if (start < 0 || start == length - 1) {
                dispatched.append(message, cursor, length);
                template.append(message, cursor, length);
                break;
            }

            int end = message.indexOf('%', start + 1);
            if (end < 0) {
                dispatched.append(message, cursor, length);
                template.append(message, cursor, length);
                break;
            }

            String key = message.substring(start + 1, end);
            if (!isValidKey(key)) {
                dispatched.append(message, cursor, end + 1);
                template.append(message, cursor, end + 1);
                cursor = end + 1;
                continue;
            }

            dispatched.append(message, cursor, start);
            template.append(message, cursor, start);

            String replacement = resolveExtra(key, apiCtx.extras());
            if (replacement == null) {
                replacement = internalResolvers.resolve(key, internalCtx);
            }
            if (replacement == null) {
                replacement = apiResolvers.resolve(key, apiCtx);
            }

            if (replacement != null) {
                dispatched.append(replacement);
                template.append('{').append(argIndex).append('}');
                if (argIndex > 0) argsJoined.append(DebugTemplate.ARG_DELIMITER);
                argsJoined.append(replacement);
                argIndex++;
            } else {
                dispatched.append(message, start, end + 1);
                template.append(message, start, end + 1);
            }

            cursor = end + 1;
        }

        String args = argIndex == 0 ? null : argsJoined.toString();
        return new Capture(dispatched.toString(), template.toString(), args);
    }

    public @NotNull Set<String> registeredKeys() {
        return registeredKeys;
    }

    public @NotNull Set<String> registeredPatterns() {
        return registeredPatterns;
    }

    private @Nullable String resolveExtra(String key, Map<String, Object> extras) {
        Object value = extras.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean isValidKey(String key) {
        if (key.isEmpty()) {
            return false;
        }

        for (int i = 0; i < key.length(); i++) {
            if (Character.isWhitespace(key.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    @FunctionalInterface
    private interface ResolverInvoker<H, C> {
        @Nullable String resolve(H holder, @NotNull String key, @NotNull C context);
    }

    @FunctionalInterface
    private interface Resolver<C> {
        @Nullable String resolve(@NotNull String key, @NotNull C context);
    }

    public record Capture(@NotNull String dispatched, @NotNull String template, @Nullable String args) {
    }

    private static final class ResolverCatalog<C> {

        private static final ResolverEntry<?>[] EMPTY = new ResolverEntry[0];

        private final Map<String, ResolverEntry<C>[]> exactResolvers;
        private final ResolverEntry<C>[] dynamicResolvers;
        private final Set<String> registeredKeys;
        private final Set<String> registeredPatterns;

        private ResolverCatalog(
                Map<String, ResolverEntry<C>[]> exactResolvers,
                ResolverEntry<C>[] dynamicResolvers,
                Set<String> registeredKeys,
                Set<String> registeredPatterns
        ) {
            this.exactResolvers = exactResolvers;
            this.dynamicResolvers = dynamicResolvers;
            this.registeredKeys = registeredKeys;
            this.registeredPatterns = registeredPatterns;
        }

        private static <H, C> ResolverCatalog<C> compile(
                List<H> holders,
                ResolverInvoker<H, C> invoker
        ) {
            List<ResolverEntry<C>> dynamicResolvers = new ArrayList<>();
            Map<String, List<ResolverEntry<C>>> exactResolvers = new LinkedHashMap<>();
            TreeSet<String> registeredKeys = new TreeSet<>();
            TreeSet<String> registeredPatterns = new TreeSet<>();

            int sequence = 0;
            for (H holder : holders) {
                ResolverEntry<C> entry = new ResolverEntry<>(sequence++, (key, context) -> invoker.resolve(holder, key, context));

                Collection<String> keys = holder instanceof PlaceholderProvider provider ? provider.keys() : List.of();
                Collection<String> patterns = holder instanceof PlaceholderProvider provider ? provider.patterns() : List.of();

                for (String key : keys) {
                    exactResolvers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entry);
                    registeredKeys.add(key);
                }

                registeredPatterns.addAll(patterns);

                if (!(holder instanceof PlaceholderProvider) || !patterns.isEmpty() || keys.isEmpty()) {
                    dynamicResolvers.add(entry);
                }
            }

            Map<String, ResolverEntry<C>[]> dispatchByKey = new HashMap<>();
            for (Map.Entry<String, List<ResolverEntry<C>>> exactEntry : exactResolvers.entrySet()) {
                dispatchByKey.put(exactEntry.getKey(), merge(exactEntry.getValue(), dynamicResolvers));
            }

            return new ResolverCatalog<>(
                    Collections.unmodifiableMap(dispatchByKey),
                    toArray(dynamicResolvers),
                    Collections.unmodifiableSet(registeredKeys),
                    Collections.unmodifiableSet(registeredPatterns)
            );
        }

        private static <C> ResolverEntry<C>[] merge(
                List<ResolverEntry<C>> exactResolvers,
                List<ResolverEntry<C>> dynamicResolvers
        ) {
            if (dynamicResolvers.isEmpty()) return toArray(exactResolvers);
            if (exactResolvers.isEmpty()) return toArray(dynamicResolvers);

            List<ResolverEntry<C>> merged = new ArrayList<>(exactResolvers.size() + dynamicResolvers.size());
            SortedMerge.into(merged, exactResolvers, dynamicResolvers, ResolverEntry::sequence);
            return toArray(merged);
        }

        @SuppressWarnings("unchecked")
        private static <C> ResolverEntry<C>[] toArray(List<ResolverEntry<C>> resolvers) {
            return resolvers.isEmpty() ? (ResolverEntry<C>[]) EMPTY : resolvers.toArray(ResolverEntry[]::new);
        }

        private @Nullable String resolve(@NotNull String key, @NotNull C context) {
            ResolverEntry<C>[] resolvers = exactResolvers.get(key);
            if (resolvers == null) {
                resolvers = dynamicResolvers;
            }

            for (ResolverEntry<C> resolver : resolvers) {
                String replacement = resolver.resolve(key, context);
                if (replacement != null) {
                    return replacement;
                }
            }

            return null;
        }

        private Set<String> registeredKeys() {
            return registeredKeys;
        }

        private Set<String> registeredPatterns() {
            return registeredPatterns;
        }
    }

    private record ResolverEntry<C>(int sequence, Resolver<C> resolver) {

        private @Nullable String resolve(@NotNull String key, @NotNull C context) {
            return resolver.resolve(key, context);
        }
    }
}
