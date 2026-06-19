#!/usr/bin/env -S uv run --script
"""Generate SUMMARY.md — table of contents for all docs (top-level + features)."""

from pathlib import Path
import re

DOCS = Path(__file__).resolve().parent
SUMMARY = DOCS / "SUMMARY.md"
FEATURES = DOCS / "features"

# ── top-level doc entries ──────────────────────────────────────
# (filename, display title) — display preserves existing emoji prefixes
TOP_LEVEL = [
    ("README.md", "👋 欢迎"),
    ("getting-started.md", "🚀 快速开始"),
    ("installation.md", "📥 安装指南"),
    ("configuration.md", "⚙️ 配置指南"),
    ("module-settings.md", "模块设置说明"),
    ("faq.md", "❓ 常见问题"),
    ("bug-report-guide.md", "🐛 问题反馈指南"),
    ("feature-request-guide.md", "⭐ 建议反馈指南"),
    ("cosmic-level-disclaimer.md", "免责声明"),
]

# ── feature category display order ──────────────────────────────
CATEGORY_ORDER = [
    "chat",
    "contacts",
    "moments",
    "payment",
    "beautify",
    "system",
    "servers",
    "notifications",
    "official_accounts",
    "miniapps",
    "shortvideos",
    "profile",
    "scripting_js",
    "debug",
    "entertain",
    "easter_egg",
]


def extract_h1(path: Path) -> str | None:
    """Return first `# Title` from a markdown file, or None."""
    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return None
    m = re.search(r"^# (.+)$", text, re.MULTILINE)
    return m.group(1).strip() if m else None


def feature_docs(category: Path) -> list[tuple[str, str]]:
    """List [(title, relative_path), …] for feature .md files (excl. README)."""
    docs: list[tuple[str, str]] = []
    for child in sorted(category.iterdir()):
        if child.name == "README.md" or child.suffix != ".md" or not child.is_file():
            continue
        title = extract_h1(child) or child.stem
        docs.append((title, child.relative_to(DOCS).as_posix()))
    return docs


def gather_categories() -> list[tuple[str, list[tuple[str, str]]]]:
    """Return [(category_title, [(doc_title, rel_path), …]), …] in display order."""
    if not FEATURES.is_dir():
        return []

    raw: dict[str, tuple[str, list[tuple[str, str]]]] = {}
    for entry in sorted(FEATURES.iterdir()):
        if not entry.is_dir():
            continue
        readme = entry / "README.md"
        if not readme.is_file():
            continue
        title = extract_h1(readme) or entry.name
        docs = feature_docs(entry)
        raw[entry.name] = (title, docs)

    ordered: list[tuple[str, list[tuple[str, str]]]] = []
    seen: set[str] = set()
    for key in CATEGORY_ORDER:
        if key in raw:
            ordered.append(raw.pop(key))
            seen.add(key)
    # Any remaining uncategorised directories
    for key in sorted(raw):
        ordered.append(raw[key])
    return ordered


def build() -> str:
    lines = ["# 目录", ""]

    # ── Top-level docs ──────────────────────────────────────
    for fname, display in TOP_LEVEL:
        if (DOCS / fname).is_file():
            lines.append(f"* [{display}]({fname})")

    # ── Feature categories ──────────────────────────────────
    categories = gather_categories()
    if categories:
        lines += ["", "## 功能文档", ""]
        for cat_title, docs in categories:
            lines.append(f"### {cat_title}")
            lines.append("")
            for doc_title, rel in docs:
                lines.append(f"* [{doc_title}]({rel})")
            lines.append("")

    return "\n".join(lines) + "\n"


def main() -> None:
    content = build()
    SUMMARY.write_text(content, encoding="utf-8")
    print(f"✓ Regenerated {SUMMARY}")


if __name__ == "__main__":
    main()
