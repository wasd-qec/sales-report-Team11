import sys
import re
from html.parser import HTMLParser
from html import unescape
from pathlib import Path

class HTMLToMarkdownParser(HTMLParser):
    def __init__(self, target_tag='main'):
        super().__init__()
        self.target_tag = target_tag
        self.in_target = target_tag is None
        self.target_depth = 0
        
        self.output = []
        self.tags = []
        self.in_pre = False
        self.in_code = False
        self.code_lang = ""
        self.current_href = None
        self.link_text = []
        
        self.list_stack = [] # 'ul' or ('ol', counter)
        
        # Table handling
        self.in_table = False
        self.table_rows = []
        self.current_row = []
        self.current_cell = []
        self.is_header_cell = False

        # Ignored tags
        self.ignore_tags = {'style', 'script', 'svg', 'noscript'}
        self.ignore_depth = 0

    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        
        if tag in self.ignore_tags:
            self.ignore_depth += 1
            return
        if self.ignore_depth > 0:
            return

        if not self.in_target:
            if tag == self.target_tag or attrs_dict.get('id') == 'top' or 'main' in attrs_dict.get('class', ''):
                self.in_target = True
                self.target_depth = 1
                return
            else:
                return
        else:
            if tag == self.target_tag:
                self.target_depth += 1

        self.tags.append(tag)

        if tag in ('h1', 'h2', 'h3', 'h4', 'h5', 'h6'):
            level = int(tag[1])
            self._ensure_newline(2)
            self.output.append('#' * level + ' ')
        elif tag == 'p':
            self._ensure_newline(2)
        elif tag == 'br':
            self.output.append('\n')
        elif tag == 'hr':
            self._ensure_newline(2)
            self.output.append('---\n\n')
        elif tag == 'pre':
            self.in_pre = True
            self._ensure_newline(2)
            self.output.append('```')
        elif tag == 'code':
            self.in_code = True
            if self.in_pre:
                cls = attrs_dict.get('class', '')
                match = re.search(r'language-([a-zA-Z0-9_\-]+)', cls)
                if match:
                    self.code_lang = match.group(1)
                elif 'mermaid' in cls:
                    self.code_lang = 'mermaid'
                self.output.append(self.code_lang + '\n')
            else:
                self.output.append('`')
        elif tag in ('strong', 'b'):
            self.output.append('**')
        elif tag in ('em', 'i'):
            self.output.append('*')
        elif tag == 'a':
            self.current_href = attrs_dict.get('href', '')
            self.link_text = []
        elif tag == 'ul':
            self._ensure_newline()
            self.list_stack.append('ul')
        elif tag == 'ol':
            self._ensure_newline()
            self.list_stack.append(['ol', 1])
        elif tag == 'li':
            self._ensure_newline()
            indent = '  ' * (len(self.list_stack) - 1)
            if self.list_stack and isinstance(self.list_stack[-1], list):
                num = self.list_stack[-1][1]
                self.list_stack[-1][1] += 1
                self.output.append(f"{indent}{num}. ")
            else:
                self.output.append(f"{indent}- ")
        elif tag == 'blockquote':
            self._ensure_newline(2)
            self.output.append('> ')
        elif tag == 'table':
            self._ensure_newline(2)
            self.in_table = True
            self.table_rows = []
        elif tag == 'tr':
            self.current_row = []
        elif tag in ('th', 'td'):
            self.current_cell = []
            self.is_header_cell = (tag == 'th')

    def handle_endtag(self, tag):
        if tag in self.ignore_tags:
            if self.ignore_depth > 0:
                self.ignore_depth -= 1
            return
        if self.ignore_depth > 0:
            return

        if not self.in_target:
            return

        if tag == self.target_tag:
            self.target_depth -= 1
            if self.target_depth <= 0:
                self.in_target = False
                return

        if self.tags and self.tags[-1] == tag:
            self.tags.pop()

        if tag in ('h1', 'h2', 'h3', 'h4', 'h5', 'h6'):
            self.output.append('\n\n')
        elif tag == 'p':
            self.output.append('\n\n')
        elif tag == 'pre':
            self.in_pre = False
            self.code_lang = ""
            self._ensure_newline()
            self.output.append('```\n\n')
        elif tag == 'code':
            self.in_code = False
            if not self.in_pre:
                self.output.append('`')
        elif tag in ('strong', 'b'):
            self.output.append('**')
        elif tag in ('em', 'i'):
            self.output.append('*')
        elif tag == 'a':
            if self.current_href:
                text = "".join(self.link_text).strip()
                if not text:
                    text = self.current_href
                # Replace with Markdown link
                self.output.append(f"[{text}]({self.current_href})")
                self.current_href = None
                self.link_text = []
        elif tag in ('ul', 'ol'):
            if self.list_stack:
                self.list_stack.pop()
            self.output.append('\n')
        elif tag == 'li':
            self.output.append('\n')
        elif tag == 'tr':
            if self.current_row is not None:
                self.table_rows.append(self.current_row)
                self.current_row = []
        elif tag in ('th', 'td'):
            cell_text = "".join(self.current_cell).strip().replace('\n', ' ')
            self.current_row.append((cell_text, self.is_header_cell))
            self.current_cell = []
        elif tag == 'table':
            self.in_table = False
            self._render_table()

    def handle_data(self, data):
        if self.ignore_depth > 0 or not self.in_target:
            return

        if self.in_table and self.current_row is not None and (self.tags and self.tags[-1] in ('th', 'td', 'code', 'strong', 'em', 'span', 'a')):
            self.current_cell.append(data)
            return

        if self.current_href is not None:
            self.link_text.append(data)
            return

        if self.in_pre:
            self.output.append(data)
        else:
            text = re.sub(r'[ \t\r\n]+', ' ', data)
            if text:
                self.output.append(text)

    def _ensure_newline(self, count=1):
        trailing = 0
        for item in reversed(self.output):
            for ch in reversed(item):
                if ch == '\n':
                    trailing += 1
                else:
                    break
            if trailing > 0 or item != '':
                break
        if trailing < count:
            self.output.append('\n' * (count - trailing))

    def _render_table(self):
        if not self.table_rows:
            return
        col_count = max(len(row) for row in self.table_rows)
        if col_count == 0:
            return
        
        has_header = any(is_h for cell, is_h in self.table_rows[0])
        header_row = self.table_rows[0]
        rows = self.table_rows[1:] if has_header else self.table_rows
        
        header_cols = [cell for cell, _ in header_row] + [''] * (col_count - len(header_row))
        self.output.append('| ' + ' | '.join(header_cols) + ' |\n')
        self.output.append('| ' + ' | '.join(['---'] * col_count) + ' |\n')
        for row in rows:
            cols = [cell for cell, _ in row] + [''] * (col_count - len(row))
            self.output.append('| ' + ' | '.join(cols) + ' |\n')
        self.output.append('\n')

    def get_markdown(self):
        raw = "".join(self.output)
        # Clean up excessive newlines
        clean = re.sub(r'\n{3,}', '\n\n', raw)
        return unescape(clean).strip() + '\n'

def convert_html_to_markdown(html_content):
    parser = HTMLToMarkdownParser(target_tag='main')
    parser.feed(html_content)
    md = parser.get_markdown()
    if not md or len(md) < 50:
        # fallback without restricting to <main>
        parser = HTMLToMarkdownParser(target_tag=None)
        parser.feed(html_content)
        md = parser.get_markdown()
    return md

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage: python convert.py <input.html> <output.md>")
        sys.exit(1)
    
    in_path = Path(sys.argv[1])
    out_path = Path(sys.argv[2])
    
    html = in_path.read_text(encoding='utf-8')
    md = convert_html_to_markdown(html)
    out_path.write_text(md, encoding='utf-8')
    print(f"Converted {in_path} -> {out_path} ({len(md)} chars)")

