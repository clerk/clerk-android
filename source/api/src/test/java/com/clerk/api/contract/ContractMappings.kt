package com.clerk.api.contract

import com.clerk.api.auth.types.Strategy
import com.clerk.api.billing.BillingAppliedDiscount
import com.clerk.api.billing.BillingCreditBalance
import com.clerk.api.billing.BillingCreditLedger
import com.clerk.api.billing.BillingCredits
import com.clerk.api.billing.BillingDiscountRedemption
import com.clerk.api.billing.BillingDiscounts
import com.clerk.api.billing.BillingMoneyAmount
import com.clerk.api.billing.BillingPayerCredit
import com.clerk.api.billing.BillingPayment
import com.clerk.api.billing.BillingPaymentMethod
import com.clerk.api.billing.BillingPaymentTotals
import com.clerk.api.billing.BillingPerUnitTotal
import com.clerk.api.billing.BillingPerUnitTotalTier
import com.clerk.api.billing.BillingPeriodTotals
import com.clerk.api.billing.BillingPlan
import com.clerk.api.billing.BillingPlanPrice
import com.clerk.api.billing.BillingPlanUnitPrice
import com.clerk.api.billing.BillingPlanUnitPriceTier
import com.clerk.api.billing.BillingProrationCreditDetail
import com.clerk.api.billing.BillingProrationDiscount
import com.clerk.api.billing.BillingStatement
import com.clerk.api.billing.BillingStatementGroup
import com.clerk.api.billing.BillingStatementTotals
import com.clerk.api.billing.BillingSubscription
import com.clerk.api.billing.BillingSubscriptionItem
import com.clerk.api.billing.BillingSubscriptionItemCredit
import com.clerk.api.billing.BillingSubscriptionItemNextPayment
import com.clerk.api.billing.BillingSubscriptionItemSeats
import com.clerk.api.billing.BillingSubscriptionNextPayment
import com.clerk.api.billing.BillingTotals
import com.clerk.api.billing.Feature
import com.clerk.api.billing.ForPayerType
import com.clerk.api.biometriccredential.BiometricCredential
import com.clerk.api.biometriccredential.BiometricCredentialChallenge
import com.clerk.api.biometriccredential.BiometricCredentialPayload
import com.clerk.api.biometriccredential.BiometricCredentialPolicy
import com.clerk.api.biometriccredential.BiometricCredentialValidation
import com.clerk.api.emailaddress.EmailAddress
import com.clerk.api.externalaccount.ExternalAccount
import com.clerk.api.network.ClerkPaginatedResponse
import com.clerk.api.network.model.account.EnterpriseAccount
import com.clerk.api.network.model.backupcodes.BackupCodeResource
import com.clerk.api.network.model.client.Client
import com.clerk.api.network.model.deleted.DeletedObject
import com.clerk.api.network.model.environment.AuthConfig
import com.clerk.api.network.model.environment.CommerceSettings
import com.clerk.api.network.model.environment.DisplayConfig
import com.clerk.api.network.model.environment.Environment
import com.clerk.api.network.model.environment.NativeAppSettings
import com.clerk.api.network.model.environment.OrganizationSettings
import com.clerk.api.network.model.environment.UserSettings
import com.clerk.api.network.model.error.ClerkErrorResponse
import com.clerk.api.network.model.error.Error
import com.clerk.api.network.model.factor.Factor
import com.clerk.api.network.model.image.ImageResource
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.model.totp.TOTPResource
import com.clerk.api.network.model.userdata.PublicUserData
import com.clerk.api.network.model.verification.Verification
import com.clerk.api.organizations.Organization
import com.clerk.api.organizations.OrganizationCreationDefaults
import com.clerk.api.organizations.OrganizationDomain
import com.clerk.api.organizations.OrganizationInvitation
import com.clerk.api.organizations.OrganizationMembership
import com.clerk.api.organizations.OrganizationMembershipRequest
import com.clerk.api.organizations.OrganizationSuggestion
import com.clerk.api.organizations.OrganizationSystemPermission
import com.clerk.api.organizations.Permission
import com.clerk.api.organizations.PublicOrganizationData
import com.clerk.api.organizations.Role
import com.clerk.api.organizations.UserOrganizationInvitation
import com.clerk.api.passkeys.Passkey
import com.clerk.api.phonenumber.PhoneNumber
import com.clerk.api.session.Session
import com.clerk.api.session.SessionActivity
import com.clerk.api.session.SessionTask
import com.clerk.api.session.SessionVerification
import com.clerk.api.signin.SignIn
import com.clerk.api.signin.SignInPayload
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.OAuthResult
import com.clerk.api.sso.ResultType
import com.clerk.api.user.User
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

internal data class ContractMapping(
  val model: KClass<*>,
  val serializer: KSerializer<*>,
  val schema: SchemaRef,
)

internal data class UnmappedModel(val model: KClass<*>, val reason: String)

internal object ContractMappings {
  private const val SIGN_IN = "Client.SignIn-2"
  private const val REQUEST = "request parameters, not a response"
  private const val ON_DEVICE = "built on the device, not read from the API"
  private const val OPEN_STRING =
    "wraps a strategy string that the spec leaves open, so there is no schema or enum to compare"
  private const val INLINE_RESPONSE =
    "declared inline in the /v1/me/organization_creation_defaults response, not as a named schema"

  private inline fun <reified T : Any> map(serializer: KSerializer<T>, schema: String) =
    ContractMapping(T::class, serializer, SchemaRef.Named(schema))

  private inline fun <reified T : Any> map(
    serializer: KSerializer<T>,
    schema: String,
    vararg path: String,
  ) = ContractMapping(T::class, serializer, SchemaRef.PropertyOf(schema, *path))

  val mapped: List<ContractMapping> =
    listOf(
      map(BillingAppliedDiscount.serializer(), "BillingAppliedDiscount"),
      map(BillingCredits.serializer(), "BillingCreditsResponse"),
      map(BillingDiscountRedemption.serializer(), "BillingDiscountRedemption"),
      map(BillingDiscounts.serializer(), "BillingDiscountsResponse"),
      map(BillingMoneyAmount.serializer(), "BillingMoneyResponse"),
      map(BillingPayerCredit.serializer(), "BillingPayerCreditResponse"),
      map(BillingPayment.serializer(), "BillingPaymentAttempt"),
      map(BillingPaymentMethod.serializer(), "BillingPaymentMethod"),
      map(BillingPaymentTotals.serializer(), "BillingTotalsResponse"),
      map(BillingPerUnitTotal.serializer(), "BillingPerUnitTotal"),
      map(BillingPerUnitTotalTier.serializer(), "BillingPerUnitTotalTier"),
      map(BillingPeriodTotals.serializer(), "BillingPerPeriodTotals"),
      map(BillingPlan.serializer(), "BillingPlan"),
      map(BillingPlanUnitPrice.serializer(), "BillingPlanUnitPrice"),
      map(BillingPlanUnitPriceTier.serializer(), "BillingPlanUnitPriceTier"),
      map(BillingProrationCreditDetail.serializer(), "BillingProrationCreditDetail"),
      map(BillingProrationDiscount.serializer(), "BillingProrationDiscountDetail"),
      map(BillingStatement.serializer(), "BillingStatement"),
      map(BillingStatementGroup.serializer(), "BillingStatementResponseGroup"),
      map(BillingStatementTotals.serializer(), "BillingStatementTotalsResponse"),
      map(BillingSubscription.serializer(), "BillingSubscription"),
      map(BillingSubscriptionItem.serializer(), "BillingSubscriptionItem"),
      map(BillingSubscriptionItemCredit.serializer(), "BillingSubscriptionCreditResponse"),
      map(BillingSubscriptionItemNextPayment.serializer(), "BillingSubscriptionItemNextPayment"),
      map(BillingSubscriptionItemSeats.serializer(), "BillingSubscriptionItemSeats"),
      map(BillingSubscriptionNextPayment.serializer(), "BillingSubscriptionNextPayment"),
      map(BillingTotals.serializer(), "BillingCheckoutTotals"),
      map(Feature.serializer(), "BillingPlan", "features"),
      map(BiometricCredentialChallenge.serializer(), "Client.BiometricCredentialChallenge"),
      map(BiometricCredentialPayload.serializer(), "Client.BiometricCredential"),
      map(BiometricCredentialValidation.serializer(), "Client.BiometricCredentialValidation"),
      map(EmailAddress.serializer(), "Client.EmailAddress"),
      map(EmailAddress.LinkedEntity.serializer(), "Stubs.Identification.Link"),
      map(ExternalAccount.serializer(), "ExternalAccountWithVerification"),
      map(BackupCodeResource.serializer(), "BackupCodes"),
      map(Client.serializer(), "Client.Client-2"),
      map(DeletedObject.serializer(), "Client.DeletedObject"),
      map(Environment.serializer(), "Client.Environment"),
      map(AuthConfig.serializer(), "Client.AuthConfig"),
      map(AuthConfig.NativeSettings.serializer(), "Client.NativeSettings"),
      map(DisplayConfig.serializer(), "Client.DisplayConfig"),
      map(CommerceSettings.serializer(), "Client.CommerceSettings"),
      map(CommerceSettings.Billing.serializer(), "Client.CommerceSettings", "billing"),
      map(
        CommerceSettings.Billing.Payer.serializer(),
        "Client.CommerceSettings",
        "billing",
        "user",
      ),
      map(
        CommerceSettings.Billing.Payer.serializer(),
        "Client.CommerceSettings",
        "billing",
        "organization",
      ),
      map(NativeAppSettings.serializer(), "Client.NativeAppSettings"),
      map(
        NativeAppSettings.MinimumSupportedVersion.serializer(),
        "Client.NativeAppSettings",
        "minimum_supported_version",
      ),
      map(
        NativeAppSettings.AndroidPolicy.serializer(),
        "Client.NativeAppSettings",
        "minimum_supported_version",
        "android",
      ),
      map(OrganizationSettings.serializer(), "Client.OrganizationSettings"),
      map(OrganizationSettings.Actions.serializer(), "OrganizationSettings.ActionsSettings"),
      map(OrganizationSettings.Domains.serializer(), "OrganizationSettings.DomainsSettings"),
      map(OrganizationSettings.Slug.serializer(), "OrganizationSettings.SlugSettings"),
      map(UserSettings.serializer(), "Client.UserSettings"),
      map(UserSettings.Actions.serializer(), "UserSettings.ActionsSettings"),
      map(UserSettings.AttributesConfig.serializer(), "UserSettings.Attribute"),
      map(UserSettings.PasskeySettings.serializer(), "UserSettings.PasskeySettings"),
      map(UserSettings.SignUpUserSettings.serializer(), "UserSettings.SignUp"),
      map(UserSettings.SocialConfig.serializer(), "UserSettings.Social"),
      map(ClerkErrorResponse.serializer(), "ClerkErrors"),
      map(Error.serializer(), "ClerkError"),
      map(Factor.serializer(), "Stubs.SignInFactor"),
      map(ImageResource.serializer(), "Image"),
      map(TokenResource.serializer(), "Token"),
      map(TOTPResource.serializer(), "TOTP"),
      map(PublicUserData.serializer(), "Client.PublicUserData"),
      map(Verification.serializer(), SIGN_IN, "first_factor_verification"),
      map(Organization.serializer(), "Client.Organization"),
      map(OrganizationDomain.serializer(), "Client.OrganizationDomain"),
      map(OrganizationDomain.Verification.serializer(), "Client.OrganizationDomainVerification"),
      map(OrganizationInvitation.serializer(), "Client.OrganizationInvitation"),
      map(OrganizationMembership.serializer(), "Client.OrganizationMembership"),
      map(OrganizationMembershipRequest.serializer(), "Client.OrganizationMembershipRequest"),
      map(OrganizationSuggestion.serializer(), "Client.OrganizationSuggestion"),
      map(Permission.serializer(), "Client.Permission"),
      map(PublicOrganizationData.serializer(), "Client.PublicOrganizationData"),
      map(Role.serializer(), "Client.Role"),
      map(UserOrganizationInvitation.serializer(), "Client.OrganizationInvitationUserContext"),
      map(
        UserOrganizationInvitation.PublicOrganizationData.serializer(),
        "Client.PublicOrganizationData",
      ),
      map(Passkey.serializer(), "Client.Passkey"),
      map(PhoneNumber.serializer(), "Client.PhoneNumber"),
      map(Session.serializer(), "Client.Session-2"),
      map(Session.serializer(), "Client.SessionReverification", "session"),
      map(SessionActivity.serializer(), "Client.SessionActivity"),
      map(SessionTask.serializer(), "Client.SessionTask"),
      map(SessionVerification.serializer(), "Client.SessionReverification"),
      map(SignInPayload.serializer(), SIGN_IN),
      map(SignIn.UserData.serializer(), SIGN_IN, "user_data"),
      map(SignUp.serializer(), "Client.SignUp"),
      map(User.serializer(), "Client.User"),
    )

  val unmapped: List<UnmappedModel> =
    listOf(
      UnmappedModel(Strategy::class, OPEN_STRING),
      UnmappedModel(
        BillingCreditBalance::class,
        "no matching schema: the spec has no /credits endpoint",
      ),
      UnmappedModel(
        BillingCreditLedger::class,
        "no matching schema: the spec has no /credits/history endpoint",
      ),
      UnmappedModel(
        BillingPlanPrice::class,
        "no matching schema: the spec's BillingPlan has no available_prices",
      ),
      UnmappedModel(ForPayerType::class, "a query parameter of billing requests, not a response"),
      UnmappedModel(
        BiometricCredential::class,
        "decoded through BiometricCredentialPayload, which is mapped",
      ),
      UnmappedModel(
        BiometricCredentialPolicy::class,
        "protects the private key on the device and never reaches the API",
      ),
      UnmappedModel(EmailAddress.PrepareVerificationParams::class, REQUEST),
      UnmappedModel(EmailAddress.PrepareVerificationParams.EmailCode::class, REQUEST),
      UnmappedModel(EmailAddress.PrepareVerificationParams.EnterpriseSSO::class, REQUEST),
      UnmappedModel(ClerkPaginatedResponse::class, "generic list wrapper, its items are mapped"),
      UnmappedModel(
        EnterpriseAccount::class,
        "no matching schema: Client.User has no enterprise_accounts, and " +
          "Client.SAMLAccount is the older SAML-only shape",
      ),
      UnmappedModel(
        EnterpriseAccount.EnterpriseConnection::class,
        "no matching schema: the spec has no enterprise_connection inside an account",
      ),
      UnmappedModel(
        OrganizationCreationDefaults::class,
        INLINE_RESPONSE,
      ),
      UnmappedModel(
        OrganizationCreationDefaults.Advisory::class,
        INLINE_RESPONSE,
      ),
      UnmappedModel(
        OrganizationCreationDefaults.Form::class,
        INLINE_RESPONSE,
      ),
      UnmappedModel(
        OrganizationSystemPermission::class,
        "keys the SDK looks up in OrganizationMembership.permissions, which the spec types as " +
          "plain strings",
      ),
      UnmappedModel(SignIn::class, "decoded through SignInPayload, which is mapped"),
      UnmappedModel(
        SignIn.CredentialType::class,
        "picks the Credential Manager request on the device and never reaches the API",
      ),
      UnmappedModel(SignIn.AttemptFirstFactorParams::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.EmailCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.Passkey::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.Password::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.PhoneCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.ResetPasswordEmailCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptFirstFactorParams.ResetPasswordPhoneCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams.BackupCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams.EmailCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams.Passkey::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams.PhoneCode::class, REQUEST),
      UnmappedModel(SignIn.AttemptSecondFactorParams.TOTP::class, REQUEST),
      UnmappedModel(SignIn.AuthenticateWithRedirectParams.EnterpriseSSO::class, REQUEST),
      UnmappedModel(SignIn.AuthenticateWithRedirectParams.OAuth::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.BiometricCredential::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.EmailCode::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.Identifier::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.Passkey::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.Password::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.PhoneCode::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.ResetPasswordEmailCode::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.ResetPasswordPhoneCode::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.Ticket::class, REQUEST),
      UnmappedModel(SignIn.CreateParams.Strategy.Transfer::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.EmailCode::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.EmailLink::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.EnterpriseSSO::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.OAuth::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.Passkey::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.PhoneCode::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.ResetPasswordEmailCode::class, REQUEST),
      UnmappedModel(SignIn.PrepareFirstFactorParams.ResetPasswordPhoneCode::class, REQUEST),
      UnmappedModel(SignIn.PrepareSecondFactorParams::class, REQUEST),
      UnmappedModel(SignIn.ResetPasswordParams::class, REQUEST),
      UnmappedModel(SignUp.AuthenticateWithRedirectParams.EnterpriseSSO::class, REQUEST),
      UnmappedModel(SignUp.AuthenticateWithRedirectParams.OAuth::class, REQUEST),
      UnmappedModel(SignUp.CreateParams::class, REQUEST),
      UnmappedModel(SignUp.CreateParams.GoogleOneTap::class, REQUEST),
      UnmappedModel(SignUp.CreateParams.None::class, REQUEST),
      UnmappedModel(SignUp.CreateParams.Standard::class, REQUEST),
      UnmappedModel(SignUp.CreateParams.Ticket::class, REQUEST),
      UnmappedModel(SignUp.CreateParams.Transfer::class, REQUEST),
      UnmappedModel(SignUp.SignUpUpdateParams::class, REQUEST),
      UnmappedModel(SignUp.SignUpUpdateParams.Standard::class, REQUEST),
      UnmappedModel(OAuthProvider::class, OPEN_STRING),
      UnmappedModel(
        OAuthResult::class,
        "built on the device from a sign-in or sign-up, not from the API",
      ),
      UnmappedModel(ResultType::class, ON_DEVICE),
      UnmappedModel(User.CreateExternalAccountParams::class, REQUEST),
      UnmappedModel(User.UpdateMetadataParams::class, REQUEST),
      UnmappedModel(User.UpdateParams::class, REQUEST),
      UnmappedModel(User.UpdatePasswordParams::class, REQUEST),
    )
}
