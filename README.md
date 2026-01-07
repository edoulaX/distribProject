**Source class summaries**

This section lists the Java classes under `src/main/java/cs451` and their responsibilities.

- `AckTracker.java`: Tracks ACKs, NACKs, and pending hosts per proposal. Determines when a proposal has reached a quorum (can decide) or when enough responses arrived to retry a proposal.
- `ConfigData.java`: Simple holder for lattice/configuration parameters (proposals per process, max elements, max distinct elements) and the list of proposal sets.
- `Constants.java`: Small constants used for CLI parsing and argument/index positions.
- `Host.java`: Immutable data class holding a process `id`, `address`, and `port`.
- `Main.java`: Program entrypoint — parses CLI arguments, loads the hosts and config files, constructs a `Process` instance and starts it.
- `Message.java`: Serializable message representation with types `PROPOSAL`, `ACK`, and `NACK`. Provides validation, factory methods, and custom `toBytes`/`fromBytes` serialization.
- `NetworkSimulator.java`: UDP sender wrapper using a `DatagramSocket` to send serialized `Message` objects to remote hosts (enforces a maximum packet size).
- `Process.java`: Core protocol implementation per process. Manages proposals, proposal numbers, decided flags, networking (listening/sending), message handlers for `PROPOSAL`/`ACK`/`NACK`, retry logic, and writing decisions to the output file.

If you want, I can replace the rest of the README with minimal build/run examples or add a concrete 3-process run script.

**Setup**

Follow these steps to prepare your environment and build the project (this makes the earlier `mvn -q -f pom.xml package` reference applicable):

- **Prerequisites:** Java JDK 11+ installed and `java` on your PATH; Apache Maven installed and `mvn` on your PATH. Verify with:

```bash
java -version
mvn -v
```

- **Optional (Windows):** ensure `JAVA_HOME` points to your JDK installation if needed.

- **Build the project:** from the repository root run:

```bash
mvn -q -f pom.xml package
```

This builds the project and places artifacts under `target/` (standard Maven layout). After this step you can run the `java -cp ... cs451.Main` commands shown below.

**Example: Local 3-process run**

Below is a minimal example to run three local processes on a single machine (PowerShell and Bash examples). The steps assume you've built the project with Maven (see earlier `mvn -q -f pom.xml package`).

1) Create a hosts file (example: `example/hosts.local`) with three entries listening on different ports:

```text
1 127.0.0.1 5001
2 127.0.0.1 5002
3 127.0.0.1 5003
```

2) Choose a config file from `example/configs` (for example `lattice-agreement-1.config`).

3) Open three terminals and run one process per terminal.

PowerShell (Windows) — example commands:

```powershell
cd path\to\distribProject
mvn -q -f pom.xml package
java -cp "target/*;target/dependency/*" cs451.Main --id 1 --hosts example/hosts.local --output proc01.output example/configs/lattice-agreement-1.config
java -cp "target/*;target/dependency/*" cs451.Main --id 2 --hosts example/hosts.local --output proc02.output example/configs/lattice-agreement-1.config
java -cp "target/*;target/dependency/*" cs451.Main --id 3 --hosts example/hosts.local --output proc03.output example/configs/lattice-agreement-1.config
```

Bash (Unix/macOS) — example commands:

```bash
cd /path/to/distribProject
mvn -q -f pom.xml package
java -cp "target/*:target/dependency/*" cs451.Main --id 1 --hosts example/hosts.local --output proc01.output example/configs/lattice-agreement-1.config
java -cp "target/*:target/dependency/*" cs451.Main --id 2 --hosts example/hosts.local --output proc02.output example/configs/lattice-agreement-1.config
java -cp "target/*:target/dependency/*" cs451.Main --id 3 --hosts example/hosts.local --output proc03.output example/configs/lattice-agreement-1.config
```

Notes:
- Ensure the `example/hosts.local` ports are free before starting. If needed, update ports.
- The `--output` parameter names are arbitrary files where each process writes decisions.
- If you prefer, you can run the included `run.sh` helper (on Unix) which wraps the correct arguments.

Want me to add a helper PowerShell script to start all three processes automatically? I can create `run-local.ps1` that launches them in separate windows and collects outputs.
