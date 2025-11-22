import java.io.File

tasks.register("generateDocsHtml") {
    group = "documentation"
    description = "Generate HTML files from Markdown documentation"
    
    val docsDir = file("docs")
    val mdFiles = fileTree(docsDir) {
        include("**/*.md")
        exclude("**/node_modules/**")
    }
    
    inputs.files(mdFiles)
    outputs.dir(docsDir)
    
    doLast {
        mdFiles.forEach { mdFile ->
            val htmlFile = File(mdFile.parent, mdFile.nameWithoutExtension + ".html")
            
            // Read markdown
            val markdown = mdFile.readText()
            
            // Convert markdown to HTML using simple parser
            val htmlContent = markdownToHtml(markdown)
            
            // Read HTML template
            val template = getHtmlTemplate(mdFile.nameWithoutExtension)
            
            // Replace content in template
            val finalHtml = template.replace("{{CONTENT}}", htmlContent)
            
            // Write HTML file
            htmlFile.writeText(finalHtml)
            
            println("Generated: ${htmlFile.relativeTo(projectDir)}")
        }
    }
}

fun markdownToHtml(markdown: String): String {
    val lines = markdown.lines()
    val html = StringBuilder()
    var inCodeBlock = false
    var codeBlockLanguage = ""
    var inList = false
    var listType = ""
    
    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()
        
        // Code blocks
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                html.append("</code></pre>\n")
                inCodeBlock = false
            } else {
                codeBlockLanguage = trimmed.substring(3).trim()
                html.append("<pre><code${if (codeBlockLanguage.isNotEmpty()) " class=\"language-$codeBlockLanguage\"" else ""}>")
                inCodeBlock = true
            }
            return@forEachIndexed
        }
        
        if (inCodeBlock) {
            html.append(escapeHtml(line)).append("\n")
            return@forEachIndexed
        }
        
        // Headers
        when {
            trimmed.startsWith("# ") -> {
                html.append("<h1>${trimmed.substring(2)}</h1>\n")
                inList = false
            }
            trimmed.startsWith("## ") -> {
                html.append("<h2>${trimmed.substring(3)}</h2>\n")
                inList = false
            }
            trimmed.startsWith("### ") -> {
                html.append("<h3>${trimmed.substring(4)}</h3>\n")
                inList = false
            }
            trimmed.startsWith("#### ") -> {
                html.append("<h4>${trimmed.substring(5)}</h4>\n")
                inList = false
            }
            trimmed == "---" || trimmed == "***" -> {
                html.append("<hr />\n")
                inList = false
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                if (!inList || listType != "ul") {
                    if (inList) html.append("</$listType>\n")
                    html.append("<ul>\n")
                    inList = true
                    listType = "ul"
                }
                html.append("<li>${processInlineMarkdown(trimmed.substring(2))}</li>\n")
            }
            trimmed.matches(Regex("^\\d+\\. ")) -> {
                if (!inList || listType != "ol") {
                    if (inList) html.append("</$listType>\n")
                    html.append("<ol>\n")
                    inList = true
                    listType = "ol"
                }
                val content = trimmed.replaceFirst(Regex("^\\d+\\. "), "")
                html.append("<li>${processInlineMarkdown(content)}</li>\n")
            }
            trimmed.isEmpty() -> {
                if (inList) {
                    html.append("</$listType>\n")
                    inList = false
                }
                html.append("\n")
            }
            trimmed.startsWith("> ") -> {
                html.append("<blockquote>${processInlineMarkdown(trimmed.substring(2))}</blockquote>\n")
                inList = false
            }
            else -> {
                if (inList) {
                    html.append("</$listType>\n")
                    inList = false
                }
                html.append("<p>${processInlineMarkdown(trimmed)}</p>\n")
            }
        }
    }
    
    if (inList) {
        html.append("</$listType>\n")
    }
    if (inCodeBlock) {
        html.append("</code></pre>\n")
    }
    
    return html.toString()
}

fun processInlineMarkdown(text: String): String {
    var result = text
    // Bold **text**
    result = result.replace(Regex("\\*\\*(.+?)\\*\\*")) { "<strong>${it.groupValues[1]}</strong>" }
    // Italic *text*
    result = result.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)")) { "<em>${it.groupValues[1]}</em>" }
    // Code `code`
    result = result.replace(Regex("`(.+?)`")) { "<code>${escapeHtml(it.groupValues[1])}</code>" }
    // Links [text](url)
    result = result.replace(Regex("\\[([^\\]]+)\\]\\(([^\\)]+)\\)")) { 
        "<a href=\"${it.groupValues[2]}\">${it.groupValues[1]}</a>" 
    }
    // Images ![alt](url)
    result = result.replace(Regex("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)")) {
        "<img src=\"${it.groupValues[2]}\" alt=\"${it.groupValues[1]}\" />"
    }
    return result
}

fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}

fun getHtmlTemplate(pageTitle: String): String {
    val title = pageTitle.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
    return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$title - Flagship</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        body {
            font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.7;
            color: #1a1a1a;
            background: #ffffff;
        }

        .header {
            background: #f7f7f7;
            border-bottom: 1px solid #e0e0e0;
            padding: 20px 0;
        }

        .header-content {
            max-width: 900px;
            margin: 0 auto;
            padding: 0 40px;
            display: flex;
            align-items: center;
            gap: 20px;
        }

        .logo { width: 40px; height: 40px; }

        .header-title {
            font-size: 20px;
            font-weight: 600;
            color: #1a1a1a;
            text-decoration: none;
        }

        .header-title:hover { color: #00d687; }

        .back-link {
            margin-left: auto;
            color: #666;
            text-decoration: none;
            font-size: 14px;
        }

        .back-link:hover { color: #00d687; }

        .content {
            max-width: 900px;
            margin: 60px auto;
            padding: 0 40px;
        }

        h1 {
            font-size: 42px;
            font-weight: 700;
            margin-bottom: 20px;
            color: #1a1a1a;
            padding-bottom: 20px;
            border-bottom: 2px solid #e0e0e0;
        }

        h2 {
            font-size: 32px;
            font-weight: 600;
            margin: 50px 0 20px;
            color: #1a1a1a;
        }

        h3 {
            font-size: 24px;
            font-weight: 600;
            margin: 40px 0 16px;
            color: #1a1a1a;
        }

        h4 {
            font-size: 18px;
            font-weight: 600;
            margin: 30px 0 12px;
            color: #333;
        }

        p {
            margin: 16px 0;
            color: #333;
        }

        a {
            color: #00d687;
            text-decoration: none;
        }

        a:hover {
            text-decoration: underline;
        }

        code {
            background: #f7f7f7;
            padding: 2px 8px;
            border-radius: 4px;
            font-family: 'SF Mono', Monaco, 'Courier New', monospace;
            font-size: 14px;
            color: #ef7200;
        }

        pre {
            background: #f7f7f7;
            border: 1px solid #e0e0e0;
            border-radius: 8px;
            padding: 20px;
            overflow-x: auto;
            margin: 20px 0;
        }

        pre code {
            background: none;
            padding: 0;
            color: #1a1a1a;
            font-size: 14px;
            line-height: 1.6;
        }

        ul, ol {
            margin: 16px 0 16px 30px;
        }

        li {
            margin: 8px 0;
        }

        hr {
            border: none;
            border-top: 1px solid #e0e0e0;
            margin: 60px 0;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }

        th, td {
            border: 1px solid #e0e0e0;
            padding: 12px;
            text-align: left;
        }

        th {
            background: #f7f7f7;
            font-weight: 600;
        }

        blockquote {
            border-left: 3px solid #00d687;
            padding-left: 20px;
            margin: 20px 0;
            color: #666;
            font-style: italic;
        }

        img {
            max-width: 100%;
            height: auto;
            border-radius: 8px;
        }

        footer {
            max-width: 900px;
            margin: 80px auto 40px;
            padding: 40px 40px 0;
            border-top: 1px solid #e0e0e0;
            text-align: center;
            color: #666;
            font-size: 14px;
        }

        footer a {
            color: #00d687;
            text-decoration: none;
        }

        footer a:hover {
            text-decoration: underline;
        }

        @media (max-width: 768px) {
            .content, .header-content, footer {
                padding-left: 20px;
                padding-right: 20px;
            }

            h1 { font-size: 32px; }
            h2 { font-size: 26px; }
            h3 { font-size: 20px; }
        }
    </style>
</head>
<body>
    <header class="header">
        <div class="header-content">
            <img src="images/flagship_icon.svg" alt="Flagship Logo" class="logo">
            <a href="index.html" class="header-title">Flagship</a>
            <a href="index.html" class="back-link">← Back to Documentation</a>
        </div>
    </header>
    <div class="content">
{{CONTENT}}
    </div>
    <footer>
        <p>Made with ❤️ by <a href="https://github.com/maxluxs" target="_blank">@maxluxs</a> • Licensed under MIT</p>
    </footer>
</body>
</html>
    """.trimIndent()
}
