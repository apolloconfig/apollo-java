#!/usr/bin/env python3
# Copyright 2026 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Trigger and monitor Sonatype portal publish flow."""

from __future__ import annotations

import base64
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any

from github_actions_utils import write_output

OSSRH_BASE = "https://ossrh-staging-api.central.sonatype.com"
PORTAL_BASE = "https://central.sonatype.com"


def request_json(
    method: str,
    url: str,
    headers: dict[str, str],
) -> tuple[int | None, dict[str, Any]]:
    request = urllib.request.Request(url=url, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            body = response.read().decode("utf-8")
            if not body:
                return response.status, {}
            try:
                return response.status, json.loads(body)
            except json.JSONDecodeError:
                return response.status, {"raw": body}
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8")
        try:
            payload = json.loads(body) if body else {}
        except json.JSONDecodeError:
            payload = {"raw": body}
        payload.setdefault("error", f"HTTP {error.code}")
        return error.code, payload
    except Exception as error:  # noqa: BLE001
        return None, {"error": str(error)}


def extract_deployment_state(payload: dict[str, Any]) -> str:
    for key in ("deploymentState", "deployment_state", "state"):
        value = payload.get(key)
        if isinstance(value, str):
            return value
    return "unknown"


def to_int(value: str, default: int) -> int:
    try:
        return int(value)
    except ValueError:
        return default


def main() -> int:
    namespace = os.environ.get("INPUT_NAMESPACE", "com.ctrip.framework.apollo").strip()
    repository_key = os.environ.get("INPUT_REPOSITORY_KEY", "").strip()
    timeout_minutes = to_int(os.environ.get("INPUT_TIMEOUT_MINUTES", "60"), 60)
    mode = os.environ.get("INPUT_MODE", "portal_api").strip().lower()

    username = os.environ.get("MAVEN_USERNAME", "")
    password = os.environ.get("MAVEN_CENTRAL_TOKEN", "")

    result = "manual_required"
    final_state = "unknown"
    deployment_id = ""
    deployment_url = ""
    reason = ""

    if not username or not password:
        reason = "Missing MAVEN_USERNAME/MAVEN_CENTRAL_TOKEN secrets"
    else:
        token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("utf-8")
        headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
        }

        if not repository_key:
            searches = ("open", "closed")
            search_errors: list[str] = []
            successful_search = False
            for state in searches:
                search_url = (
                    f"{OSSRH_BASE}/manual/search/repositories?"
                    f"ip=any&profile_id={urllib.parse.quote(namespace)}"
                    f"&state={urllib.parse.quote(state)}"
                )
                search_status, payload = request_json("GET", search_url, headers)
                if search_status is None or search_status < 200 or search_status >= 300:
                    search_error = payload.get("error") if isinstance(payload, dict) else ""
                    if not search_error:
                        search_error = (
                            f"HTTP {search_status}"
                            if search_status is not None
                            else "HTTP unknown"
                        )
                    search_errors.append(
                        f"Repository search failed ({state}): {search_error}"
                    )
                    continue

                successful_search = True
                repositories = payload.get("repositories", []) if isinstance(payload, dict) else []
                if repositories:
                    repository_key = repositories[0].get("key", "") or ""
                    break

        if not repository_key:
            if search_errors and not successful_search:
                reason = "; ".join(search_errors)
            else:
                reason = "No staging repository key found"
        else:
            upload_url = (
                f"{OSSRH_BASE}/manual/upload/repository/{urllib.parse.quote(repository_key)}"
                f"?publishing_type={urllib.parse.quote(mode)}"
            )
            upload_status, upload_payload = request_json("POST", upload_url, headers)
            if upload_status is None:
                reason = f"Upload API failed: {upload_payload.get('error', 'unknown error')}"
            elif upload_status >= 400:
                upload_error = upload_payload.get("error")
                if not upload_error:
                    upload_error = f"HTTP {upload_status}"
                reason = (
                    f"Upload API failed: {upload_error}"
                )

            if not reason:
                list_url = (
                    f"{OSSRH_BASE}/manual/search/repositories?"
                    f"ip=any&profile_id={urllib.parse.quote(namespace)}"
                )
                list_status, list_payload = request_json("GET", list_url, headers)
                if list_status is None or list_status < 200 or list_status >= 300:
                    list_error = list_payload.get("error") if isinstance(list_payload, dict) else ""
                    if not list_error:
                        list_error = (
                            f"HTTP {list_status}"
                            if list_status is not None
                            else "HTTP unknown"
                        )
                    reason = f"Repository list API failed after upload: {list_error}"
                else:
                    repositories = (
                        list_payload.get("repositories", [])
                        if isinstance(list_payload, dict)
                        else []
                    )
                    for item in repositories:
                        if item.get("key") == repository_key and item.get("portal_deployment_id"):
                            deployment_id = item.get("portal_deployment_id")
                            break

                    if deployment_id:
                        deployment_url = f"{PORTAL_BASE}/publishing/deployments/{deployment_id}"
                        publish_triggered = False
                        deadline = time.time() + timeout_minutes * 60

                        while time.time() <= deadline:
                            status_url = (
                                f"{PORTAL_BASE}/api/v1/publisher/status?"
                                f"id={urllib.parse.quote(deployment_id)}"
                            )
                            poll_status, status_payload = request_json(
                                "POST",
                                status_url,
                                headers,
                            )
                            if poll_status is None or poll_status < 200 or poll_status >= 300:
                                poll_error = (
                                    status_payload.get("error")
                                    if isinstance(status_payload, dict)
                                    else ""
                                )
                                if not poll_error:
                                    poll_error = (
                                        f"HTTP {poll_status}"
                                        if poll_status is not None
                                        else "HTTP unknown"
                                    )
                                reason = f"Status polling API failed: {poll_error}"
                                break

                            final_state = extract_deployment_state(status_payload)

                            if final_state == "PUBLISHED":
                                result = "published"
                                reason = ""
                                break

                            if final_state in {"FAILED", "BROKEN", "ERROR"}:
                                reason = f"Deployment entered terminal state: {final_state}"
                                break

                            if (
                                mode == "portal_api"
                                and final_state == "VALIDATED"
                                and not publish_triggered
                            ):
                                publish_url = (
                                    f"{PORTAL_BASE}/api/v1/publisher/deployment/"
                                    f"{urllib.parse.quote(deployment_id)}"
                                )
                                publish_status, publish_payload = request_json(
                                    "POST",
                                    publish_url,
                                    headers,
                                )
                                if publish_status is None or publish_status >= 400:
                                    publish_error = publish_payload.get("error")
                                    if not publish_error:
                                        publish_error = (
                                            f"HTTP {publish_status}"
                                            if publish_status is not None
                                            else "HTTP unknown"
                                        )
                                    reason = f"Publish API failed: {publish_error}"
                                    break
                                publish_triggered = True

                            if mode == "user_managed" and final_state == "VALIDATED":
                                reason = "Mode user_managed requires manual publish in portal"
                                break

                            time.sleep(10)

                        if result != "published" and not reason:
                            reason = (
                                "Timed out waiting for deployment status. "
                                f"Latest state={final_state}"
                            )
                    else:
                        reason = "No portal deployment id found for repository"

    if result != "published" and not reason:
        reason = "Automatic publish did not complete"

    write_output("result", result)
    write_output("repository_key", repository_key)
    write_output("deployment_id", deployment_id)
    write_output("deployment_url", deployment_url)
    write_output("final_state", final_state)
    write_output("reason", reason)

    display_key = repository_key or "-"
    display_deployment = deployment_id or "-"
    display_url = deployment_url or "-"
    print(
        "SONATYPE_RESULT "
        f"result={result} "
        f"repository_key={display_key} "
        f"deployment_id={display_deployment} "
        f"final_state={final_state} "
        f"deployment_url={display_url}"
    )
    if reason:
        print(f"SONATYPE_REASON {reason}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
