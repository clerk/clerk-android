#!/usr/bin/env python3
"""Preflight and fixture cleanup for the Clerk instances behind the Trailblaze e2e trails.

Keys come from `.keys.json` at the repository root (same format and instance names as clerk-ios),
or from E2E_CLERK_PUBLISHABLE_KEY / E2E_CLERK_SECRET_KEY when E2E_CLERK_KEY_NAME names the key.
"""

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BACKEND_API_VERSION = "2026-05-12"
HTTP_TIMEOUT_SECONDS = 20

# Recorded trails replay a fixed sequence of screens, so each instance must make the prebuilt
# sign-up collect exactly the fields its trail types in.
SIGN_UP_FIELDS_BY_KEY_NAME = {
    "auth-email-code-password": {"email_address", "password"},
    "auth-phone-code": {"phone_number"},
    "session-task-setup-mfa": {"email_address"},
}

SECRET_KEY_REQUIRED_KEY_NAMES = {"auth-phone-code"}

SIGN_UP_ATTRIBUTES = [
    "email_address",
    "phone_number",
    "username",
    "password",
    "first_name",
    "last_name",
]


def attribute(environment, name):
    return environment.get("user_settings", {}).get("attributes", {}).get(name) or {}


def requirement(description, predicate):
    return description, predicate


def first_factor(name):
    return requirement(
        f"{name} is enabled as a first factor",
        lambda env: attribute(env, name).get("enabled") is True
        and attribute(env, name).get("used_for_first_factor") is True,
    )


def second_factor(name):
    return requirement(
        f"{name} is enabled as a second factor",
        lambda env: attribute(env, name).get("enabled") is True
        and attribute(env, name).get("used_for_second_factor") is True,
    )


def verification_strategy(name, strategy):
    return requirement(
        f"{name} is verified with {strategy}",
        lambda env: strategy in (attribute(env, name).get("verifications") or []),
    )


def required_sign_up_fields(environment):
    return {
        name
        for name in SIGN_UP_ATTRIBUTES
        if attribute(environment, name).get("enabled") is True
        and attribute(environment, name).get("required") is True
    }


def sign_up_fields(expected):
    def predicate(environment):
        return required_sign_up_fields(environment) == expected

    return requirement(f"sign-up requires exactly {sorted(expected)}", predicate)


BASE_REQUIREMENTS = [
    requirement(
        "environment payload includes auth_config, user_settings, and display_config",
        lambda env: all(isinstance(env.get(key), dict) for key in ("auth_config", "user_settings", "display_config")),
    ),
    requirement(
        "public sign-up is enabled",
        lambda env: env.get("user_settings", {}).get("sign_up", {}).get("mode") == "public",
    ),
    requirement(
        "legal consent is disabled for sign-up",
        lambda env: env.get("user_settings", {}).get("sign_up", {}).get("legal_consent_enabled") is not True,
    ),
    requirement(
        "self-serve account deletion is enabled",
        lambda env: env.get("user_settings", {}).get("actions", {}).get("delete_self") is True,
    ),
]

REQUIREMENTS_BY_KEY_NAME = {
    "auth-email-code-password": [
        first_factor("email_address"),
        verification_strategy("email_address", "email_code"),
    ],
    "auth-phone-code": [
        first_factor("phone_number"),
        verification_strategy("phone_number", "phone_code"),
    ],
    "session-task-setup-mfa": [
        first_factor("email_address"),
        verification_strategy("email_address", "email_code"),
        second_factor("authenticator_app"),
    ],
}


def load_keys(keys_file):
    path = Path(keys_file)
    if not path.is_absolute():
        path = REPO_ROOT / path
    if not path.exists():
        return {}
    return json.loads(path.read_text())


def key_entry(keys, key_name):
    entry = keys.get(key_name)
    return entry if isinstance(entry, dict) else {}


def env_override(key_name, variable):
    if os.environ.get("E2E_CLERK_KEY_NAME", "").strip() != key_name:
        return ""
    return os.environ.get(variable, "").strip()


def publishable_key_for(keys, key_name):
    return env_override(key_name, "E2E_CLERK_PUBLISHABLE_KEY") or str(key_entry(keys, key_name).get("pk") or "").strip()


def secret_key_for(keys, key_name):
    entry = key_entry(keys, key_name)
    return (
        env_override(key_name, "E2E_CLERK_SECRET_KEY")
        or str(entry.get("sk") or entry.get("secret_key") or entry.get("secretKey") or "").strip()
    )


def frontend_api_host(publishable_key):
    if not publishable_key.startswith(("pk_test_", "pk_live_")):
        raise ValueError("publishable key has an invalid prefix")
    encoded = publishable_key.split("_", 2)[2].rstrip("$")
    encoded += "=" * (-len(encoded) % 4)
    try:
        host = base64.urlsafe_b64decode(encoded).decode().rstrip("$")
    except (ValueError, UnicodeDecodeError) as error:
        raise ValueError("publishable key could not be decoded") from error
    if not host:
        raise ValueError("publishable key does not decode to a frontend API host")
    return host


def backend_api_base_url(publishable_key):
    override = os.environ.get("E2E_CLERK_BACKEND_API_URL", "").strip().rstrip("/")
    if override:
        return override.removesuffix("/v1")
    host = frontend_api_host(publishable_key)
    if "accountsstage" in host or "clerkstage" in host:
        return "https://api.clerkstage.dev"
    if "lclclerk" in host:
        return "https://api.lclclerk.com"
    return "https://api.clerk.com"


def http_json(method, url, headers=None):
    request = urllib.request.Request(url, method=method, headers={"Accept": "application/json", **(headers or {})})
    try:
        with urllib.request.urlopen(request, timeout=HTTP_TIMEOUT_SECONDS) as response:
            body = response.read()
    except urllib.error.HTTPError as error:
        detail = error.read().decode(errors="replace")[:300]
        raise RuntimeError(f"{method} {urllib.parse.urlsplit(url).path} failed with HTTP {error.code}: {detail}") from error
    return json.loads(body) if body else None


def fetch_environment(publishable_key):
    host = frontend_api_host(publishable_key)
    return http_json("GET", f"https://{host}/v1/environment?_is_native=true")


def report(message):
    if os.environ.get("GITHUB_ACTIONS") == "true":
        print(f"::error::{message}", file=sys.stderr)
    else:
        print(f"  - {message}", file=sys.stderr)


def validate(args):
    keys = load_keys(args.keys_file)
    failures = []
    for key_name in args.key_names:
        publishable_key = publishable_key_for(keys, key_name)
        if not publishable_key:
            failures.append(f"{key_name}: missing publishable key")
            continue
        if key_name in SECRET_KEY_REQUIRED_KEY_NAMES and not secret_key_for(keys, key_name):
            failures.append(f"{key_name}: missing secret key")

        try:
            environment = fetch_environment(publishable_key)
        except (ValueError, RuntimeError, OSError) as error:
            failures.append(f"{key_name}: {error}")
            continue

        requirements = list(BASE_REQUIREMENTS) + REQUIREMENTS_BY_KEY_NAME.get(key_name, [])
        expected_fields = SIGN_UP_FIELDS_BY_KEY_NAME.get(key_name)
        if expected_fields is not None:
            requirements.append(sign_up_fields(expected_fields))
        missing = [description for description, predicate in requirements if not predicate(environment)]
        if key_name not in REQUIREMENTS_BY_KEY_NAME:
            print(f"⚠️  {key_name}: no key-specific capability map exists; validated base requirements only")
        if missing:
            actual = sorted(required_sign_up_fields(environment))
            for description in missing:
                failures.append(f"{key_name}: expected {description} (sign-up currently requires {actual})")
        else:
            print(f"✅ {key_name}: frontend environment preflight passed")

    if failures:
        print("❌ E2E test instance preflight failed:", file=sys.stderr)
        for failure in failures:
            report(failure)
        return 1
    print("✅ E2E test instance preflight passed")
    return 0


def e164(phone_number):
    digits = "".join(character for character in phone_number if character.isdigit())
    return f"+{digits}" if len(digits) == 11 and digits.startswith("1") else f"+1{digits}"


def delete_phone_users(args):
    keys = load_keys(args.keys_file)
    publishable_key = publishable_key_for(keys, args.key_name)
    secret_key = secret_key_for(keys, args.key_name)
    if not publishable_key or not secret_key:
        print(f"⚠️  {args.key_name}: no publishable and secret key pair; skipping phone fixture cleanup")
        return 0

    phone_number = e164(args.phone_number)
    base_url = backend_api_base_url(publishable_key)
    headers = {"Authorization": f"Bearer {secret_key}", "Clerk-API-Version": BACKEND_API_VERSION}
    query = urllib.parse.urlencode({"phone_number[]": phone_number, "limit": "100"})
    response = http_json("GET", f"{base_url}/v1/users?{query}", headers)
    users = response.get("data", []) if isinstance(response, dict) else response or []
    user_ids = [
        user["id"]
        for user in users
        if any(resource.get("phone_number") == phone_number for resource in user.get("phone_numbers") or [])
    ]
    for user_id in user_ids:
        http_json("DELETE", f"{base_url}/v1/users/{user_id}", headers)
    print(f"✅ {args.key_name}: deleted {len(user_ids)} existing user(s) with the e2e sign-up phone number")
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--keys-file", default=".keys.json")
    commands = parser.add_subparsers(dest="command", required=True)

    validate_parser = commands.add_parser("validate", help="Check that each instance supports its trail")
    validate_parser.add_argument("key_names", nargs="+")
    validate_parser.set_defaults(handler=validate)

    delete_parser = commands.add_parser("delete-phone-users", help="Delete users holding a fixture phone number")
    delete_parser.add_argument("key_name")
    delete_parser.add_argument("phone_number")
    delete_parser.set_defaults(handler=delete_phone_users)

    args = parser.parse_args()
    return args.handler(args)


if __name__ == "__main__":
    sys.exit(main())
