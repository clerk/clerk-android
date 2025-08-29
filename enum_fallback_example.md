# Enum Fallback Implementation

## How It Works

With `coerceInputValues = true` already enabled in the JSON configuration, any unknown enum values during deserialization will be treated as if the property was missing from the JSON, causing it to fall back to the default value specified in the data class.

## Example

### Before (would crash):
```json
{
  "instance_environment_type": "super_production", // unknown value
  "application_name": "My App"
}
```

### After (safe fallback):
```kotlin
@Serializable
data class DisplayConfig(
  @SerialName("instance_environment_type") 
  val instanceEnvironmentType: InstanceEnvironmentType = InstanceEnvironmentType.UNKNOWN,
  // other properties...
)

// When deserializing the JSON above:
// instanceEnvironmentType will be UNKNOWN instead of crashing
```

## Benefits of This Approach

1. **Much Simpler**: No custom serializers needed
2. **Leverages Built-in Feature**: Uses kotlinx.serialization's `coerceInputValues` 
3. **Consistent**: Same pattern across all enums
4. **Maintainable**: Less code to maintain
5. **Performance**: No reflection or custom logic overhead

## Implementation Summary

✅ **Added UNKNOWN values to key enums**:
- `OAuthProvider.UNKNOWN`
- `SSOResult.ResultType.UNKNOWN` 
- `SignIn.CredentialType.UNKNOWN`
- `UserOrganizationInvitation.Status.UNKNOWN`

✅ **Added default values to data class properties**:
- `instanceEnvironmentType: InstanceEnvironmentType = InstanceEnvironmentType.UNKNOWN`
- `preferredSignInStrategy: PreferredSignInStrategy = PreferredSignInStrategy.UNKNOWN`
- `deviceAttestationMode: DeviceAttestationMode = DeviceAttestationMode.UNKNOWN`
- `status: Status = Status.UNKNOWN` (for various status enums)

✅ **Removed complex custom serializers** in favor of simple defaults

This approach is much cleaner and leverages the framework's built-in capabilities rather than fighting against them.