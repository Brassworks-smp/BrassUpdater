## Brass Updater
The Auto Updater for the Brassworks SMP by swzo

- Can be disabled with this JVM arg `-Dbrassupdater.skip=true`

### Configuration

The update check URL can be configured using JVM arguments:

* **Specify a Custom URL:** Use `-Dbrassupdater.url=<YOUR_URL>` to set a custom URL for the `pack.toml` file.
* **Use Development URL:** Use `-Dbrassupdater.dev=true` to switch the updater to use the development branch URL instead of the default master branch URL.
