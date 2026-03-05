# Quick Start: Core SDK Integration

This guide provides a minimal app-integration path for `geo-event-trigger-core-sdk`.

## 1) Prepare artifact

```bash
scripts/build_artifact.sh 0.1.0
```

Output:
- `dist/geo-event-trigger-core-sdk-0.1.0.jar`
- `dist/geo-event-trigger-core-sdk-0.1.0.pom`

## 2) Add dependency

### Option A: Local JAR (fastest)
Use classpath-based integration with the generated JAR.

### Option B: Maven/Gradle coordinates (baseline)
- Group: `com.geo.sdk`
- Artifact: `geo-event-trigger-core-sdk`
- Version: `0.1.0`

## 3) Minimal wrapper usage

See sample wrapper:
- `examples/minimal-wrapper/src/main/java/com/geo/sdk/example/MinimalWrapperExample.java`

Core pattern:
1. Build `InputEvent` from app context.
2. Build `Context` + `Budget`.
3. Execute `PipelineEngine.run(...)`.
4. Apply feedback via `InMemoryUpdater.apply(...)` when available.

## 4) Verify consumer integration

```bash
scripts/verify_consumer.sh 0.1.0
```

Expected output includes:
- `Decision: ...`
- `Consumer verification: ok`

## 5) Security defaults

- No external transfer primitives in core runtime.
- No raw location/context logging in core runtime.
- Integration sample avoids network and sensitive payload logging by default.
