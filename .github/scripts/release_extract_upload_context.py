#!/usr/bin/env python3
"""Extract uploaded artifact URLs from Maven deploy logs."""

from __future__ import annotations

import json
import os
import re
from pathlib import Path


def write_output(key: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT", "").strip()
    if not output_path:
        return
    with open(output_path, "a", encoding="utf-8") as out:
        out.write(f"{key}={value}\n")


def main() -> int:
    repository_name = os.environ.get("TARGET_REPOSITORY", "").strip()
    log_file = Path(os.environ.get("DEPLOY_LOG", "maven-deploy.log"))
    context_file = Path(os.environ.get("DEPLOY_ARTIFACTS_FILE", "deploy-artifacts.json"))

    log_text = log_file.read_text(encoding="utf-8")
    pattern = re.compile(r"Uploaded to (\S+):\s+(\S+)")

    uploaded_urls: list[str] = []
    for target_repo, url in pattern.findall(log_text):
        normalized = target_repo.rstrip(":")
        if normalized == repository_name:
            uploaded_urls.append(url)

    deduped_urls = sorted(set(uploaded_urls))
    jar_urls = [
        url for url in deduped_urls
        if url.endswith(".jar") and not url.endswith(".jar.asc")
    ]
    pom_urls = [
        url for url in deduped_urls
        if url.endswith(".pom") and not url.endswith(".pom.asc")
    ]

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
