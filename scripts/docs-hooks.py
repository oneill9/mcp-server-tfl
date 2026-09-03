"""Publish the project reference as raw Markdown instead of a rendered page."""

from pathlib import Path
from shutil import copyfile


def on_post_build(config, **kwargs):
    # MkDocs excludes this file from page rendering; copy it after every build,
    # including the build performed by gh-deploy.
    copyfile(Path(config["docs_dir"]) / "llms.md", Path(config["site_dir"]) / "llms.md")
