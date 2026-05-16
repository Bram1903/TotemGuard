TotemGuard Loader -- local/ drop bucket
=======================================

Drop any TotemGuard plugin jar here and the loader will import it into its
version catalog on the next start (or on `/tgloader import`).

Imported jars become available to `/tgloader load <version>` exactly like
remote-downloaded builds. Multiple builds of the same version (e.g. several
3.0.0-SNAPSHOT iterations) are kept apart by their SHA-256 prefix, so testing
different commits side-by-side is fine.

Requirements for imported jars:
  - Must carry the TotemGuard jar integrity stamp (3.0.0-SNAPSHOT or newer).
  - Version is read from plugin.yml (Bukkit) or fabric.mod.json (Fabric).
  - Anything older than 3.0.0-SNAPSHOT is refused.

After a successful import, the original file is moved into local/.imported/
so it isn't processed twice. Delete files from there to forget them.

You can also set `source: LOCAL` in loader-config.yml to make the loader
resolve `LATEST` exclusively from jars in this directory -- handy for fully
air-gapped servers that still want the loader's hot-reload and integrity
features.
