import { useEffect, useRef, useState } from "react";
import "./App.css";

const loadDocs = () => {
  try { return JSON.parse(localStorage.getItem("docs")) || []; } catch { return []; }
};

async function api(path, options) {
  const res = await fetch(path, options);
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || `Request failed (${res.status})`);
  return data;
}

export default function App() {
  const [docs, setDocs] = useState(loadDocs);
  const [scope, setScope] = useState("all");
  const [messages, setMessages] = useState([]);
  const [question, setQuestion] = useState("");
  const [asking, setAsking] = useState(false);
  const [uploading, setUploading] = useState("");
  const [notice, setNotice] = useState(null);
  const [dragging, setDragging] = useState(false);
  const fileInput = useRef(null);
  const endRef = useRef(null);

  useEffect(() => { localStorage.setItem("docs", JSON.stringify(docs)); }, [docs]);
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages, asking]);

  async function upload(files) {
    setNotice(null);
    for (const file of files) {
      setUploading(file.name);
      const form = new FormData();
      form.append("file", file);
      try {
        const doc = await api("/api/ingest", { method: "POST", body: form });
        setDocs((d) => [doc, ...d]);
        setScope(doc.documentId);
      } catch (e) {
        setNotice(`${file.name}: ${e.message}`);
      }
    }
    setUploading("");
  }

  async function ask(e) {
    e.preventDefault();
    const q = question.trim();
    if (!q || asking) return;
    setQuestion("");
    setMessages((m) => [...m, { role: "user", text: q }]);
    setAsking(true);
    try {
      const body = { question: q, documentId: scope === "all" ? null : scope };
      const res = await api("/api/ask", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      setMessages((m) => [...m, { role: "assistant", ...res }]);
    } catch (err) {
      setMessages((m) => [...m, { role: "error", text: err.message }]);
    } finally {
      setAsking(false);
    }
  }

  function removeDoc(id) {
    setDocs((d) => d.filter((x) => x.documentId !== id));
    if (scope === id) setScope("all");
  }

  const scopeName = scope === "all" ? "all documents" : docs.find((d) => d.documentId === scope)?.documentName;

  return (
    <div className="app">
      <aside className="side">
        <h1>Doc Q&amp;A</h1>
        <p className="muted">Ask questions about your own files. Answers come only from what you upload.</p>

        <div
          className={`drop ${dragging ? "over" : ""}`}
          onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => { e.preventDefault(); setDragging(false); upload([...e.dataTransfer.files]); }}
        >
          <input ref={fileInput} type="file" accept=".pdf,.txt,.md" multiple hidden
            onChange={(e) => { upload([...e.target.files]); e.target.value = ""; }} />
          <button className="btn" onClick={() => fileInput.current.click()} disabled={!!uploading}>
            {uploading ? "Processing…" : "Choose files"}
          </button>
          <span className="muted small">{uploading || "or drop PDF / text files here"}</span>
        </div>
        {notice && <div className="notice" role="alert">{notice}</div>}

        <h2>Search in</h2>
        <ul className="docs">
          <li>
            <label className={scope === "all" ? "doc active" : "doc"}>
              <input type="radio" name="scope" checked={scope === "all"} onChange={() => setScope("all")} />
              <span className="name">All documents</span>
            </label>
          </li>
          {docs.map((d) => (
            <li key={d.documentId}>
              <label className={scope === d.documentId ? "doc active" : "doc"}>
                <input type="radio" name="scope" checked={scope === d.documentId} onChange={() => setScope(d.documentId)} />
                <span className="name" title={d.documentName}>{d.documentName}</span>
                <span className="small muted">{d.chunkCount} chunks</span>
                <button type="button" className="x" aria-label={`Remove ${d.documentName} from list`}
                  title="Remove from this list (stays in the database)"
                  onClick={(e) => { e.preventDefault(); removeDoc(d.documentId); }}>×</button>
              </label>
            </li>
          ))}
        </ul>
      </aside>

      <main className="chat">
        <div className="thread">
          {messages.length === 0 && (
            <div className="empty">
              <h2>{docs.length ? "Ask your first question" : "Add a document to begin"}</h2>
              <p className="muted">
                {docs.length
                  ? `Searching ${scopeName}. Try a specific question, like a number, a date, or a policy.`
                  : "Upload a PDF or text file on the left, then ask anything about it."}
              </p>
            </div>
          )}

          {messages.map((m, i) =>
            m.role === "user" ? (
              <div key={i} className="msg user">{m.text}</div>
            ) : m.role === "error" ? (
              <div key={i} className="msg error" role="alert">{m.text}</div>
            ) : (
              <div key={i} className="msg bot">
                <p>{m.answer}</p>
                {m.sources?.length > 0 && (
                  <details>
                    <summary>{m.sources.length} sources · {m.totalTimeMs} ms</summary>
                    <ul className="sources">
                      {m.sources.map((s, j) => (
                        <li key={j}>
                          <div className="src-head">
                            <span>{s.documentName}, chunk {s.chunkIndex}</span>
                            <span className="bar" title={`Similarity ${s.similarityScore}`}>
                              <i style={{ width: `${Math.max(0, Math.min(1, s.similarityScore)) * 100}%` }} />
                            </span>
                            <span className="small muted">{s.similarityScore.toFixed(2)}</span>
                          </div>
                          <p className="small muted">{s.snippet}</p>
                        </li>
                      ))}
                    </ul>
                  </details>
                )}
              </div>
            )
          )}

          {asking && <div className="msg bot typing" aria-label="Thinking"><i /><i /><i /></div>}
          <div ref={endRef} />
        </div>

        <form className="composer" onSubmit={ask}>
          <input value={question} onChange={(e) => setQuestion(e.target.value)}
            placeholder={`Ask about ${scopeName || "your documents"}`} aria-label="Question" />
          <button className="btn primary" disabled={asking || !question.trim()}>Ask</button>
        </form>
      </main>
    </div>
  );
}
