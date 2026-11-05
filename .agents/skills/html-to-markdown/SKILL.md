---
name: html-to-markdown
description: Fast HTML to Markdown converter using Node.js with zero external dependencies.
---

# HTML to Markdown Converter

Converts HTML documents (such as lab instructions, guides, and web pages) into clean, standard GitHub-flavored Markdown.

## When to Use

Use this skill whenever an HTML file needs to be converted or rendered into a `.md` file.

## Usage

Run the bundled conversion script:

```bash
node .agents/skills/html-to-markdown/scripts/convert.js <input.html> <output.md>
```

### Features

- Zero external dependencies (uses Node.js standard runtime).
- Converts headings (`h1`-`h6`), paragraphs, blockquotes, lists (`ul`, `ol`), definition lists, tables, bold/italic, and inline code.
- Detects and preserves fenced code blocks (`<pre><code>`) with syntax highlighting languages.
- Automatically extracts main content containers (`<main>`) while removing sidebars, scripts, and stylesheets.
