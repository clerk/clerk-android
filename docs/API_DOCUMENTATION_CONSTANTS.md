# Clerk Android SDK - Constants & Configuration

## Constants

The `Constants` object provides consolidated constants used throughout the Clerk SDK.

### Authentication Strategies

```kotlin
object Strategy {
    const val PHONE_CODE = "phone_code"
    const val EMAIL_CODE = "email_code"
    const val TOTP = "totp"
    const val BACKUP_CODE = "backup_code"
    const val PASSWORD = "password"
    const val PASSKEY = "passkey"
    const val RESET_PASSWORD_EMAIL_CODE = "reset_password_email_code"
    const val RESET_PASSWORD_PHONE_CODE = "reset_password_phone_code"
    const val TICKET = "ticket"
    const val TRANSFER = "transfer"
    const val ENTERPRISE_SSO = "enterprise_sso"
    const val STRATEGY_KEY = "strategy"
}
```

### HTTP and API Constants

```kotlin
object Http {
    const val NO_CONTENT = 204
    const val RESET_CONTENT = 205
    const val CURRENT_API_VERSION = "2024-10-01"
    const val CURRENT_SDK_VERSION = "0.1.0"
    const val IS_MOBILE_HEADER_VALUE = "1"
    const val AUTHORIZATION_HEADER = "Authorization"
    const val IS_NATIVE_QUERY_PARAM = "_is_native"
}
```

### Configuration Constants

```kotlin
object Config {
    const val REFRESH_TOKEN_INTERVAL = 5
    const val API_TIMEOUT_SECONDS = 30L
    const val TIMEOUT_MULTIPLIER = 1000
    const val BACKOFF_BASE_DELAY_SECONDS = 5L
    const val MAX_ATTESTATION_RETRIES = 3
    const val EXPONENTIAL_BACKOFF_SHIFT = 1
    const val DEFAULT_EXPIRATION_BUFFER = 1000L
    const val COMPRESSION_PERCENTAGE = 75
}
```

### URL and Key Prefixes

```kotlin
object Prefixes {
    const val URL_SSL_PREFIX = "https://"
    const val TOKEN_PREFIX_LIVE = "pk_live_"
    const val TOKEN_PREFIX_TEST = "pk_test_"
}
```

### Storage Constants

```kotlin
object Storage {
    const val CLERK_PREFERENCES_FILE_NAME = "clerk_preferences"
    const val KEY_AUTHORIZATION_STARTED = "authStarted"
}
```

### Device Attestation Constants

```kotlin
object Attestation {
    const val HASH_CONSTANT = 0xff
    const val PREPARATION_TIMEOUT_MS = 30_000L // 30 seconds
    const val ATTESTATION_TIMEOUT_MS = 15_000L // 15 seconds
    const val HASH_CACHE_MAX_SIZE = 100
    const val SHA256_HEX_LENGTH = 64
}
```

### Passkey Constants

```kotlin
object Passkey {
    const val STRATEGY_KEY = "strategy"
    const val PASSKEY_STRATEGY_VALUE = "passkey"
}
```

---

## Complete Integration Examples

### Basic Authentication Flow

```kotlin
class AuthenticationActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        
        // Check if user is already signed in
        if (Clerk.isSignedIn) {
            navigateToMainActivity()
            return
        }
        
        setupAuthenticationUI()
    }
    
    private fun setupAuthenticationUI() {
        findViewById<Button>(R.id.btnSignInEmail).setOnClickListener {
            signInWithEmail()
        }
        
        findViewById<Button>(R.id.btnSignInGoogle).setOnClickListener {
            signInWithGoogle()
        }
        
        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            navigateToSignUp()
        }
    }
    
    private fun signInWithEmail() {
        val email = findViewById<EditText>(R.id.etEmail).text.toString()
        val password = findViewById<EditText>(R.id.etPassword).text.toString()
        
        lifecycleScope.launch {
            // Create sign-in with password
            val createResult = SignIn.create(
                SignIn.CreateParams.Strategy.Password(email, password)
            )
            
            createResult.onSuccess { signIn ->
                when (signIn.status) {
                    SignIn.Status.COMPLETE -> {
                        // Sign-in complete
                        navigateToMainActivity()
                    }
                    SignIn.Status.NEEDS_SECOND_FACTOR -> {
                        // Handle MFA
                        handleMFA(signIn)
                    }
                    else -> {
                        showError("Unexpected sign-in status: ${signIn.status}")
                    }
                }
            }.onFailure { error ->
                showError("Sign-in failed: ${error.message}")
            }
        }
    }
    
    private fun signInWithGoogle() {
        lifecycleScope.launch {
            val result = SignIn.authenticateWithRedirect(
                SignIn.AuthenticateWithRedirectParams.OAuth(
                    provider = OAuthProvider.GOOGLE
                )
            )
            
            result.onSuccess { oauthResult ->
                // OAuth flow initiated - handle redirect
                handleOAuthRedirect(oauthResult)
            }.onFailure { error ->
                showError("Google sign-in failed: ${error.message}")
            }
        }
    }
    
    private suspend fun handleMFA(signIn: SignIn) {
        // Prepare second factor (SMS or TOTP)
        val prepareResult = signIn.prepareSecondFactor()
        
        prepareResult?.onSuccess { signIn ->
            // Show MFA code input dialog
            showMFADialog { code ->
                attemptMFA(signIn, code)
            }
        }?.onFailure { error ->
            showError("Failed to prepare MFA: ${error.message}")
        }
    }
    
    private suspend fun attemptMFA(signIn: SignIn, code: String) {
        val result = signIn.attemptSecondFactor(
            SignIn.AttemptSecondFactorParams.TOTP(code)
        )
        
        result?.onSuccess { signIn ->
            if (signIn.status == SignIn.Status.COMPLETE) {
                navigateToMainActivity()
            }
        }?.onFailure { error ->
            showError("Invalid MFA code: ${error.message}")
        }
    }
}
```

### Complete User Profile Management

```kotlin
class UserProfileFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        displayUserProfile()
        setupProfileActions()
    }
    
    private fun displayUserProfile() {
        Clerk.user?.let { user ->
            view?.apply {
                findViewById<TextView>(R.id.tvUserName).text = "${user.firstName} ${user.lastName}"
                findViewById<TextView>(R.id.tvEmail).text = user.emailAddresses.firstOrNull()?.emailAddress
                findViewById<TextView>(R.id.tvPhone).text = user.phoneNumbers.firstOrNull()?.phoneNumber
                
                // Load profile image
                Glide.with(this@UserProfileFragment)
                    .load(user.imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(findViewById<ImageView>(R.id.ivProfileImage))
                
                // Display security settings
                findViewById<TextView>(R.id.tvMFAStatus).text = 
                    if (user.twoFactorEnabled) "Enabled" else "Disabled"
                findViewById<TextView>(R.id.tvPasskeyCount).text = 
                    "${user.passkeys.size} passkeys"
            }
        }
    }
    
    private fun setupProfileActions() {
        view?.apply {
            findViewById<Button>(R.id.btnEditProfile).setOnClickListener {
                showEditProfileDialog()
            }
            
            findViewById<Button>(R.id.btnManageEmails).setOnClickListener {
                showEmailManagement()
            }
            
            findViewById<Button>(R.id.btnManagePhones).setOnClickListener {
                showPhoneManagement()
            }
            
            findViewById<Button>(R.id.btnSecuritySettings).setOnClickListener {
                showSecuritySettings()
            }
            
            findViewById<Button>(R.id.btnSignOut).setOnClickListener {
                signOut()
            }
        }
    }
    
    private fun showEmailManagement() {
        lifecycleScope.launch {
            val result = Clerk.user?.emailAddresses()
            
            result?.onSuccess { emailAddresses ->
                val dialog = EmailManagementDialog(emailAddresses)
                dialog.onAddEmail = { email -> addEmailAddress(email) }
                dialog.onDeleteEmail = { emailAddress -> deleteEmailAddress(emailAddress) }
                dialog.show(parentFragmentManager, "email_management")
            }?.onFailure { error ->
                showError("Failed to load emails: ${error.message}")
            }
        }
    }
    
    private fun addEmailAddress(email: String) {
        lifecycleScope.launch {
            val result = Clerk.user?.createEmailAddress(email)
            
            result?.onSuccess { emailAddress ->
                // Email added, now verify it
                prepareEmailVerification(emailAddress)
            }?.onFailure { error ->
                showError("Failed to add email: ${error.message}")
            }
        }
    }
    
    private fun prepareEmailVerification(emailAddress: EmailAddress) {
        lifecycleScope.launch {
            val result = emailAddress.prepareVerification(
                EmailAddress.PrepareVerificationParams.EmailCode()
            )
            
            result?.onSuccess {
                showVerificationDialog { code ->
                    attemptEmailVerification(emailAddress, code)
                }
            }?.onFailure { error ->
                showError("Failed to send verification: ${error.message}")
            }
        }
    }
    
    private fun attemptEmailVerification(emailAddress: EmailAddress, code: String) {
        lifecycleScope.launch {
            val result = emailAddress.attemptVerification(code)
            
            result?.onSuccess {
                showMessage("Email verified successfully")
                displayUserProfile() // Refresh display
            }?.onFailure { error ->
                showError("Verification failed: ${error.message}")
            }
        }
    }
    
    private fun showSecuritySettings() {
        val dialog = SecuritySettingsDialog()
        dialog.onSetupMFA = { setupMFA() }
        dialog.onCreatePasskey = { createPasskey() }
        dialog.onChangePassword = { showChangePasswordDialog() }
        dialog.show(parentFragmentManager, "security_settings")
    }
    
    private fun setupMFA() {
        lifecycleScope.launch {
            val result = Clerk.user?.createTOTP()
            
            result?.onSuccess { totpResource ->
                showTOTPSetupDialog(totpResource) { code ->
                    verifyTOTPSetup(code)
                }
            }?.onFailure { error ->
                showError("Failed to setup MFA: ${error.message}")
            }
        }
    }
    
    private fun verifyTOTPSetup(code: String) {
        lifecycleScope.launch {
            val result = Clerk.user?.attemptTOTPVerification(code)
            
            result?.onSuccess {
                showMessage("MFA setup complete")
                displayUserProfile() // Refresh to show MFA enabled
            }?.onFailure { error ->
                showError("Invalid code: ${error.message}")
            }
        }
    }
    
    private fun createPasskey() {
        lifecycleScope.launch {
            val result = Clerk.user?.createPasskey()
            
            result?.onSuccess { passkey ->
                showMessage("Passkey created successfully")
                displayUserProfile() // Refresh passkey count
            }?.onFailure { error ->
                showError("Failed to create passkey: ${error.message}")
            }
        }
    }
    
    private fun signOut() {
        lifecycleScope.launch {
            val result = Clerk.signOut()
            
            result.onSuccess {
                // Navigate to sign-in screen
                findNavController().navigate(R.id.action_profile_to_signin)
            }.onFailure { error ->
                showError("Sign out failed: ${error.message}")
            }
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}
```

### Advanced Session Management

```kotlin
class SessionManagerActivity : AppCompatActivity() {
    
    private val sessionAdapter = SessionAdapter()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_manager)
        
        setupRecyclerView()
        loadSessions()
    }
    
    private fun setupRecyclerView() {
        findViewById<RecyclerView>(R.id.rvSessions).apply {
            adapter = sessionAdapter
            layoutManager = LinearLayoutManager(this@SessionManagerActivity)
        }
        
        sessionAdapter.onRevokeSession = { session ->
            revokeSession(session)
        }
    }
    
    private fun loadSessions() {
        lifecycleScope.launch {
            val result = Clerk.user?.allSessions()
            
            result?.onSuccess { sessions ->
                sessionAdapter.updateSessions(sessions)
            }?.onFailure { error ->
                showError("Failed to load sessions: ${error.message}")
            }
        }
    }
    
    private fun revokeSession(session: Session) {
        lifecycleScope.launch {
            val result = session.revoke()
            
            result?.onSuccess {
                showMessage("Session revoked")
                loadSessions() // Refresh list
            }?.onFailure { error ->
                showError("Failed to revoke session: ${error.message}")
            }
        }
    }
}

class SessionAdapter : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {
    
    private var sessions = listOf<Session>()
    var onRevokeSession: ((Session) -> Unit)? = null
    
    fun updateSessions(newSessions: List<Session>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }
    
    override fun getItemCount() = sessions.size
    
    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        fun bind(session: Session) {
            itemView.apply {
                findViewById<TextView>(R.id.tvSessionId).text = session.id
                findViewById<TextView>(R.id.tvSessionStatus).text = session.status.name
                findViewById<TextView>(R.id.tvLastActive).text = formatDate(session.lastActiveAt)
                findViewById<TextView>(R.id.tvDeviceInfo).text = getDeviceInfo(session)
                
                val isCurrentDevice = session.isThisDevice
                findViewById<TextView>(R.id.tvDeviceLabel).text = 
                    if (isCurrentDevice) "This device" else "Other device"
                
                findViewById<Button>(R.id.btnRevokeSession).apply {
                    isEnabled = !isCurrentDevice && session.status == Session.SessionStatus.ACTIVE
                    setOnClickListener {
                        onRevokeSession?.invoke(session)
                    }
                }
            }
        }
        
        private fun getDeviceInfo(session: Session): String {
            val activity = session.latestActivity
            return if (activity != null) {
                buildString {
                    activity.browserName?.let { append("$it ") }
                    activity.browserVersion?.let { append("$it, ") }
                    activity.city?.let { append("$it, ") }
                    activity.country?.let { append(it) }
                }
            } else {
                "Unknown device"
            }
        }
        
        private fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(Date(timestamp))
        }
    }
}
```

### Custom Error Handling

```kotlin
object ClerkErrorHandler {
    
    fun handleError(context: Context, error: ClerkErrorResponse) {
        when (error.code) {
            // Authentication errors
            "form_identifier_not_found" -> {
                showError(context, "No account found with this email address.")
            }
            "form_password_incorrect" -> {
                showError(context, "Incorrect password. Please try again.")
            }
            "form_identifier_exists" -> {
                showError(context, "An account with this email already exists.")
            }
            
            // Verification errors
            "verification_failed" -> {
                showError(context, "Verification failed. Please check your code and try again.")
            }
            "verification_expired" -> {
                showError(context, "Verification code has expired. Please request a new one.")
            }
            
            // Password security errors
            "form_password_pwned" -> {
                showError(context, "This password has been found in a data breach. Please choose a different password.")
            }
            "form_password_too_common" -> {
                showError(context, "This password is too common. Please choose a more secure password.")
            }
            
            // Rate limiting
            "rate_limit_exceeded" -> {
                showError(context, "Too many attempts. Please wait before trying again.")
            }
            
            // Network errors
            "network_error" -> {
                showError(context, "Network error. Please check your connection and try again.")
            }
            
            // MFA errors
            "totp_invalid" -> {
                showError(context, "Invalid authenticator code. Please try again.")
            }
            "backup_code_invalid" -> {
                showError(context, "Invalid backup code. Please try again.")
            }
            
            // Session errors
            "session_expired" -> {
                showError(context, "Your session has expired. Please sign in again.")
                // Navigate to sign-in screen
                navigateToSignIn(context)
            }
            
            // Default case
            else -> {
                showError(context, error.message ?: "An unexpected error occurred")
            }
        }
    }
    
    private fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    private fun navigateToSignIn(context: Context) {
        val intent = Intent(context, SignInActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}
```

### Reactive State Management

```kotlin
class AuthStateManager : ViewModel() {
    
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user
    
    init {
        // Observe Clerk initialization state
        Clerk.isInitialized.asLiveData().observe(this) { isInitialized ->
            if (isInitialized) {
                updateAuthState()
            }
        }
    }
    
    private fun updateAuthState() {
        _authState.value = when {
            Clerk.isSignedIn -> {
                _user.value = Clerk.user
                AuthState.SignedIn
            }
            Clerk.signIn != null -> AuthState.SigningIn
            Clerk.signUp != null -> AuthState.SigningUp
            else -> AuthState.SignedOut
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            val result = Clerk.signOut()
            result.onSuccess {
                updateAuthState()
            }
        }
    }
    
    fun refreshUser() {
        viewModelScope.launch {
            val result = Clerk.user?.get()
            result?.onSuccess { user ->
                _user.value = user
            }
        }
    }
}

sealed class AuthState {
    object SignedOut : AuthState()
    object SigningIn : AuthState()
    object SigningUp : AuthState()
    object SignedIn : AuthState()
}
```

---

## Best Practices

### 1. Initialization

Always initialize Clerk in your Application class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Clerk.initialize(
            context = this,
            publishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
        )
    }
}
```

### 2. Error Handling

Use consistent error handling throughout your app:

```kotlin
private suspend fun handleClerkOperation(operation: suspend () -> ClerkResult<*, ClerkErrorResponse>) {
    try {
        val result = operation()
        result.onFailure { error ->
            ClerkErrorHandler.handleError(this, error)
        }
    } catch (e: Exception) {
        showError("An unexpected error occurred: ${e.message}")
    }
}
```

### 3. State Observation

Use reactive patterns to observe authentication state:

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Observe initialization
        Clerk.isInitialized.asLiveData().observe(this) { isInitialized ->
            if (isInitialized) {
                setupUI()
            }
        }
    }
    
    private fun setupUI() {
        if (Clerk.isSignedIn) {
            // Show authenticated UI
            navigateToMainContent()
        } else {
            // Show authentication UI
            navigateToSignIn()
        }
    }
}
```

### 4. Security

- Never log sensitive information
- Use secure storage for tokens
- Implement proper session management
- Handle deep links securely

### 5. Performance

- Cache user data appropriately
- Use pagination for large lists
- Implement proper loading states
- Handle network errors gracefully

This comprehensive documentation covers all public APIs, functions, and components of the Clerk Android SDK with detailed examples and usage instructions.