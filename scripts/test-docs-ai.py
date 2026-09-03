"""Check the published files and discovery links in a built MkDocs site."""

import sys
import unittest
from html.parser import HTMLParser
from pathlib import Path


class DocumentationPageDriver(HTMLParser):
    def __init__(self, path):
        super().__init__()
        self.reference_links = []
        self.feed(path.read_text())

    def handle_starttag(self, tag, attributes):
        attributes = dict(attributes)
        if tag == "link" and attributes.get("rel") == "describedby":
            self.reference_links.append((attributes.get("href"), attributes.get("type")))


class DocumentationSiteDriver:
    def __init__(self, directory):
        self.directory = directory

    def pages(self):
        return list(self.directory.rglob("*.html"))

    def reference(self, name):
        return (self.directory / name).read_bytes()

    def has_rendered_markdown_reference(self):
        return (self.directory / "llms" / "index.html").exists()


class DocumentationDiscoveryTest(unittest.TestCase):
    def test_every_page_links_to_the_same_raw_references_even_from_nested_paths(self):
        site = DocumentationSiteDriver(SITE_DIRECTORY)
        self.assertTrue(site.pages(), "Build the MkDocs site before running this test")
        for path in site.pages():
            with self.subTest(page=str(path.relative_to(SITE_DIRECTORY))):
                page = DocumentationPageDriver(path)
                self.assertEqual(page.reference_links, [
                    ("https://oneill9.github.io/tfl-mcp-server/llms.txt", "text/plain"),
                    ("https://oneill9.github.io/tfl-mcp-server/llms.md", "text/markdown"),
                ])

    def test_build_publishes_both_references_without_rendering_markdown_to_html(self):
        site = DocumentationSiteDriver(SITE_DIRECTORY)
        source_directory = Path(__file__).resolve().parent.parent / "docs"
        for name in ("llms.txt", "llms.md"):
            with self.subTest(reference=name):
                content = site.reference(name)
                self.assertEqual(content, (source_directory / name).read_bytes())
                self.assertTrue(content.startswith(b"# TfL MCP Server\n\n> "))
                self.assertNotIn(b"<html", content.lower())
        self.assertFalse(site.has_rendered_markdown_reference())
        self.assertIn(b"](https://oneill9.github.io/tfl-mcp-server/llms.md)",
                      site.reference("llms.txt"))
        for tool in (b"service_status", b"arrivals", b"journey", b"bike_points", b"crowding", b"fares"):
            self.assertIn(tool, site.reference("llms.md"))


if __name__ == "__main__":
    SITE_DIRECTORY = Path(sys.argv.pop(1)) if len(sys.argv) > 1 else Path("site")
    unittest.main()
