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
"""Resolve Sonatype repository context for release deployments."""

from __future__ import annotations

import base64
import json
import os
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path
from typing import Any

from github_actions_utils import write_output

OSSRH_BASE = "https://ossrh-staging-api.central.sonatype.com"


def request_json(url: str, headers: dict[str, str]) -> tuple[int | None, dict[str, Any]]:
    request = urllib.request.Request(url=url, method="GET", headers=headers)
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


def main() -> int:
    target_repository = os.environ.get("TARGET_REPOSITORY", "").strip()
    namespace = os.environ.get("TARGET_NAMESPACE", "").strip()
    username = os.environ.get("MAVEN_USERNAME", "")
    password = os.environ.get("MAVEN_CENTRAL_TOKEN", "")
    context_path = Path(
        os.environ.get("REPOSITORY_CONTEXT_FILE", "repository-context.json")
    )

    context: dict[str, Any] = {
        "target_repository": target_repository,
        "namespace": namespace,
        "status": "not_applicable",
        "reason": "repository input is not releases",
        "repository_key": "",
        "portal_deployment_id": "",
        "search_candidates": [],
    }

    if target_repository == "releases":
        if not username or not password:
            context["status"] = "manual_required"
            context["reason"] = "Missing MAVEN_USERNAME/MAVEN_CENTRAL_TOKEN"
        else:
            token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("utf-8")
            headers = {
                "Authorization": f"Bearer {token}",
                "Accept": "application/json",
            }

            searches = [
                ("open", "client"),
                ("closed", "client"),
                ("open", "any"),
                ("closed", "any"),
            ]
            selected: dict[str, Any] | None = None
            last_error = ""

            for state, ip in searches:
                url = (
                    f"{OSSRH_BASE}/manual/search/repositories?"
                    f"profile_id={urllib.parse.quote(namespace)}"
                    f"&state={urllib.parse.quote(state)}"
                    f"&ip={urllib.parse.quote(ip)}"
                )
                status, payload = request_json(url, headers)
                if status is None:
                    last_error = payload.get("error", "unknown error")
                    context["search_candidates"].append(
                        {
                            "state": state,
                            "ip": ip,
                            "status": None,
                            "count": 0,
                            "error": last_error,
                        }
                    )
                    continue

                if status < 200 or status >= 300:
                    http_error = payload.get("error", f"HTTP {status}")
                    last_error = http_error
                    context["search_candidates"].append(
                        {
                            "state": state,
                            "ip": ip,
                            "status": status,
                            "count": 0,
                            "error": http_error,
                        }
                    )
                    continue

                repositories = (
                    payload.get("repositories", []) if isinstance(payload, dict) else []
                )
                context["search_candidates"].append(
                    {"state": state, "ip": ip, "status": status, "count": len(repositories)}
                )
                if repositories:
                    selected = repositories[0]
                    break

            if selected:
                context["status"] = "resolved"
                context["reason"] = ""
                context["repository_key"] = selected.get("key", "") or ""
                context["portal_deployment_id"] = (
                    selected.get("portal_deployment_id", "") or ""
                )
            else:
                context["status"] = "manual_required"
                context["reason"] = last_error or "No staging repository key found"

    context_path.write_text(json.dumps(context, indent=2) + "\n", encoding="utf-8")
    write_output("repository_key", context.get("repository_key", ""))
    write_output("portal_deployment_id", context.get("portal_deployment_id", ""))
    write_output("status", context.get("status", ""))
    write_output("reason", context.get("reason", ""))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
