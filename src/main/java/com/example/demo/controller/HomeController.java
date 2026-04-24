package com.example.demo.controller;

import com.example.demo.entity.Post;
import com.example.demo.repository.PostRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final PostRepository postRepository;

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String home() {
        String content = """
                <h1>Spring Boot App Running</h1>
                <p>Your backend is up successfully. Explore the app with links below.</p>
                <div class="grid">
                  <a class="tile" href="/posts">
                    <h3>View Posts</h3>
                    <p>Open a styled list of all posts from PostgreSQL.</p>
                  </a>
                  <a class="tile" href="/create-post">
                    <h3>Create Post</h3>
                    <p>Submit a new post using a simple form.</p>
                  </a>
                  <a class="tile" href="/api/posts">
                    <h3>Posts API</h3>
                    <p>Raw JSON endpoint at <code>GET /api/posts</code>.</p>
                  </a>
                </div>
                """;
        return pageTemplate("Home", content);
    }

    @GetMapping(value = "/posts", produces = MediaType.TEXT_HTML_VALUE)
    public String postsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String created,
            @RequestParam(required = false) String updated,
            @RequestParam(required = false) String deleted) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 20));
        String query = q == null ? "" : q.trim();
        Page<Post> postsPage = query.isBlank()
                ? postRepository.findAll(PageRequest.of(safePage, safeSize))
                : postRepository.findByContentContainingIgnoreCase(query, PageRequest.of(safePage, safeSize));

        String notice = buildNotice(created, updated, deleted);
        String rows = postsPage.getContent().isEmpty()
                ? "<p class=\"empty\">No posts yet. Create one from the Create Post page.</p>"
                : postsPage.getContent().stream()
                .map(post -> """
                        <div class="post">
                          <div><strong>ID:</strong> %d</div>
                          <div><strong>Author ID:</strong> %d</div>
                          <div><strong>Content:</strong> %s</div>
                          <div class="actions">
                            <a class="btn" href="/edit-post/%d">Edit</a>
                            <form method="post" action="/posts/%d/delete" onsubmit="return confirm('Delete this post?');">
                              <button type="submit">Delete</button>
                            </form>
                          </div>
                        </div>
                        """.formatted(
                        post.getId(),
                        post.getAuthorId(),
                        escapeHtml(post.getContent()),
                        post.getId(),
                        post.getId()))
                .collect(Collectors.joining());

        String base = "/posts?size=" + safeSize + "&q=" + urlEncode(query);
        String prevLink = postsPage.hasPrevious() ? "<a class=\"btn\" href=\"" + base + "&page=" + (safePage - 1) + "\">Previous</a>" : "";
        String nextLink = postsPage.hasNext() ? "<a class=\"btn\" href=\"" + base + "&page=" + (safePage + 1) + "\">Next</a>" : "";

        String content = """
                <h1>All Posts</h1>
                <p>Live data loaded from the database.</p>
                %s
                <form method="get" action="/posts" class="search">
                  <input type="hidden" name="size" value="%d" />
                  <input type="text" name="q" placeholder="Search by content..." value="%s" />
                  <button type="submit">Search</button>
                </form>
                <p>Page %d of %d</p>
                %s
                <div class="pager">%s %s</div>
                """.formatted(
                notice,
                safeSize,
                escapeHtml(query),
                postsPage.getTotalPages() == 0 ? 1 : (safePage + 1),
                Math.max(postsPage.getTotalPages(), 1),
                rows,
                prevLink,
                nextLink);
        return pageTemplate("Posts", content);
    }

    @GetMapping(value = "/create-post", produces = MediaType.TEXT_HTML_VALUE)
    public String createPostPage() {
        String content = """
                <h1>Create Post</h1>
                <form method="post" action="/create-post" class="form">
                  <label>Author ID</label>
                  <input type="number" name="authorId" min="1" required />
                  <label>Content</label>
                  <textarea name="content" rows="5" required></textarea>
                  <button type="submit">Create Post</button>
                </form>
                """;
        return pageTemplate("Create Post", content);
    }

    @PostMapping(value = "/create-post", produces = MediaType.TEXT_HTML_VALUE)
    public String createPostSubmit(@RequestParam Long authorId, @RequestParam String content) {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setContent(content);
        postRepository.save(post);
        return redirectHtml("/posts?created=1");
    }

    @GetMapping(value = "/edit-post/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String editPostPage(@PathVariable Long id) {
        Post post = postRepository.findById(id).orElse(null);
        if (post == null) {
            return redirectHtml("/posts");
        }
        String content = """
                <h1>Edit Post</h1>
                <form method="post" action="/edit-post/%d" class="form">
                  <label>Author ID</label>
                  <input type="number" name="authorId" min="1" value="%d" required />
                  <label>Content</label>
                  <textarea name="content" rows="5" required>%s</textarea>
                  <button type="submit">Update Post</button>
                </form>
                """.formatted(post.getId(), post.getAuthorId(), escapeHtml(post.getContent()));
        return pageTemplate("Edit Post", content);
    }

    @PostMapping(value = "/edit-post/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public String editPostSubmit(
            @PathVariable Long id,
            @RequestParam Long authorId,
            @RequestParam String content) {
        Post post = postRepository.findById(id).orElse(null);
        if (post != null) {
            post.setAuthorId(authorId);
            post.setContent(content);
            postRepository.save(post);
        }
        return redirectHtml("/posts?updated=1");
    }

    @PostMapping(value = "/posts/{id}/delete", produces = MediaType.TEXT_HTML_VALUE)
    public String deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return redirectHtml("/posts?deleted=1");
    }

    private String pageTemplate(String title, String content) {
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>__TITLE__</title>
                  <link rel="icon" href="data:,">
                  <style>
                    :root { --bg:#0f172a; --card:#111827; --soft:#1f2937; --text:#e5e7eb; --muted:#9ca3af; --brand:#60a5fa; --brand2:#34d399; }
                    * { box-sizing: border-box; }
                    body { margin:0; font-family: Arial, sans-serif; background: linear-gradient(120deg,#0f172a,#111827 40%,#0b1222); color: var(--text); }
                    .wrap { max-width: 1000px; margin: 40px auto; padding: 0 16px; }
                    .nav { display:flex; gap:10px; flex-wrap:wrap; margin-bottom:20px; }
                    .nav a { text-decoration:none; color:var(--text); background:#1e293b; border:1px solid #334155; padding:10px 14px; border-radius:10px; }
                    .nav a:hover { border-color: var(--brand); }
                    .card { background: rgba(17,24,39,.92); border:1px solid #334155; border-radius:16px; padding:24px; box-shadow:0 10px 30px rgba(0,0,0,.25); }
                    h1 { margin-top:0; font-size:30px; }
                    p { color:var(--muted); }
                    .grid { display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:12px; margin-top:16px; }
                    .tile { display:block; text-decoration:none; color:var(--text); background:#1e293b; border:1px solid #334155; border-radius:12px; padding:14px; }
                    .tile:hover { border-color:var(--brand2); transform: translateY(-1px); }
                    .tile h3 { margin:0 0 8px; }
                    code { background:#111827; border:1px solid #374151; border-radius:6px; padding:2px 6px; }
                    .post { border:1px solid #374151; background:#111827; border-radius:10px; padding:12px; margin:10px 0; line-height:1.6; }
                    .empty { background:#111827; border:1px dashed #4b5563; border-radius:10px; padding:14px; }
                    .form { display:flex; flex-direction:column; gap:10px; max-width:520px; }
                    .search { display:flex; gap:8px; max-width:700px; margin:12px 0; }
                    .actions { display:flex; gap:8px; margin-top:10px; }
                    .pager { display:flex; gap:10px; margin-top:14px; }
                    .notice { border:1px solid #065f46; background:#022c22; color:#6ee7b7; border-radius:10px; padding:10px 12px; margin-bottom:12px; }
                    input, textarea { width:100%; border:1px solid #374151; background:#111827; color:var(--text); border-radius:10px; padding:10px; }
                    button { background:linear-gradient(90deg,var(--brand),var(--brand2)); color:#0b1020; font-weight:700; border:none; border-radius:10px; padding:10px 14px; cursor:pointer; }
                    .btn { display:inline-block; background:#1e293b; color:#e5e7eb; border:1px solid #334155; text-decoration:none; border-radius:10px; padding:9px 12px; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <nav class="nav">
                      <a href="/">Home</a>
                      <a href="/posts">View Posts</a>
                      <a href="/create-post">Create Post</a>
                      <a href="/api/posts">Posts API</a>
                    </nav>
                    <section class="card">
                      __CONTENT__
                    </section>
                  </div>
                </body>
                </html>
                """;
        return html
                .replace("__TITLE__", title)
                .replace("__CONTENT__", content);
    }

    private String buildNotice(String created, String updated, String deleted) {
        if (created != null) {
            return "<div class=\"notice\">Post created successfully.</div>";
        }
        if (updated != null) {
            return "<div class=\"notice\">Post updated successfully.</div>";
        }
        if (deleted != null) {
            return "<div class=\"notice\">Post deleted successfully.</div>";
        }
        return "";
    }

    private String redirectHtml(String url) {
        return """
                <!doctype html>
                <html><head><meta http-equiv="refresh" content="0; URL='%s'"></head><body></body></html>
                """.formatted(url);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
