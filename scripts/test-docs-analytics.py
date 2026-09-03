"""Check the generated documentation before deployment."""

import re
import sys
import unittest
from html.parser import HTMLParser
from pathlib import Path


class DocumentationPageDriver(HTMLParser):
    def __init__(self, path):
        super().__init__()
        self.html = path.read_text()
        self.analytics_loaders = []
        self.feed(self.html)

    def handle_starttag(self, tag, attributes):
        attributes = dict(attributes)
        if tag == "script" and "googletagmanager.com/gtag/js" in attributes.get("src", ""):
            self.analytics_loaders.append(attributes)

    def analytics_configurations(self):
        return re.findall(r"gtag\('config',\s*'([^']+)',\s*\{([^}]+)\}\)", self.html)


class DocumentationAnalyticsTest(unittest.TestCase):
    def test_every_generated_page_uses_one_site_specific_tag_with_isolated_cookies(self):
        pages = list(SITE_DIRECTORY.rglob("*.html"))
        self.assertTrue(pages, "The MkDocs site must be built before running this test")
        for path in pages:
            with self.subTest(page=str(path.relative_to(SITE_DIRECTORY))):
                page = DocumentationPageDriver(path)
                self.assertEqual(len(page.analytics_loaders), 1)
                self.assertEqual(page.analytics_loaders[0]["src"],
                                 "https://www.googletagmanager.com/gtag/js?id=G-TZX1QK2613")
                self.assertIn("async", page.analytics_loaders[0])
                configurations = page.analytics_configurations()
                self.assertEqual(len(configurations), 1)
                self.assertEqual(configurations[0][0], "G-TZX1QK2613")
                self.assertIn("cookie_path: '/tfl-mcp-server/'", configurations[0][1])
                self.assertIn("cookie_prefix: 'tfl_mcp_server'", configurations[0][1])


if __name__ == "__main__":
    SITE_DIRECTORY = Path(sys.argv.pop(1)) if len(sys.argv) > 1 else Path("site")
    unittest.main()
