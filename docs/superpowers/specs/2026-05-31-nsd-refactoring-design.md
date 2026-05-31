# NSD Service Discovery Refactoring — Design Spec

## Overview

Refactor the Network Service Discovery (NSD) system across the Windows host (`YTShare.Host`) and Android app (`YTShare.Android`, Compose path only). Fix hostname transmission, eliminate duplicate host cards, add lifecycle management, and align NSD code with the MVVM + Koin architecture established by the history and chat redesigns.

## Goals

- Fix hostname always showing "Unknown" in the Android app
- Show one card per host, auto-selecting the best IP (same subnet as phone)
- Add activity lifecycle binding (discover on resume, stop on pause)
- Rewrite `NSDHelper` with `StateFlow`, thread safety, and Koin registration
- Introduce `SettingsViewModel` following existing MVVM patterns
- Clean up `MainActivityCompose` by removing manual helper initialization
- Remove dead code (`HostAdapter`, `DBHelper` references in Compose path)

## Non-Goals

- Migrating Volley to Ktor for LAN host communication
- Deleting legacy `MainActivity.kt` or Fragment files
- Migrating SharedPreferences to DataStore
- Modifying the legacy Fragment-based screens
- Custom/configurable device names on the host

---

## Architecture

### Host Side (`YTShare.Host/YTShare.Server/Program.cs`)

Add a TXT record containing the Windows computer name to the Bonjour service registration.

**Current:**
```csharp
bonjourService.Register(
    0, 0, "YTShareService", "_http._tcp.",
    null, null, 7296,
    null,           // TXT record — null, no hostname sent
    eventManager
);
```

**New:**
```csharp
TXTRecord txtRecord = new TXTRecord();
txtRecord.SetValue("hostname", Environment.MachineName);

bonjourService.Register(
    0, 0, "YTShareService", "_http._tcp.",
    null, null, 7296,
    txtRecord,      // TXT record with hostname
    eventManager
);
```

No other host-side changes.

---

### Android — `NSDHelper` Rewrite

**File:** `helpers/NSDHelper.kt`

Complete rewrite. Key changes:

**State management:**
- Private `MutableStateFlow<List<HostModel>>` for discovered hosts
- Public `val hosts: StateFlow<List<HostModel>>` exposed as read-only
- Thread-safe updates via `MutableStateFlow.update { }` 

**Lifecycle methods:**
- `startDiscovery()` — begins NSD discovery, stores the `DiscoveryListener` reference
- `stopDiscovery()` — calls `nsdManager.stopServiceDiscovery()`, clears the listener
- Guards against double-start/double-stop

**Service resolution:**
- Read `hostname` from TXT attributes: `resolvedService.attributes["hostname"]?.decodeToString() ?: resolvedService.serviceName`
- Fallback to `serviceName` if TXT record is missing (backwards compatibility)
- Collect all resolved IP addresses, pick the best one using subnet matching
- Deduplicate by hostname — if a host with the same name already exists, update its IP

**Best IP selection logic:**
1. Get the phone's current Wi-Fi IP via `WifiManager` or `ConnectivityManager`
2. Derive the /24 subnet (e.g., `192.168.1.x`)
3. From the host's resolved addresses, prefer the one on the same subnet
4. Fallback: first valid IPv4 address

**Service removal:**
- `onServiceLost` removes the host from the StateFlow by service name

**IPv4 validation:**
- Replace the broken regex (`\.?` makes dots optional) with a correct pattern:
  `^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$`

**Koin registration:**
- Registered as `single { NSDHelper(androidContext()) }` in `AppModules.kt`

---

### Android — `HostModel` Update

**File:** `models/HostModel.kt`

Change from `class` to `data class` for proper equality checks and deduplication:

```kotlin
data class HostModel(
    val address: String,
    val hostName: String,
    val port: Int
) {
    override fun toString(): String = "$address:$port"
}
```

Fields changed from `var` to `val` — immutable.

---

### Android — New `SettingsViewModel`

**File:** `ui/screens/SettingsViewModel.kt`

Follows the same pattern as `HistoryViewModel` and `FriendsViewModel`.

**State:**
- `hosts: StateFlow<List<HostModel>>` — collected from `NSDHelper.hosts`
- `isTrackingEnabled: StateFlow<Boolean>` — from SharedPreferences
- `selectedIp: StateFlow<String>` — currently saved IP

**Actions:**
- `selectHost(host: HostModel)` — saves IP to SharedPreferences, updates `selectedIp`
- `setTracking(enabled: Boolean)` — saves tracking preference

**DI:**
- Injected via `koinViewModel()` in SettingsScreen
- Constructor takes `NSDHelper` and `SharedPreferences` (both from Koin)

---

### Android — `SettingsScreen` Update

**File:** `ui/screens/SettingsScreen.kt`

Update to receive state from `SettingsViewModel` instead of raw props:

**Current signature:**
```kotlin
fun SettingsScreen(
    hosts: List<HostModel>,
    isTrackingEnabled: Boolean,
    onHostSelected: (HostModel) -> Unit,
    onTrackingChanged: (Boolean) -> Unit
)
```

**New signature:**
```kotlin
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel())
```

The composable collects StateFlows from the ViewModel internally. No functional UI changes — same card layout, same behavior.

---

### Android — `MainActivityCompose` Cleanup

**File:** `MainActivityCompose.kt`

**Remove:**
- `lateinit var nsd: NSDHelper` — inject from Koin instead
- `lateinit var db: DBHelper` — dead code in Compose path, remove entirely
- `lateinit var queue: RequestQueue` — register in Koin, inject where needed
- `initializeNSD()` method — replaced by lifecycle binding
- `var hosts by remember { mutableStateOf(nsd.addresses) }` — moved to SettingsViewModel
- SharedPreferences IP/tracking state management — moved to SettingsViewModel

**Add:**
- Inject `NSDHelper` from Koin
- Override `onResume()` → `nsd.startDiscovery()`
- Override `onPause()` → `nsd.stopDiscovery()`

**Auto-select first host:**
- Move to a `LaunchedEffect` that collects `nsd.hosts` and auto-selects the first host only if no IP is saved yet (replacing the broken synchronous check in `initializeNSD()`)

**Settings navigation:**
- Remove `hosts`, `isTracking`, and callbacks from the `composable("settings")` block
- SettingsScreen now manages its own state via SettingsViewModel

---

### Android — `AppModules.kt` Updates

**File:** `di/AppModules.kt`

Add to existing modules:

```kotlin
// In networkModule or a new helperModule
single { NSDHelper(androidContext()) }
single { Volley.newRequestQueue(androidContext()) }
single<SharedPreferences> { androidContext().getSharedPreferences("ytshare_prefs", Context.MODE_PRIVATE) }

// In viewModelModule
viewModel { SettingsViewModel(get(), get()) }
```

Note: SharedPreferences changes from `Activity.getPreferences()` (private to activity) to `Context.getSharedPreferences()` with a named file, so Koin can provide it without an activity reference. Existing keys are preserved.

---

### Android — Dead Code Removal

**Delete:**
- `adapters/HostAdapter.kt` — legacy RecyclerView adapter for Fragment-based SettingsFragment. References `MainActivity` directly. Fully replaced by Compose `HostCard` in `SettingsScreen.kt`.

**Remove references:**
- `DBHelper` instantiation and `db` property from `MainActivityCompose`
- `db` parameter from `HomeScreen` composable (unused)

---

## File Change Summary

| File | Action | Description |
|------|--------|-------------|
| `YTShare.Server/Program.cs` | Modify | Add TXT record with `Environment.MachineName` |
| `helpers/NSDHelper.kt` | Rewrite | StateFlow, lifecycle, best-IP selection, dedup, fixed regex |
| `models/HostModel.kt` | Modify | Change to `data class`, `var` → `val` |
| `ui/screens/SettingsViewModel.kt` | Create | New ViewModel for settings screen |
| `ui/screens/SettingsScreen.kt` | Modify | Use SettingsViewModel instead of prop drilling |
| `MainActivityCompose.kt` | Modify | Remove manual init, add lifecycle binding, inject from Koin |
| `di/AppModules.kt` | Modify | Register NSDHelper, RequestQueue, SharedPreferences, SettingsViewModel |
| `ui/screens/HomeScreen.kt` | Modify | Remove unused `db` parameter |
| `adapters/HostAdapter.kt` | Delete | Dead code, replaced by Compose SettingsScreen |

---

## Testing

- Verify hostname appears correctly on host cards (not "Unknown")
- Verify only one card per host machine (even with multiple network adapters)
- Verify the selected IP is on the same subnet as the phone
- Verify discovery restarts when returning to the app (resume) and stops on pause
- Verify new hosts appear in real-time on the Settings screen
- Verify hosts disappear when the host app is stopped
- Verify auto-selection works on first launch (no saved IP)
- Verify sharing a link to the selected host still works end-to-end
