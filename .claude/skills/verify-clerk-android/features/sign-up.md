# Sign up

A new user enters a test email or a test phone number, gives what the instance still requires, enters the code for each address, and lands signed in.

## Sub-features

- `complete` reaches the password step for a new test email, sets the run password, types `424242` on the code screen, and lands signed in.
- `phone` starts with a phone number, gives the email and the password that the standard settings require, verifies the email and then the phone with `424242`, and lands signed in.

## How to get to it (user POV)

- Open AuthView in sign-up mode, type an email, tap Continue, type a password, tap Continue, then type the code.
- For a phone sign-up, tap `Use phone number` on the start screen, type the number, and tap Continue. Type an email, tap Continue, type a password, tap Continue, then type the emailed code and the SMS code.

## Driving it with verify

Preconditions:

- Each spec gets an email from `host.newEmail()` and launches with `authMode: 'signUp'`, then taps `e2e.auth.signInFullScreen`. `e2e-tests/bin/control-clerk-android down` deletes the user the form creates.
- The standard settings take a phone number at sign-up and verify it by SMS code, and they still require an email and a password. `phone.e2e.ts` gets a number that no user holds from `host.newPhone()`.

- **Password step.** Run `e2e-tests/bin/control-clerk-android run sign-up/complete`. It fills `clerk.auth.start.identifier`, taps `clerk.auth.start.continue`, and expects `clerk.auth.signUp.password` and `clerk.auth.signUp.continue`. Screenshot `signup-password`.
- **Request code.** It then fills `clerk.auth.signUp.password` with `Verify-<runId>-Pw1!`, taps `clerk.auth.signUp.continue`, and expects `clerk.auth.signUp.code`. Screenshot `signup-code`.
- **Complete.** After the code screen it fills `CLERK_TEST_CODE` and waits for the home to show `Signed in as` the new email with a user ID. Screenshot `signed-up`.
- **Sign up with a phone number.** Run `e2e-tests/bin/control-clerk-android run sign-up/phone`. It taps `clerk.auth.start.identifierSwitcher`, fills `clerk.auth.start.phoneNumber` with the ten digits of the number, and taps `clerk.auth.start.continue`. It fills `clerk.auth.signUp.emailAddress` with the email and `clerk.auth.signUp.password` with the run password, and taps `clerk.auth.signUp.continue` after each. It expects `Check your email` with the email on the code screen, and fills `clerk.auth.signUp.code` with `CLERK_TEST_CODE`. Screenshot `signup-phone-email-code`. It expects `Check your phone` with the number as Clerk formats it, fills the code again, and waits for the home to show `Signed in as` the email with a user ID. Screenshots `signup-phone-code` and `signed-up-with-phone`.
- **Proof.** Both specs pass. `run.json` lists the email under `identities` with the id of the user the form created.

## Gotchas

- Android asks for the password before the code.
- A phone sign-up collects everything before it verifies anything: the email, then the password. It then verifies the email before the phone.
- Both code screens use `clerk.auth.signUp.code`. Their titles tell them apart, `Check your email` and `Check your phone`, and neither title has a test tag, so those checks use text.
- The phone code screen shows the number formatted, `+1 201-555-01xx`. `asShownByClerk(phone)` in `specs/phone.ts` gives that text.
- The password field withholds its value from the screen tree. `host.fill` types the password once and cannot confirm that it landed, so assert on what the form does next.
