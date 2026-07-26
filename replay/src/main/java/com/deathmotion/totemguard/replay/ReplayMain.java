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

package com.deathmotion.totemguard.replay;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ReplayMain {

    private ReplayMain() {
    }

    public static void main(String[] args) {
        Options options = Options.parse(args);
        if (options.help) {
            printUsage();
            return;
        }
        if (options.selfTest) {
            System.exit(SelfTest.run());
            return;
        }
        if (options.analyze) {
            System.exit(new RecordingAnalysis(options.recordings).run());
            return;
        }
        if (options.prologue > 0) {
            System.exit(new PrologueRehearsal(options.recordings, options.only, options.prologue).run());
            return;
        }
        ReplayRunner runner = new ReplayRunner(options);
        System.exit(runner.run());
    }

    private static void printUsage() {
        System.out.println("""
                TotemGuard replay runner
                
                  --recordings=<dir>   where the library lives (default ./recordings)
                  --only=<glob>        run one recording or one glob, e.g. legit/sneak-edge
                  --accept             rewrite the golden traces and flag logs from this run
                  --diff               report golden differences and change nothing
                  --trace=<name>       print TraceFormatter rows for one recording
                  --from=<tick>        first tick to print with --trace
                  --to=<tick>          last tick to print with --trace
                  --verify-determinism replay each recording twice and compare
                  --config=recorded    run the config the recording was captured with
                  --selftest           round-trip a hand-built recording through the whole path
                  --analyze            report where the bytes in each recording went
                  --prologue=<secs>    cut each recording there, rebuild the world from the mirror,
                                       and report how long the tail takes to agree with the whole
                """);
    }

    record Options(Path recordings, String only, boolean accept, boolean diff, String trace,
                   long from, long to, boolean verifyDeterminism, boolean recordedConfig,
                   boolean selfTest, boolean analyze, double prologue, boolean help) {
        static Options parse(String[] args) {
            Path recordings = Paths.get("recordings");
            String only = null;
            String trace = null;
            boolean accept = false;
            boolean diff = false;
            boolean determinism = false;
            boolean selfTest = false;
            boolean analyze = false;
            boolean recordedConfig = false;
            boolean help = false;
            long from = 0;
            long to = Long.MAX_VALUE;
            double prologue = 0.0;

            for (String arg : args) {
                if (arg.startsWith("--prologue=")) prologue = Double.parseDouble(value(arg));
                else if (arg.equals("--prologue")) prologue = 10.0;
                else if (arg.startsWith("--recordings=")) recordings = Paths.get(value(arg));
                else if (arg.startsWith("--only=")) only = value(arg);
                else if (arg.startsWith("--trace=")) trace = value(arg);
                else if (arg.startsWith("--from=")) from = Long.parseLong(value(arg));
                else if (arg.startsWith("--to=")) to = Long.parseLong(value(arg));
                else if (arg.equals("--accept")) accept = true;
                else if (arg.equals("--diff")) diff = true;
                else if (arg.equals("--verify-determinism")) determinism = true;
                else if (arg.equals("--selftest")) selfTest = true;
                else if (arg.equals("--analyze")) analyze = true;
                else if (arg.equals("--config=recorded")) recordedConfig = true;
                else help = true;
            }
            return new Options(recordings, only, accept, diff, trace, from, to, determinism,
                    recordedConfig, selfTest, analyze, prologue, help);
        }

        private static String value(String arg) {
            return arg.substring(arg.indexOf('=') + 1);
        }
    }
}
