# Convert Markdown -> DOCX using Word COM automation
# Renders the markdown to HTML first, then opens in Word and saves as .docx

param(
    [string]$MdPath = "C:\AI projects\dummby-project\Interview_Scheduler_Complete_Documentation.md",
    [string]$DocxPath = "C:\AI projects\dummby-project\Interview_Scheduler_Complete_Documentation.docx"
)

# --- 1. Lightweight Markdown -> HTML converter (good enough for our doc) ---
function Convert-MdToHtml([string]$md) {
    $lines = $md -split "`r?`n"
    $html = New-Object System.Collections.ArrayList
    [void]$html.Add('<html><head><meta charset="utf-8"><style>
        body { font-family: "Plus Jakarta Sans", "Segoe UI", Arial, sans-serif; line-height: 1.55; color: #1e293b; }
        h1 { color: #1e293b; border-bottom: 3px solid #4f46e5; padding-bottom: 8px; margin-top: 24px; font-size: 26pt; }
        h2 { color: #4f46e5; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; margin-top: 20px; font-size: 18pt; }
        h3 { color: #334155; margin-top: 16px; font-size: 14pt; }
        h4 { color: #475569; font-size: 12pt; }
        table { border-collapse: collapse; margin: 12px 0; width: 100%; }
        th { background: #4f46e5; color: white; padding: 6px 10px; text-align: left; border: 1px solid #4338ca; }
        td { padding: 5px 10px; border: 1px solid #cbd5e1; vertical-align: top; }
        tr:nth-child(even) td { background: #f8fafc; }
        code { background: #f1f5f9; padding: 1px 5px; border-radius: 3px; font-family: Consolas, "Cascadia Code", monospace; color: #be123c; font-size: 10pt; }
        pre { background: #0f172a; color: #e2e8f0; padding: 14px; border-radius: 6px; overflow-x: auto; font-family: Consolas, monospace; font-size: 9.5pt; line-height: 1.4; }
        pre code { background: transparent; color: inherit; padding: 0; }
        blockquote { border-left: 4px solid #4f46e5; padding: 6px 14px; background: #eef2ff; color: #312e81; margin: 12px 0; }
        ul, ol { margin: 8px 0; padding-left: 24px; }
        li { margin: 3px 0; }
        hr { border: 0; border-top: 1px solid #cbd5e1; margin: 18px 0; }
        a { color: #4f46e5; text-decoration: none; }
        strong { color: #0f172a; }
    </style></head><body>')

    $inCodeBlock = $false
    $inTable = $false
    $tableHeader = $false
    $inList = $false
    $listType = ""

    foreach ($raw in $lines) {
        $line = $raw

        # Fenced code blocks
        if ($line -match '^```') {
            if ($inCodeBlock) {
                [void]$html.Add('</code></pre>')
                $inCodeBlock = $false
            } else {
                if ($inList) { [void]$html.Add("</$listType>"); $inList = $false }
                $lang = ($line -replace '^```', '').Trim()
                [void]$html.Add('<pre><code>')
                $inCodeBlock = $true
            }
            continue
        }
        if ($inCodeBlock) {
            $escaped = $line.Replace('&','&amp;').Replace('<','&lt;').Replace('>','&gt;')
            [void]$html.Add($escaped)
            continue
        }

        # Tables (simple: detect | ... | and separator | --- |)
        if ($line -match '^\s*\|') {
            $cells = ($line.Trim().Trim('|') -split '\|') | ForEach-Object { $_.Trim() }
            # detect separator row
            if ($cells -and ($cells -join '') -match '^[-:\s]+$') {
                # skip separator; mark next rows as body
                $tableHeader = $false
                continue
            }
            if (-not $inTable) {
                if ($inList) { [void]$html.Add("</$listType>"); $inList = $false }
                [void]$html.Add('<table>')
                $inTable = $true
                $tableHeader = $true
            }
            $tag = if ($tableHeader) { 'th' } else { 'td' }
            $row = '<tr>'
            foreach ($c in $cells) {
                $cellHtml = Format-Inline $c
                $row += "<$tag>$cellHtml</$tag>"
            }
            $row += '</tr>'
            [void]$html.Add($row)
            $tableHeader = $false
            continue
        } elseif ($inTable) {
            [void]$html.Add('</table>')
            $inTable = $false
        }

        # Headings
        if ($line -match '^(#{1,6})\s+(.*)$') {
            if ($inList) { [void]$html.Add("</$listType>"); $inList = $false }
            $level = $matches[1].Length
            $text  = Format-Inline $matches[2]
            [void]$html.Add("<h$level>$text</h$level>")
            continue
        }

        # Horizontal rule
        if ($line -match '^---+\s*$') {
            if ($inList) { [void]$html.Add("</$listType>"); $inList = $false }
            [void]$html.Add('<hr/>')
            continue
        }

        # Blockquote
        if ($line -match '^>\s?(.*)$') {
            if ($inList) { [void]$html.Add("</$listType>"); $inList = $false }
            [void]$html.Add("<blockquote>$(Format-Inline $matches[1])</blockquote>")
            continue
        }

        # Lists
        if ($line -match '^\s*[-*]\s+(.*)$') {
            if (-not $inList -or $listType -ne 'ul') {
                if ($inList) { [void]$html.Add("</$listType>") }
                [void]$html.Add('<ul>'); $inList = $true; $listType = 'ul'
            }
            [void]$html.Add("<li>$(Format-Inline $matches[1])</li>")
            continue
        }
        if ($line -match '^\s*\d+\.\s+(.*)$') {
            if (-not $inList -or $listType -ne 'ol') {
                if ($inList) { [void]$html.Add("</$listType>") }
                [void]$html.Add('<ol>'); $inList = $true; $listType = 'ol'
            }
            [void]$html.Add("<li>$(Format-Inline $matches[1])</li>")
            continue
        }
        if ($inList -and $line.Trim() -eq '') {
            [void]$html.Add("</$listType>")
            $inList = $false
            continue
        }

        # Paragraph
        if ($line.Trim() -ne '') {
            [void]$html.Add("<p>$(Format-Inline $line)</p>")
        }
    }

    if ($inList) { [void]$html.Add("</$listType>") }
    if ($inTable) { [void]$html.Add('</table>') }
    if ($inCodeBlock) { [void]$html.Add('</code></pre>') }
    [void]$html.Add('</body></html>')
    return ($html -join "`r`n")
}

function Format-Inline([string]$text) {
    # Inline code
    $text = [regex]::Replace($text, '`([^`]+)`', { param($m) "<code>$(($m.Groups[1].Value).Replace('<','&lt;').Replace('>','&gt;'))</code>" })
    # Bold **x**
    $text = [regex]::Replace($text, '\*\*([^\*]+)\*\*', '<strong>$1</strong>')
    # Italic *x*  (avoid bold leftovers)
    $text = [regex]::Replace($text, '(?<!\*)\*([^\*\n]+)\*(?!\*)', '<em>$1</em>')
    # Links [text](url)
    $text = [regex]::Replace($text, '\[([^\]]+)\]\(([^)]+)\)', '<a href="$2">$1</a>')
    return $text
}

Write-Host "Reading markdown..."
$md = Get-Content -Raw -Path $MdPath -Encoding UTF8
Write-Host "Converting to HTML..."
$html = Convert-MdToHtml $md
$tmpHtml = [System.IO.Path]::GetTempFileName() + '.html'
Set-Content -Path $tmpHtml -Value $html -Encoding UTF8

Write-Host "Opening Word..."
$word = New-Object -ComObject Word.Application
$word.Visible = $false
try {
    $doc = $word.Documents.Open($tmpHtml)
    # 16 = wdFormatDocumentDefault (.docx)
    $doc.SaveAs([ref]$DocxPath, [ref]16)
    $doc.Close()
    Write-Host "Saved: $DocxPath"
} finally {
    $word.Quit()
    [System.Runtime.Interopservices.Marshal]::ReleaseComObject($word) | Out-Null
    Remove-Item $tmpHtml -ErrorAction SilentlyContinue
}

