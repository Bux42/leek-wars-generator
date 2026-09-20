#!/usr/bin/env python3
"""Run one fight and build browsable flamegraph pages for its profiler output."""

from __future__ import annotations

import argparse
import html
import os
from pathlib import Path
import shutil
import subprocess
import sys
import webbrowser
from urllib.parse import quote


REPOSITORY_ROOT = Path(__file__).resolve().parent
PROFILER_OUTPUT = REPOSITORY_ROOT / "profiler-output"
GENERATOR_JAR = REPOSITORY_ROOT / "generator.jar"
FLAMEGRAPH_SCRIPT = REPOSITORY_ROOT / "FlameGraph" / "flamegraph.pl"


class ProfileError(RuntimeError):
    """Expected setup or profiler failure."""


def find_java() -> str | None:
    java = shutil.which("java")
    if java:
        return java

    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        executable = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java")
        if executable.is_file():
            return str(executable)

    return None


def find_perl() -> str | None:
    perl = os.environ.get("PERL")
    if perl:
        configured = Path(perl)
        if configured.is_file():
            return str(configured)
        resolved = shutil.which(perl)
        if resolved:
            return resolved

    perl = shutil.which("perl")
    if perl:
        return perl

    if os.name == "nt":
        for candidate in (
            Path(r"C:\Strawberry\perl\bin\perl.exe"),
            Path(r"C:\Strawberry\bin\perl.exe"),
        ):
            if candidate.is_file():
                return str(candidate)

    return None


def last_output_line(result: subprocess.CompletedProcess[str]) -> str:
    output = (result.stderr or result.stdout or "").strip()
    if output:
        return output.splitlines()[-1]
    return f"process exited with code {result.returncode}"


def run_fight(scenario: Path, java: str) -> Path:
    PROFILER_OUTPUT.mkdir(parents=True, exist_ok=True)
    previous_runs = {
        path.name for path in PROFILER_OUTPUT.iterdir() if path.is_dir()
    }

    command = [java, "-jar", str(GENERATOR_JAR), str(scenario), "."]
    result = subprocess.run(
        command,
        cwd=REPOSITORY_ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode != 0:
        raise ProfileError(f"fight failed: {last_output_line(result)}")

    new_runs = [
        path
        for path in PROFILER_OUTPUT.iterdir()
        if path.is_dir() and path.name not in previous_runs
    ]
    if not new_runs:
        raise ProfileError("fight finished without creating a profiler output folder")

    return max(new_runs, key=lambda path: path.stat().st_mtime)


def render_graph(folded_file: Path, perl: str) -> None:
    svg_file = folded_file.with_suffix(".svg")
    command = [
        perl,
        str(FLAMEGRAPH_SCRIPT),
        "--width",
        "1920",
        "--countname",
        "OPs",
        "--colors",
        "wakeup",
        str(folded_file),
    ]

    try:
        with svg_file.open("w", encoding="utf-8", newline="\n") as output:
            result = subprocess.run(
                command,
                cwd=REPOSITORY_ROOT,
                stdout=output,
                stderr=subprocess.PIPE,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
    except OSError as error:
        raise ProfileError(f"cannot write {svg_file.name}: {error}") from error

    if result.returncode != 0:
        if svg_file.exists():
            svg_file.unlink()
        raise ProfileError(f"flamegraph failed for {folded_file.name}: {last_output_line(result)}")


def render_graphs(output_root: Path, current_run: Path, perl: str) -> tuple[int, int]:
    folded_files = sorted(output_root.rglob("*.folded"))
    current_files = [
        path for path in folded_files if current_run == path or current_run in path.parents
    ]
    if not current_files:
        raise ProfileError(f"no folded profiler files found in {current_run.name}")

    rendered = 0
    skipped = 0
    for folded_file in folded_files:
        svg_file = folded_file.with_suffix(".svg")
        is_current_run = current_run == folded_file or current_run in folded_file.parents
        if not is_current_run and svg_file.exists():
            skipped += 1
            continue
        render_graph(folded_file, perl)
        rendered += 1

    return rendered, skipped


def url_for(page: Path, target: Path) -> str:
    relative = os.path.relpath(target, page.parent)
    return "/".join(quote(part, safe="") for part in Path(relative).parts)


def escape(value: object) -> str:
    return html.escape(str(value), quote=True)


def page(title: str, body: str) -> str:
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{escape(title)}</title>
  <style>
    :root {{ color-scheme: dark; }}
    body {{
      background: #171717;
      color: #e8e8e8;
      font: 16px system-ui, sans-serif;
      margin: 0;
      padding: 24px;
    }}
    main {{ margin: 0 auto; max-width: 1920px; }}
    a {{ color: #80c7ff; }}
    li {{ margin: 8px 0; }}
    .meta {{ color: #aaa; }}
    .graph {{
      background: #242424;
      border: 1px solid #444;
      margin: 20px 0;
      overflow-x: auto;
      padding: 16px;
    }}
    .graph img {{
      background: white;
      display: block;
      height: auto;
      max-width: 100%;
    }}
  </style>
</head>
<body>
<main>
{body}
</main>
</body>
</html>
"""


def write_page(path: Path, title: str, body: str) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as output:
        output.write(page(title, body))


def turn_sort_key(path: Path) -> tuple[int, int | str]:
    if path.name.isdigit():
        return 0, int(path.name)
    return 1, path.name


def write_turn_index(turn_dir: Path, run_dir: Path, output_root: Path) -> None:
    index = turn_dir / "index.html"
    folded_files = sorted(turn_dir.glob("*.folded"), key=lambda path: path.name.lower())
    links = [
        f'<a href="{escape(url_for(index, run_dir / "index.html"))}">Run {escape(run_dir.name)}</a>',
        f'<a href="{escape(url_for(index, output_root / "index.html"))}">All runs</a>',
    ]
    body = [
        f"<p>{' · '.join(links)}</p>",
        f"<h1>{escape(run_dir.name)} / turn {escape(turn_dir.name)}</h1>",
    ]

    if not folded_files:
        body.append("<p class=\"meta\">No folded profiler files.</p>")

    for folded_file in folded_files:
        svg_file = folded_file.with_suffix(".svg")
        svg_url = url_for(index, svg_file)
        entity_name = escape(folded_file.stem)
        body.append(
            f"""<section class="graph">
  <h2>{entity_name}</h2>
  <p><a href="{escape(svg_url)}" target="_blank">Open SVG</a></p>
  <img src="{escape(svg_url)}" alt="{entity_name}" loading="lazy">
</section>"""
        )

    write_page(index, f"{run_dir.name} / turn {turn_dir.name}", "\n".join(body))


def write_run_index(run_dir: Path, output_root: Path) -> None:
    index = run_dir / "index.html"
    turn_dirs = sorted(
        (path for path in run_dir.iterdir() if path.is_dir()),
        key=turn_sort_key,
    )
    body = [
        f'<p><a href="{escape(url_for(index, output_root / "index.html"))}">All runs</a></p>',
        f"<h1>Profiler run {escape(run_dir.name)}</h1>",
        "<ul>",
    ]
    for turn_dir in turn_dirs:
        folded_count = len(list(turn_dir.glob("*.folded")))
        body.append(
            f'<li><a href="{escape(url_for(index, turn_dir / "index.html"))}">Turn '
            f'{escape(turn_dir.name)}</a> <span class="meta">({folded_count} entities)</span></li>'
        )
    body.append("</ul>")
    write_page(index, f"Profiler run {run_dir.name}", "\n".join(body))

    for turn_dir in turn_dirs:
        write_turn_index(turn_dir, run_dir, output_root)


def write_root_index(output_root: Path) -> Path:
    index = output_root / "index.html"
    run_dirs = sorted(
        (path for path in output_root.iterdir() if path.is_dir()),
        key=lambda path: path.name,
        reverse=True,
    )
    body = ["<h1>Profiler runs</h1>", "<ul>"]
    for run_dir in run_dirs:
        turn_count = len([path for path in run_dir.iterdir() if path.is_dir()])
        folded_count = len(list(run_dir.rglob("*.folded")))
        body.append(
            f'<li><a href="{escape(url_for(index, run_dir / "index.html"))}">'
            f'{escape(run_dir.name)}</a> <span class="meta">'
            f'({turn_count} turns, {folded_count} entities)</span></li>'
        )
    body.append("</ul>")
    write_page(index, "Profiler runs", "\n".join(body))

    for run_dir in run_dirs:
        write_run_index(run_dir, output_root)

    return index


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run fight profiler and open browsable flamegraph output."
    )
    parser.add_argument("scenario", type=Path, help="Path to fight scenario JSON")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    scenario = args.scenario.expanduser().resolve()
    if not scenario.is_file():
        raise ProfileError(f"scenario not found: {scenario}")
    if not GENERATOR_JAR.is_file():
        raise ProfileError("generator.jar not found; run .\\gradlew.bat jar first")
    if not FLAMEGRAPH_SCRIPT.is_file():
        raise ProfileError(f"FlameGraph script not found: {FLAMEGRAPH_SCRIPT}")

    java = find_java()
    if not java:
        raise ProfileError("java not found; add Java to PATH or set JAVA_HOME")
    perl = find_perl()
    if not perl:
        raise ProfileError("perl not found; add Strawberry Perl to PATH or set PERL")

    current_run = run_fight(scenario, java)
    rendered, skipped = render_graphs(PROFILER_OUTPUT, current_run, perl)
    index = write_root_index(PROFILER_OUTPUT)
    current_index = current_run / "index.html"
    webbrowser.open_new_tab(current_index.resolve().as_uri())

    print(f"Profiler run: {current_run.name}")
    print(f"Graphs generated: {rendered}; existing graphs kept: {skipped}")
    print(f"Browser page: {current_index}")
    print(f"All runs page: {index}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ProfileError as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(1)
