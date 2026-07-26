## ADDED Requirements

### Requirement: Playable bundled game
Cinderhell SHALL include at least one redistributable single-player Freedoom IWAD and SHALL register it in the library without requiring a network connection or user import.

#### Scenario: First launch without imported content
- **WHEN** the user opens Cinderhell for the first time while offline
- **THEN** the launcher offers a playable Freedoom game

### Requirement: User-scoped document import
Cinderhell SHALL import user-selected `.wad`, `.pk3`, `.zip`, `.deh`, and `.bex` documents through Android's system document picker without requesting broad shared-storage access.

#### Scenario: Select a supported document
- **WHEN** the user selects a supported document from any available document provider
- **THEN** Cinderhell reads that document through its granted content URI and starts an import

#### Scenario: Inspect application permissions
- **WHEN** the installed application manifest is inspected
- **THEN** it does not request `MANAGE_EXTERNAL_STORAGE`

### Requirement: Durable app-owned content
Cinderhell SHALL stream each accepted document into app-owned storage, calculate its SHA-256 digest, and make the catalogue entry visible only after the copy and metadata transaction complete.

#### Scenario: Original document becomes unavailable
- **WHEN** a successfully imported source document is moved, renamed, deleted, or its URI grant is lost
- **THEN** Cinderhell can still launch its app-owned copy

#### Scenario: Import is interrupted
- **WHEN** copying or hashing fails before the import commits
- **THEN** Cinderhell reports the failure and leaves neither a catalogue entry nor a partial playable blob

### Requirement: Duplicate content detection
Cinderhell SHALL use the content digest as the stable identity of an imported document and SHALL avoid storing duplicate blobs.

#### Scenario: Import identical bytes twice
- **WHEN** the user imports a document whose digest already exists in the library
- **THEN** Cinderhell reuses the existing content record and informs the user that the content was already imported

### Requirement: Content-aware classification
Cinderhell SHALL inspect file signatures and, for WADs, the WAD header and lump directory rather than trusting the filename or MIME type. It SHALL classify supported commercial and Freedoom IWADs as games, and supported PWAD/PK3/ZIP/DEH/BEX documents as mods or patches.

#### Scenario: Renamed supported IWAD
- **WHEN** the user imports a supported IWAD whose filename is non-standard
- **THEN** Cinderhell identifies the game from its contents and known metadata where available

#### Scenario: Misleading extension
- **WHEN** a selected document has a supported extension but invalid or incompatible contents
- **THEN** Cinderhell rejects it with a user-facing reason and does not add it to the library

### Requirement: Persistent catalogue metadata
Cinderhell SHALL persist each content item's digest, original display name, internal path, byte size, content type, detected game identity, and import time.

#### Scenario: Restart after import
- **WHEN** the user restarts Cinderhell after a successful import
- **THEN** the imported item remains available with the same identity and metadata

### Requirement: Referentially safe removal
Cinderhell SHALL identify profiles that reference content before removing its catalogue record and blob, and SHALL require explicit confirmation before invalidating those profiles.

#### Scenario: Remove content used by profiles
- **WHEN** the user requests removal of content referenced by one or more profiles
- **THEN** Cinderhell names the affected profiles and does not remove the content until the user confirms
