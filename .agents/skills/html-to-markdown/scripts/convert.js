const fs = require('fs');
const path = require('path');

function htmlToMarkdown(html) {
    // Extract main or body content if present
    const mainMatch = html.match(/<main[\s\S]*?<\/main>/i);
    let content = mainMatch ? mainMatch[0] : html;

    // Strip scripts, styles, svgs
    content = content.replace(/<script[\s\S]*?<\/script>/gi, '');
    content = content.replace(/<style[\s\S]*?<\/style>/gi, '');
    content = content.replace(/<svg[\s\S]*?<\/svg>/gi, '');

    // Replace pre/code blocks first to protect them
    const codeBlocks = [];
    content = content.replace(/<pre[^>]*><code(?: class="[^"]*language-([^"\s]+)[^"]*")?[^>]*>([\s\S]*?)<\/code><\/pre>/gi, (match, lang, code) => {
        const decoded = decodeEntities(code).trim();
        const placeholder = `___CODE_BLOCK_${codeBlocks.length}___`;
        codeBlocks.push(`\n\n\`\`\`${lang || ''}\n${decoded}\n\`\`\`\n\n`);
        return placeholder;
    });

    // Also pre blocks without code tag
    content = content.replace(/<pre[^>]*>([\s\S]*?)<\/pre>/gi, (match, code) => {
        const decoded = decodeEntities(code).trim();
        const placeholder = `___CODE_BLOCK_${codeBlocks.length}___`;
        codeBlocks.push(`\n\n\`\`\`\n${decoded}\n\`\`\`\n\n`);
        return placeholder;
    });

    // Tables
    content = content.replace(/<table[^>]*>([\s\S]*?)<\/table>/gi, (match, tableHtml) => {
        const rows = [];
        const trMatches = tableHtml.matchAll(/<tr[^>]*>([\s\S]*?)<\/tr>/gi);
        for (const tr of trMatches) {
            const cells = [];
            const cellMatches = tr[1].matchAll(/<t[hd][^>]*>([\s\S]*?)<\/t[hd]>/gi);
            for (const cell of cellMatches) {
                const text = cleanInline(cell[1]).replace(/\|/g, '\\|').trim();
                cells.push(text);
            }
            if (cells.length > 0) rows.push(cells);
        }
        if (rows.length === 0) return '';
        const colCount = Math.max(...rows.map(r => r.length));
        const formattedRows = rows.map(r => {
            while (r.length < colCount) r.push('');
            return '| ' + r.join(' | ') + ' |';
        });
        const headerSep = '| ' + Array(colCount).fill('---').join(' | ') + ' |';
        return '\n\n' + formattedRows[0] + '\n' + headerSep + '\n' + formattedRows.slice(1).join('\n') + '\n\n';
    });

    // Headings
    content = content.replace(/<h([1-6])[^>]*>([\s\S]*?)<\/h\1>/gi, (m, level, text) => {
        return `\n\n${'#'.repeat(parseInt(level))} ${cleanInline(text)}\n\n`;
    });

    // Definition lists
    content = content.replace(/<dt[^>]*>([\s\S]*?)<\/dt>\s*<dd[^>]*>([\s\S]*?)<\/dd>/gi, (m, dt, dd) => {
        return `\n- **${cleanInline(dt)}**: ${cleanInline(dd)}`;
    });

    // List items
    content = content.replace(/<li[^>]*>([\s\S]*?)<\/li>/gi, (m, text) => {
        return `\n- ${cleanInline(text)}`;
    });

    // Paragraphs and divs
    content = content.replace(/<p[^>]*>([\s\S]*?)<\/p>/gi, (m, text) => {
        return `\n\n${cleanInline(text)}\n\n`;
    });
    content = content.replace(/<br\s*\/?>/gi, '\n');
    content = content.replace(/<hr\s*\/?>/gi, '\n\n---\n\n');

    // Clean inline formatting
    content = cleanInline(content);

    // Restore code blocks
    codeBlocks.forEach((block, idx) => {
        content = content.replace(`___CODE_BLOCK_${idx}___`, block);
    });

    // Normalize spacing
    content = content.replace(/\n{3,}/g, '\n\n').trim();
    return content + '\n';
}

function cleanInline(text) {
    let s = text;
    s = s.replace(/<strong[^>]*>([\s\S]*?)<\/strong>/gi, '**$1**');
    s = s.replace(/<b[^>]*>([\s\S]*?)<\/b>/gi, '**$1**');
    s = s.replace(/<em[^>]*>([\s\S]*?)<\/em>/gi, '*$1*');
    s = s.replace(/<i[^>]*>([\s\S]*?)<\/i>/gi, '*$1*');
    s = s.replace(/<code[^>]*>([\s\S]*?)<\/code>/gi, '`$1`');
    s = s.replace(/<a\s+[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>/gi, (m, href, linkText) => {
        const txt = decodeEntities(linkText.replace(/<[^>]+>/g, '')).trim();
        return `[${txt || href}](${href})`;
    });
    s = s.replace(/<[^>]+>/g, ' ');
    s = decodeEntities(s);
    s = s.replace(/[ \t]+/g, ' ');
    return s;
}

function decodeEntities(str) {
    return str
        .replace(/&amp;/g, '&')
        .replace(/&lt;/g, '<')
        .replace(/&gt;/g, '>')
        .replace(/&quot;/g, '"')
        .replace(/&#39;/g, "'")
        .replace(/&nbsp;/g, ' ');
}

if (process.argv.length < 4) {
    console.error('Usage: node convert.js <input.html> <output.md>');
    process.exit(1);
}

const inPath = path.resolve(process.argv[2]);
const outPath = path.resolve(process.argv[3]);

const html = fs.readFileSync(inPath, 'utf8');
const md = htmlToMarkdown(html);
fs.writeFileSync(outPath, md, 'utf8');
console.log(`Converted ${inPath} -> ${outPath} (${md.length} chars)`);

