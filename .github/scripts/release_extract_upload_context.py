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
"""Extract uploaded artifact URLs from Maven deploy logs."""

from __future__ import annotations

import json
import os
import re
from pathlib import Path

from github_actions_utils import write_output


def main() -> int:
    repository_name = os.environ.get("TARGET_REPOSITORY", "").strip()
    log_file = Path(os.environ.get("DEPLOY_LOG", "maven-deploy.log"))
    context_file = Path(os.environ.get("DEPLOY_ARTIFACTS_FILE", "deploy-artifacts.json"))

    log_text = log_file.read_text(encoding="utf-8")
    pattern = re.compile(r"Uploaded to (\S+):\s+(\S+)")

    uploaded_urls: list[str] = []
    for target_repo, url in pattern.findall(log_text):
        if target_repo == repository_name:
            uploaded_urls.append(url)

    deduped_urls = sorted(set(uploaded_urls))
    jar_urls = [url for url in deduped_urls if url.endswith(".jar")]
    pom_urls = [url for url in deduped_urls if url.endswith(".pom")]

    payload = {
        "target_repository": repository_name,
        "uploaded_urls": deduped_urls,
        "jar_urls": jar_urls,
        "pom_urls": pom_urls,
    }
    context_file.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")

    write_output("uploaded_urls_count", str(len(deduped_urls)))
    write_output("jar_urls_count", str(len(jar_urls)))
    write_output("pom_urls_count", str(len(pom_urls)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
