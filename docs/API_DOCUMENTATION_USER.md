# Clerk Android SDK - User Management Documentation

## User Management

The `User` object holds all information for a single user and provides methods to manage their account.

### User Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique identifier for the user |
| `firstName` | `String?` | User's first name |
| `lastName` | `String?` | User's last name |
| `username` | `String?` | User's username |
| `emailAddresses` | `List<EmailAddress>` | All email addresses (including primary) |
| `phoneNumbers` | `List<PhoneNumber>` | All phone numbers (including primary) |
| `primaryEmailAddressId` | `String?` | ID of primary email address |
| `primaryPhoneNumberId` | `String?` | ID of primary phone number |
| `passwordEnabled` | `Boolean` | Whether user has a password |
| `twoFactorEnabled` | `Boolean` | Whether 2FA is enabled |
| `totpEnabled` | `Boolean` | Whether TOTP is enabled |
| `backupCodeEnabled` | `Boolean?` | Whether backup codes are enabled |
| `passkeys` | `List<Passkey>` | User's passkeys |
| `externalAccounts` | `List<ExternalAccount>` | Connected OAuth accounts |
| `publicMetadata` | `JsonObject?` | Public metadata (read-only from frontend) |
| `privateMetadata` | `JsonObject?` | Private metadata (backend only) |
| `unsafeMetadata` | `JsonObject?` | Unsafe metadata (read/write from frontend) |
| `imageUrl` | `String` | Profile image URL |
| `hasImage` | `Boolean` | Whether user has uploaded image |
| `createdAt` | `Long?` | Account creation timestamp |
| `updatedAt` | `Long` | Last update timestamp |
| `lastSignInAt` | `Long?` | Last sign-in timestamp |

### Getting User Information

#### `suspend fun User.get(): ClerkResult<User, ClerkErrorResponse>`

Retrieves the current user from the API.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.get()
    
    result?.onSuccess { user ->
        // Use updated user information
        displayUserProfile(user)
    }?.onFailure { error ->
        // Handle error
        showError("Failed to get user: ${error.message}")
    }
}
```

### Updating User Information

#### `suspend fun User.update(params: UpdateParams): ClerkResult<User, ClerkErrorResponse>`

Updates user profile information.

**Parameters:**
```kotlin
User.UpdateParams(
    firstName = "Jane",
    lastName = "Smith", 
    username = "janesmith",
    primaryEmailAddressId = "email_id",
    primaryPhoneNumberId = "phone_id",
    profileImageId = "image_id",
    publicMetadata = """{"role": "admin"}""",
    privateMetadata = """{"internal_id": "12345"}"""
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.update(
        User.UpdateParams(
            firstName = "Jane",
            lastName = "Smith",
            username = "janesmith"
        )
    )
    
    result?.onSuccess { user ->
        // Profile updated successfully
        displayUserProfile(user)
    }?.onFailure { error ->
        // Handle update error
        showError("Failed to update profile: ${error.message}")
    }
}
```

### Password Management

#### `suspend fun User.updatePassword(params: UpdatePasswordParams): ClerkResult<User, ClerkErrorResponse>`

Updates user's password.

**Parameters:**
```kotlin
User.UpdatePasswordParams(
    currentPassword = "oldPassword123",
    newPassword = "newSecurePassword456",
    signOutOfOtherSessions = true
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.updatePassword(
        User.UpdatePasswordParams(
            currentPassword = "currentPassword123",
            newPassword = "newSecurePassword456",
            signOutOfOtherSessions = true
        )
    )
    
    result?.onSuccess { user ->
        // Password updated successfully
        showMessage("Password updated successfully")
    }?.onFailure { error ->
        // Handle password update error
        showError("Failed to update password: ${error.message}")
    }
}
```

#### `suspend fun User.deletePassword(currentPassword: String): ClerkResult<User, ClerkErrorResponse>`

Deletes user's password (removes password authentication).

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.deletePassword("currentPassword123")
    
    result?.onSuccess { user ->
        // Password deleted successfully
        showMessage("Password authentication removed")
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to remove password: ${error.message}")
    }
}
```

### Profile Image Management

#### `suspend fun User.setProfileImage(file: File): ClerkResult<ImageResource, ClerkErrorResponse>`

Sets user's profile image.

**Example:**
```kotlin
private fun selectAndUploadImage() {
    // Use image picker to select file
    val imageFile = File(selectedImagePath)
    
    lifecycleScope.launch {
        val result = Clerk.user?.setProfileImage(imageFile)
        
        result?.onSuccess { imageResource ->
            // Image uploaded successfully
            val imageUrl = imageResource.publicUrl
            loadProfileImage(imageUrl)
        }?.onFailure { error ->
            // Handle upload error
            showError("Failed to upload image: ${error.message}")
        }
    }
}
```

#### `suspend fun User.deleteProfileImage(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Deletes user's profile image.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.deleteProfileImage()
    
    result?.onSuccess {
        // Image deleted successfully
        showDefaultAvatar()
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to delete image: ${error.message}")
    }
}
```

### Session Management

#### `suspend fun User.activeSessions(): ClerkResult<List<Session>, ClerkErrorResponse>`

Retrieves all active sessions for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.activeSessions()
    
    result?.onSuccess { sessions ->
        // Display active sessions
        displayActiveSessions(sessions)
    }?.onFailure { error ->
        // Handle error
        showError("Failed to get sessions: ${error.message}")
    }
}
```

#### `suspend fun User.allSessions(): ClerkResult<List<Session>, ClerkErrorResponse>`

Retrieves all sessions (active and inactive) for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.allSessions()
    
    result?.onSuccess { sessions ->
        // Display all sessions with status
        displayAllSessions(sessions)
    }?.onFailure { error ->
        // Handle error
        showError("Failed to get sessions: ${error.message}")
    }
}
```

### Email Address Management

#### `suspend fun User.emailAddresses(): ClerkResult<List<EmailAddress>, ClerkErrorResponse>`

Retrieves all email addresses for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.emailAddresses()
    
    result?.onSuccess { emailAddresses ->
        // Display email addresses
        displayEmailAddresses(emailAddresses)
    }?.onFailure { error ->
        // Handle error
        showError("Failed to get email addresses: ${error.message}")
    }
}
```

#### `suspend fun User.createEmailAddress(email: String): ClerkResult<EmailAddress, ClerkErrorResponse>`

Adds a new email address to the user's account.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createEmailAddress("newemail@example.com")
    
    result?.onSuccess { emailAddress ->
        // Email address created (unverified)
        // Prompt user to verify
        promptEmailVerification(emailAddress)
    }?.onFailure { error ->
        // Handle creation error
        showError("Failed to add email: ${error.message}")
    }
}
```

### Phone Number Management

#### `suspend fun User.phoneNumbers(): ClerkResult<List<PhoneNumber>, ClerkErrorResponse>`

Retrieves all phone numbers for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.phoneNumbers()
    
    result?.onSuccess { phoneNumbers ->
        // Display phone numbers
        displayPhoneNumbers(phoneNumbers)
    }?.onFailure { error ->
        // Handle error
        showError("Failed to get phone numbers: ${error.message}")
    }
}
```

#### `suspend fun User.createPhoneNumber(phoneNumber: String): ClerkResult<PhoneNumber, ClerkErrorResponse>`

Adds a new phone number to the user's account.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createPhoneNumber("+1234567890")
    
    result?.onSuccess { phoneNumber ->
        // Phone number created (unverified)
        // Prompt user to verify
        promptPhoneVerification(phoneNumber)
    }?.onFailure { error ->
        // Handle creation error
        showError("Failed to add phone number: ${error.message}")
    }
}
```

### Passkey Management

#### `suspend fun User.createPasskey(): ClerkResult<Passkey, ClerkErrorResponse>`

Creates a new passkey for the user.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createPasskey()
    
    result?.onSuccess { passkey ->
        // Passkey created successfully
        showMessage("Passkey created successfully")
        refreshPasskeysList()
    }?.onFailure { error ->
        // Handle creation error
        showError("Failed to create passkey: ${error.message}")
    }
}
```

### External Account Management

#### `suspend fun User.createExternalAccount(params: CreateExternalAccountParams): ClerkResult<ExternalAccount, ClerkErrorResponse>`

Connects an external OAuth account to the user.

**Parameters:**
```kotlin
User.CreateExternalAccountParams(
    provider = OAuthProvider.GOOGLE,
    redirectUrl = "https://yourapp.com/auth/callback",
    oidcPrompt = "consent",
    oidcLoginHint = "user@example.com"
)
```

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createExternalAccount(
        User.CreateExternalAccountParams(
            provider = OAuthProvider.GOOGLE,
            redirectUrl = "https://yourapp.com/auth/callback"
        )
    )
    
    result?.onSuccess { externalAccount ->
        // External account connection initiated
        // Handle redirect for OAuth flow
        val redirectUrl = externalAccount.verification?.externalVerificationRedirectUrl
        redirectUrl?.let { url ->
            openOAuthFlow(url)
        }
    }?.onFailure { error ->
        // Handle connection error
        showError("Failed to connect account: ${error.message}")
    }
}
```

### Multi-Factor Authentication (MFA)

#### TOTP (Authenticator App)

##### `suspend fun User.createTOTP(): ClerkResult<TOTPResource, ClerkErrorResponse>`

Creates TOTP configuration for authenticator apps.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createTOTP()
    
    result?.onSuccess { totpResource ->
        // Show QR code and secret for setup
        val qrCodeUrl = totpResource.qrCode
        val secret = totpResource.secret
        
        displayTOTPSetup(qrCodeUrl, secret)
    }?.onFailure { error ->
        // Handle TOTP creation error
        showError("Failed to create TOTP: ${error.message}")
    }
}
```

##### `suspend fun User.attemptTOTPVerification(code: String): ClerkResult<TOTPResource, ClerkErrorResponse>`

Verifies TOTP setup with authenticator app code.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.attemptTOTPVerification("123456")
    
    result?.onSuccess { totpResource ->
        // TOTP verified and enabled
        showMessage("Authenticator app setup complete")
        refreshMFASettings()
    }?.onFailure { error ->
        // Handle verification error
        showError("Invalid code: ${error.message}")
    }
}
```

##### `suspend fun User.deleteTOTP(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Disables TOTP authentication.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.deleteTOTP()
    
    result?.onSuccess {
        // TOTP disabled
        showMessage("Authenticator app disabled")
        refreshMFASettings()
    }?.onFailure { error ->
        // Handle deletion error
        showError("Failed to disable TOTP: ${error.message}")
    }
}
```

#### Backup Codes

##### `suspend fun User.createBackupCodes(): ClerkResult<BackupCodeResource, ClerkErrorResponse>`

Generates backup codes for account recovery.

**Example:**
```kotlin
lifecycleScope.launch {
    val result = Clerk.user?.createBackupCodes()
    
    result?.onSuccess { backupCodes ->
        // Display backup codes for user to save
        val codes = backupCodes.codes
        displayBackupCodes(codes)
    }?.onFailure { error ->
        // Handle backup code generation error
        showError("Failed to generate backup codes: ${error.message}")
    }
}
```

### Account Deletion

#### `suspend fun User.delete(): ClerkResult<DeletedObject, ClerkErrorResponse>`

Deletes the user's account permanently.

**Example:**
```kotlin
private fun confirmAccountDeletion() {
    AlertDialog.Builder(this)
        .setTitle("Delete Account")
        .setMessage("This action cannot be undone. Are you sure?")
        .setPositiveButton("Delete") { _, _ ->
            deleteAccount()
        }
        .setNegativeButton("Cancel", null)
        .show()
}

private fun deleteAccount() {
    lifecycleScope.launch {
        val result = Clerk.user?.delete()
        
        result?.onSuccess {
            // Account deleted successfully
            showMessage("Account deleted successfully")
            navigateToWelcomeScreen()
        }?.onFailure { error ->
            // Handle deletion error
            showError("Failed to delete account: ${error.message}")
        }
    }
}
```

### Complete User Profile Management Example

```kotlin
class UserProfileActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        displayUserInfo()
        setupClickListeners()
    }
    
    private fun displayUserInfo() {
        Clerk.user?.let { user ->
            // Display basic info
            findViewById<TextView>(R.id.tvName).text = "${user.firstName} ${user.lastName}"
            findViewById<TextView>(R.id.tvUsername).text = user.username
            findViewById<TextView>(R.id.tvEmail).text = user.emailAddresses.firstOrNull()?.emailAddress
            
            // Load profile image
            Glide.with(this)
                .load(user.imageUrl)
                .placeholder(R.drawable.default_avatar)
                .into(findViewById<ImageView>(R.id.ivProfileImage))
                
            // Display MFA status
            findViewById<TextView>(R.id.tvMFAStatus).text = if (user.twoFactorEnabled) "Enabled" else "Disabled"
        }
    }
    
    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
            showEditProfileDialog()
        }
        
        findViewById<Button>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }
        
        findViewById<Button>(R.id.btnManageEmails).setOnClickListener {
            showEmailManagement()
        }
        
        findViewById<Button>(R.id.btnSetupMFA).setOnClickListener {
            showMFASetup()
        }
    }
    
    private fun showEditProfileDialog() {
        val dialog = EditProfileDialog()
        dialog.onSave = { firstName, lastName, username ->
            updateProfile(firstName, lastName, username)
        }
        dialog.show(supportFragmentManager, "edit_profile")
    }
    
    private fun updateProfile(firstName: String, lastName: String, username: String) {
        lifecycleScope.launch {
            val result = Clerk.user?.update(
                User.UpdateParams(
                    firstName = firstName,
                    lastName = lastName,
                    username = username
                )
            )
            
            result?.onSuccess { user ->
                displayUserInfo()
                showMessage("Profile updated successfully")
            }?.onFailure { error ->
                showError("Failed to update profile: ${error.message}")
            }
        }
    }
    
    private fun showChangePasswordDialog() {
        val dialog = ChangePasswordDialog()
        dialog.onSave = { currentPassword, newPassword ->
            changePassword(currentPassword, newPassword)
        }
        dialog.show(supportFragmentManager, "change_password")
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            val result = Clerk.user?.updatePassword(
                User.UpdatePasswordParams(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    signOutOfOtherSessions = true
                )
            )
            
            result?.onSuccess {
                showMessage("Password changed successfully")
            }?.onFailure { error ->
                showError("Failed to change password: ${error.message}")
            }
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
```