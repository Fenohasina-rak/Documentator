package org.acme.Helpers;


import jakarta.enterprise.context.ApplicationScoped;
import org.acme.Models.File.ProjectFile;
import org.acme.Models.File.ProjectStructure;
import org.jboss.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@ApplicationScoped
public class HtmlAssembler {

    public String assemble(ProjectStructure structure, String architectureHtml,
                           String apiHtml, String erdHtml) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm:ss"));
        String fileTree = buildFileTree(structure);

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s — Code Documentation</title>
    <style>
        :root {
            --bg-primary: #0d0f12;
            --bg-secondary: #13171e;
            --bg-card: #1a1f29;
            --bg-card-hover: #1e2535;
            --border: #2a3040;
            --accent: #4f9cf9;
            --accent-glow: rgba(79, 156, 249, 0.15);
            --accent-2: #7c5cbf;
            --accent-3: #2eb87e;
            --accent-4: #e8634a;
            --text-primary: #e2e8f0;
            --text-secondary: #8892a4;
            --text-muted: #4a5568;
            --code-bg: #0a0c10;
            --sidebar-width: 280px;
            --header-height: 64px;
        }

        * { margin: 0; padding: 0; box-sizing: border-box; }

        html { scroll-behavior: smooth; }

        body {
            font-family: 'Georgia', 'Times New Roman', serif;
            background: var(--bg-primary);
            color: var(--text-primary);
            line-height: 1.7;
            min-height: 100vh;
        }

        /* ── TOP HEADER ── */
        .top-header {
            position: fixed;
            top: 0; left: 0; right: 0;
            height: var(--header-height);
            background: rgba(13, 15, 18, 0.92);
            backdrop-filter: blur(12px);
            border-bottom: 1px solid var(--border);
            display: flex;
            align-items: center;
            padding: 0 32px;
            z-index: 100;
            gap: 20px;
        }

        .header-logo {
            font-family: 'Courier New', monospace;
            font-size: 13px;
            font-weight: 700;
            color: var(--accent);
            letter-spacing: 0.1em;
            text-transform: uppercase;
            opacity: 0.9;
        }

        .header-divider {
            width: 1px;
            height: 24px;
            background: var(--border);
        }

        .header-project {
            font-family: 'Georgia', serif;
            font-size: 15px;
            color: var(--text-primary);
            font-weight: normal;
        }

        .header-meta {
            margin-left: auto;
            font-family: 'Courier New', monospace;
            font-size: 11px;
            color: var(--text-muted);
        }

        /* ── SIDEBAR ── */
        .sidebar {
            position: fixed;
            top: var(--header-height);
            left: 0;
            bottom: 0;
            width: var(--sidebar-width);
            background: var(--bg-secondary);
            border-right: 1px solid var(--border);
            overflow-y: auto;
            z-index: 50;
            padding: 24px 0;
        }

        .sidebar::-webkit-scrollbar { width: 4px; }
        .sidebar::-webkit-scrollbar-track { background: transparent; }
        .sidebar::-webkit-scrollbar-thumb { background: var(--border); border-radius: 2px; }

        .sidebar-section {
            padding: 0 20px;
            margin-bottom: 28px;
        }

        .sidebar-label {
            font-family: 'Courier New', monospace;
            font-size: 10px;
            font-weight: 700;
            letter-spacing: 0.15em;
            text-transform: uppercase;
            color: var(--text-muted);
            margin-bottom: 10px;
            padding-left: 4px;
        }

        .sidebar-nav a {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            border-radius: 6px;
            color: var(--text-secondary);
            text-decoration: none;
            font-size: 13.5px;
            transition: all 0.15s ease;
            margin-bottom: 2px;
        }

        .sidebar-nav a:hover,
        .sidebar-nav a.active {
            background: var(--accent-glow);
            color: var(--accent);
        }

        .sidebar-nav .icon {
            font-size: 14px;
            width: 18px;
            text-align: center;
            flex-shrink: 0;
        }

        /* ── MAIN CONTENT ── */
        .main {
            margin-left: var(--sidebar-width);
            margin-top: var(--header-height);
            padding: 48px 56px 80px;
            max-width: 1100px;
        }

        /* ── HERO SECTION ── */
        .hero {
            margin-bottom: 64px;
            padding-bottom: 48px;
            border-bottom: 1px solid var(--border);
        }

        .hero-eyebrow {
            font-family: 'Courier New', monospace;
            font-size: 11px;
            letter-spacing: 0.2em;
            text-transform: uppercase;
            color: var(--accent);
            margin-bottom: 16px;
        }

        .hero-title {
            font-size: 48px;
            font-weight: normal;
            line-height: 1.15;
            color: var(--text-primary);
            margin-bottom: 20px;
            letter-spacing: -0.02em;
        }

        .hero-title em {
            font-style: italic;
            color: var(--accent);
        }

        .hero-subtitle {
            font-size: 16px;
            color: var(--text-secondary);
            max-width: 600px;
            margin-bottom: 36px;
        }

        .hero-stats {
            display: flex;
            gap: 32px;
            flex-wrap: wrap;
        }

        .stat {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .stat-value {
            font-family: 'Courier New', monospace;
            font-size: 28px;
            font-weight: 700;
            color: var(--text-primary);
            letter-spacing: -0.02em;
        }

        .stat-label {
            font-size: 12px;
            color: var(--text-muted);
            text-transform: uppercase;
            letter-spacing: 0.08em;
        }

        /* ── DOC SECTIONS ── */
        .doc-section {
            margin-bottom: 72px;
            scroll-margin-top: calc(var(--header-height) + 24px);
        }

        .section-header {
            display: flex;
            align-items: center;
            gap: 16px;
            margin-bottom: 32px;
            padding-bottom: 20px;
            border-bottom: 1px solid var(--border);
        }

        .section-icon {
            width: 44px;
            height: 44px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;
        }

        .section-icon.arch { background: rgba(79, 156, 249, 0.12); }
        .section-icon.api  { background: rgba(46, 184, 126, 0.12); }
        .section-icon.erd  { background: rgba(124, 92, 191, 0.12); }
        .section-icon.files { background: rgba(232, 99, 74, 0.12); }

        .section-title {
            font-size: 28px;
            font-weight: normal;
            letter-spacing: -0.01em;
        }

        .section-title.arch { color: var(--accent); }
        .section-title.api  { color: var(--accent-3); }
        .section-title.erd  { color: var(--accent-2); }
        .section-title.files { color: var(--accent-4); }

        .section-desc {
            font-size: 13px;
            color: var(--text-muted);
            margin-top: 2px;
        }

        /* ── CONTENT STYLES (applied to Gemini output) ── */
        .doc-content h2 {
            font-size: 20px;
            font-weight: normal;
            color: var(--text-primary);
            margin: 32px 0 12px;
            letter-spacing: -0.01em;
        }

        .doc-content h3 {
            font-size: 15px;
            font-weight: 700;
            color: var(--text-secondary);
            margin: 24px 0 10px;
            text-transform: uppercase;
            letter-spacing: 0.06em;
        }

        .doc-content p {
            color: var(--text-secondary);
            margin-bottom: 14px;
            font-size: 15px;
        }

        .doc-content ul, .doc-content ol {
            margin: 12px 0 18px 20px;
            color: var(--text-secondary);
            font-size: 15px;
        }

        .doc-content li { margin-bottom: 6px; }

        .doc-content table {
            width: 100%%;
            border-collapse: collapse;
            margin: 20px 0 28px;
            font-size: 13.5px;
            font-family: 'Courier New', monospace;
        }

        .doc-content th {
            background: var(--bg-card);
            color: var(--text-primary);
            padding: 10px 14px;
            text-align: left;
            border: 1px solid var(--border);
            font-size: 11px;
            text-transform: uppercase;
            letter-spacing: 0.08em;
        }

        .doc-content td {
            padding: 9px 14px;
            border: 1px solid var(--border);
            color: var(--text-secondary);
            vertical-align: top;
        }

        .doc-content tr:hover td { background: var(--bg-card); }

        .doc-content pre {
            background: var(--code-bg);
            border: 1px solid var(--border);
            border-radius: 8px;
            padding: 20px 24px;
            overflow-x: auto;
            margin: 16px 0 24px;
            font-size: 13px;
            line-height: 1.6;
        }

        .doc-content code {
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            color: var(--accent);
            background: rgba(79, 156, 249, 0.08);
            padding: 2px 6px;
            border-radius: 3px;
        }

        .doc-content pre code {
            color: #a8c5e8;
            background: none;
            padding: 0;
            font-size: 13px;
        }

        .doc-content strong { color: var(--text-primary); font-weight: 700; }
        .doc-content em { color: var(--accent); font-style: italic; }

        .doc-content hr {
            border: none;
            border-top: 1px solid var(--border);
            margin: 28px 0;
        }

        .doc-content blockquote {
            border-left: 3px solid var(--accent);
            padding: 10px 20px;
            margin: 16px 0;
            color: var(--text-secondary);
            font-style: italic;
            background: var(--accent-glow);
            border-radius: 0 6px 6px 0;
        }

        /* ── FILE TREE ── */
        .file-tree {
            background: var(--code-bg);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 24px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            line-height: 1.8;
            color: var(--text-secondary);
        }

        .file-tree .dir {
            color: var(--accent);
            font-weight: 700;
        }

        .file-tree .file-java { color: #e8a94a; }
        .file-tree .file-xml  { color: #7c5cbf; }
        .file-tree .file-props { color: #2eb87e; }
        .file-tree .file-sql  { color: #e8634a; }
        .file-tree .file-other { color: var(--text-muted); }

        .file-tree .stats-row {
            margin-top: 16px;
            padding-top: 12px;
            border-top: 1px solid var(--border);
            color: var(--text-muted);
            font-size: 11px;
        }

        /* ── ERROR SECTION ── */
        .error-section {
            background: rgba(232, 99, 74, 0.08);
            border: 1px solid rgba(232, 99, 74, 0.3);
            border-radius: 8px;
            padding: 20px 24px;
            color: var(--text-secondary);
        }

        /* ── FOOTER ── */
        .doc-footer {
            margin-top: 80px;
            padding-top: 32px;
            border-top: 1px solid var(--border);
            font-family: 'Courier New', monospace;
            font-size: 11px;
            color: var(--text-muted);
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-wrap: wrap;
            gap: 8px;
        }

        /* ── SCROLLBAR ── */
        ::-webkit-scrollbar { width: 6px; }
        ::-webkit-scrollbar-track { background: var(--bg-primary); }
        ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 3px; }

        /* ── ANIMATIONS ── */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(12px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .doc-section { animation: fadeIn 0.4s ease forwards; }

        /* ── RESPONSIVE ── */
        @media (max-width: 768px) {
            .sidebar { display: none; }
            .main { margin-left: 0; padding: 24px 20px; }
            .hero-title { font-size: 32px; }
        }
    </style>
</head>
<body>

<!-- ── TOP HEADER ── -->
<header class="top-header">
    <span class="header-logo">⟨/⟩ DocGen</span>
    <div class="header-divider"></div>
    <span class="header-project">%s</span>
    <span class="header-meta">Generated %s</span>
</header>

<!-- ── SIDEBAR NAVIGATION ── -->
<nav class="sidebar">
    <div class="sidebar-section">
        <div class="sidebar-label">Documentation</div>
        <div class="sidebar-nav">
            <a href="#overview"><span class="icon">◈</span> Overview</a>
            <a href="#architecture"><span class="icon">🏗</span> Architecture</a>
            <a href="#api"><span class="icon">🔌</span> API Specification</a>
            <a href="#erd"><span class="icon">🗄</span> Entity Relationships</a>
            <a href="#files"><span class="icon">📂</span> File Structure</a>
        </div>
    </div>

    <div class="sidebar-section">
        <div class="sidebar-label">Project Stats</div>
        <div style="padding: 0 4px; font-size: 12px; color: var(--text-muted); line-height: 2;">
            <div>📄 <strong style="color:var(--text-secondary)">%d</strong> files</div>
            <div>📁 <strong style="color:var(--text-secondary)">%d</strong> directories</div>
        </div>
    </div>
</nav>

<!-- ── MAIN CONTENT ── -->
<main class="main">

    <!-- HERO -->
    <section id="overview" class="hero">
        <p class="hero-eyebrow">Code Documentation — Auto-Generated</p>
        <h1 class="hero-title">
            <em>%s</em><br>Documentation
        </h1>
        <p class="hero-subtitle">
            Comprehensive technical documentation automatically generated by analyzing
            the source code with Google Gemini AI.
        </p>
        <div class="hero-stats">
            <div class="stat">
                <span class="stat-value">%d</span>
                <span class="stat-label">Source Files</span>
            </div>
            <div class="stat">
                <span class="stat-value">%d</span>
                <span class="stat-label">Directories</span>
            </div>
            <div class="stat">
                <span class="stat-value">3</span>
                <span class="stat-label">Doc Sections</span>
            </div>
        </div>
    </section>

    <!-- ARCHITECTURE -->
    <section id="architecture" class="doc-section">
        <div class="section-header">
            <div class="section-icon arch">🏗</div>
            <div>
                <h2 class="section-title arch">Architecture</h2>
                <div class="section-desc">System design, module breakdown & design patterns</div>
            </div>
        </div>
        <div class="doc-content">
            %s
        </div>
    </section>

    <!-- API SPECIFICATION -->
    <section id="api" class="doc-section">
        <div class="section-header">
            <div class="section-icon api">🔌</div>
            <div>
                <h2 class="section-title api">API Specification</h2>
                <div class="section-desc">Endpoints, request/response models & authentication</div>
            </div>
        </div>
        <div class="doc-content">
            %s
        </div>
    </section>

    <!-- ERD -->
    <section id="erd" class="doc-section">
        <div class="section-header">
            <div class="section-icon erd">🗄</div>
            <div>
                <h2 class="section-title erd">Entity Relationships</h2>
                <div class="section-desc">Data model, entities, relationships & database schema</div>
            </div>
        </div>
        <div class="doc-content">
            %s
        </div>
    </section>

    <!-- FILE STRUCTURE -->
    <section id="files" class="doc-section">
        <div class="section-header">
            <div class="section-icon files">📂</div>
            <div>
                <h2 class="section-title files">File Structure</h2>
                <div class="section-desc">All scanned source files and directories</div>
            </div>
        </div>
        <div class="file-tree">
            <pre>%s</pre>
        </div>
    </section>

    <!-- FOOTER -->
    <footer class="doc-footer">
        <span>Generated by <strong>DocGen</strong> — Powered by Google Gemini AI</span>
        <span>%s</span>
    </footer>
</main>

<script>
    // Active nav highlight on scroll
    const sections = document.querySelectorAll('.doc-section, .hero');
    const navLinks = document.querySelectorAll('.sidebar-nav a');

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                navLinks.forEach(link => link.classList.remove('active'));
                const id = entry.target.id;
                const active = document.querySelector('.sidebar-nav a[href="#' + id + '"]');
                if (active) active.classList.add('active');
            }
        });
    }, { rootMargin: '-20%% 0px -70%% 0px' });

    sections.forEach(s => observer.observe(s));
</script>

</body>
</html>
""".formatted(
                structure.projectName(),     // title
                structure.projectName(),     // header project name
                timestamp,                   // header meta
                structure.files().size(),    // sidebar files
                structure.byDirectory().size(), // sidebar dirs
                structure.projectName(),     // hero title
                structure.files().size(),    // stat files
                structure.byDirectory().size(), // stat dirs
                cleanGeminiOutput(architectureHtml),
                cleanGeminiOutput(apiHtml),
                cleanGeminiOutput(erdHtml),
                escapeHtml(fileTree),
                timestamp
        );
    }


    private String cleanGeminiOutput(String raw) {
        if (raw == null) return "";
        String cleaned = raw.strip();
        // Remove ```html ... ``` fences
        if (cleaned.startsWith("```html")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.strip();
    }

    private String buildFileTree(ProjectStructure structure) {
        StringBuilder sb = new StringBuilder();
        sb.append(structure.projectName()).append("/\n");

        String currentDir = null;
        for (ProjectFile file : structure.files()) {
            String dir = file.directory();
            if (!dir.equals(currentDir)) {
                currentDir = dir;
                if (!dir.equals("root")) {
                    sb.append("│\n");
                    sb.append("├── ").append(dir).append("/\n");
                }
            }
            String prefix = dir.equals("root") ? "├── " : "│   ├── ";
            sb.append(prefix).append(file.fileName())
                    .append("  (").append(formatSize(file.sizeBytes())).append(")\n");
        }

        sb.append("\n");
        sb.append("── ").append(structure.files().size()).append(" files, ")
                .append(structure.byDirectory().size()).append(" directories");

        return sb.toString();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
