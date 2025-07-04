## Planned possibly breaking changes for Version 6.x:

- Update required Java Version to 21
- Remove `throws IOException` from `AbstractConnectionBase.close()` (Issue #287)
- Remove all methods and classes marked as `@Deprecated(forRemoval = true)`
- Support usage of `Struct`s as return value (as alternative to `Tuple` with generics) (based on discussion in #285)
