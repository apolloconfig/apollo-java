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
"""Write release publish context summary for GitHub Actions."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any


def read_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {}
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {}


def main() -> int:
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY", "").strip()
    if not summary_path:
        return 0

    deploy_path = Path(os.environ.get("DEPLOY_ARTIFACTS_FILE", "deploy-artifacts.json"))
    repository_path = Path(
        os.environ.get("REPOSITORY_CONTEXT_FILE", "repository-context.json")
    )

    deploy = read_json(deploy_path)
    repository = read_json(repository_path)

    lines = [
        "## Publish Context",
        "",
        f"- target repository: {deploy.get('target_repository', '')}",
        f"- uploaded URLs: {len(deploy.get('uploaded_urls', []))}",
        f"- jar URLs: {len(deploy.get('jar_urls', []))}",
        f"- pom URLs: {len(deploy.get('pom_urls', []))}",
        f"- staging key status: {repository.get('status', '')}",
        f"- repository_key: {repository.get('repository_key', '')}",
        f"- portal_deployment_id: {repository.get('portal_deployment_id', '')}",
    ]
    reason = repository.get("reason", "")
    if reason:
        lines.append(f"- reason: {reason}")

    with open(summary_path, "a", encoding="utf-8") as output:
        output.write("\n".join(lines) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
